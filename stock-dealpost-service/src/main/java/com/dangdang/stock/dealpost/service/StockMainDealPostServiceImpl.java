package com.dangdang.stock.dealpost.service;

import com.alibaba.dubbo.rpc.RpcContext;
import com.dangdang.modules.hestia.logger.AlertChannel;
import com.dangdang.modules.hestia.logger.AppType;
import com.dangdang.modules.hestia.logger.ErrorInfo;
import com.dangdang.stock.api.StockDealPostService;
import com.dangdang.stock.dealpost.common.ApplicationContextProvider;
import com.dangdang.stock.dealpost.dao.LimitPostQueueDao;
import com.dangdang.stock.dealpost.dao.PostStockTriggerQueueDao;
import com.dangdang.stock.dealpost.dao.ProductWarehouseStockDao;
import com.dangdang.stock.dealpost.dto.Order;
import com.dangdang.stock.dealpost.dto.Product;
import com.dangdang.stock.dealpost.dto.ResponseDTO;
import com.dangdang.stock.dealpost.model.LimitPostQueue;
import com.dangdang.stock.dealpost.model.PostSTQueue;
import com.dangdang.stock.dealpost.model.ProductWStock;
import com.dangdang.stock.dealpost.util.SystemConstant;
import com.dangdang.stock.dealpost.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhanghaihua on 2017/6/2.
 */
@Slf4j
@Service("stockDealPostService")
public class StockMainDealPostServiceImpl implements StockDealPostService, InitializingBean {

    private Logger limitLog = LoggerFactory.getLogger("limit-log");
    private Logger mainLog = LoggerFactory.getLogger("main-log");
    private Logger slaLog = LoggerFactory.getLogger("sla-log");

    //productList count limit
    private final static Integer LIMIT_NUM = 200;

    //main process timeout,time_unit:ms
    // for productList count in (0,50]
    private final static Integer TIME_OUT_LOW = 100;
    // for productList count in (50,100]
    private final static Integer TIME_OUT_MID = 300;
    // for productList count (100,200]
    private final static Integer TIME_OUT_HIG = 500;
    //虚拟捆绑商品类型
    private final static Integer VIRTUAL_PRODUCT_TYPE = 9;

    //占库存来源类型，101：促销占库存，102：普通占库存
    private final static String SOURCE_CUXIAO = "101";
    private final static String SOURCE_COMMON = "102";

    //response success
    private final static int RESP_OK = 1;
    //response failed
    private final static int RESP_FAILED = 0;

    @Resource
    private LimitPostQueueDao limitPostQueueDao;

    @Resource
    private ProductWarehouseStockDao productWarehouseStockDao;

    @Resource
    private PostStockTriggerQueueDao postStockTriggerQueueDao;

    private volatile StockMainDealPostServiceImpl selfService;

    private ExecutorService executorService;

    @Override
    public void afterPropertiesSet() throws Exception {
        ErrorInfo.setGlobalDepartmentAppName(SystemConstant.DEPARTMENT_NAME, SystemConstant.APP_NAME
                , AppType.WEB_SERVER, new HashMap<AlertChannel, Collection<String>>());
        executorService = ThreadPoolUtil.getThreadPool();
    }

    public ResponseDTO postStock(final Order order) {
        long startTime = System.currentTimeMillis();
        if (selfService == null) {
            selfService = ApplicationContextProvider.getContext().getBean(getClass());
        }
        mainLog.info("RequestParameter:{},ClientIp:{}", order, RpcContext.getContext().getRemoteHost());
        boolean successFlag = true;
        int timeout = 0;
        try {
            //check order
            checkAndInitOrder(order);
            //prepare data
            timeout = prepareData(order);

            //submit mainPostStock and get result
            Integer mainResult = executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return selfService.mainPostStock(order);
                }
            }).get(timeout, TimeUnit.MILLISECONDS);

            //execute successful
            if (mainResult.equals(RESP_OK)) {
                //execute insert limit_post_queue task task
                executeLimitStock(order);
            }
            return new ResponseDTO(RESP_OK, "SUCCESS");
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                mainLog.info("TimeoutException:orderId:{},mainPost's times greater than {}ms!"
                        , order.getOrderId(), timeout);
                executeLimitStock(order);
                return new ResponseDTO(RESP_OK, "SUCCESS");
            } else if (e instanceof ExecutionException) {
                if (e.getMessage().contains("RunExp200")) {
                    successFlag = false;
                    return new ResponseDTO(RESP_FAILED
                            , "Failed to post stocks!orderId:" + order.getOrderId());
                } else if (e.getMessage().contains("DupKey201")) {
                    return new ResponseDTO(RESP_OK, "SUCCESS");
                } else {
                    successFlag = false;
                    ErrorInfo.builder().module(SystemConstant.MODULE_NAME)
                            .addParam("order", order + "")
                            .exception(e).build().error(log);
                    return new ResponseDTO(RESP_FAILED, e.getMessage());
                }
            } else {
                successFlag = false;
                ErrorInfo.builder().module(SystemConstant.MODULE_NAME)
                        .addParam("order", order + "")
                        .exception(e).build().error(log);
                return new ResponseDTO(RESP_FAILED, e.getMessage());
            }
        } finally {
            long useTime = System.currentTimeMillis() - startTime;
            mainLog.info("apiResponse-->>orderId:{},successFlag:{},useTimes:{}ms"
                    , order.getOrderId(), successFlag, useTime);
            slaLog.info("API-SLA-ANALYSIS[ statType=provider apiURL=dealpostservice.dubbo " +
                    "responseTime={} successFlag={} ]", useTime, successFlag);
        }
    }

    private int prepareData(Order order) {
        //sort productList for avoiding dead lock when write db
        Collections.sort(order.getProductList());
        //get timeout time
        int count = order.getProductList().size();
        if (count <= 50) {
            return TIME_OUT_LOW;
        } else if (count <= 100) {
            return TIME_OUT_MID;
        } else {
            return TIME_OUT_HIG;
        }
    }

    private void checkAndInitOrder(Order order) {

        if (order == null) {
            throw new RuntimeException("ERROR:order is null!");
        }

        if (order.getOrderId() <= 0) {
            throw new RuntimeException("ERROR:orderId <= 0!");
        }

        if (order.getPostTime() == null) {
            order.setPostTime(new Date());
        }

        if (StringUtils.isEmpty(order.getCartPostStockId())) {
            order.setCartPostStockId("-1");
        }

        List<Product> productList = order.getProductList();

        if (productList == null || productList.size() > LIMIT_NUM) {
            throw new RuntimeException("ERROR:productList is null or Count is greater than boundary");
        }

        for (Product product : order.getProductList()) {
            if (product == null) {
                throw new RuntimeException("ERROR:product is null!");
            }
            checkProduct(product);
        }
    }

    private boolean isRightNumeric(String str) {
        Pattern pattern = Pattern.compile("[1-9][0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

    private void checkProduct(Product product) {
        if (product.getProductId() <= 0) {
            throw new RuntimeException("ERROR:productId <= 0!");
        }
        if (product.getWarehouseId() <= 0) {
            throw new RuntimeException("ERROR:warehouseId <= 0!");
        }

        if (product.getOpNum() <= 0) {
            throw new RuntimeException("ERROR:opNum <= 0!");
        }

        if (product.getStockTypeId() == null) {
            product.setStockTypeId(0);
        } else if (product.getStockTypeId().compareTo(0) == -1) {
            throw new RuntimeException("ERROR:stockTypeId < 0!");
        }

        String sourceIds = product.getSourceIds();
        if (StringUtils.isNotEmpty(sourceIds)) {
            String[] sourceIdArr = sourceIds.split(",");
            for (String sourceId : sourceIdArr) {
                if (!isRightNumeric(sourceId)) {
                    throw new RuntimeException("ERROR:sourceIds is not Numeric!");
                }
            }
        }
        if (product.getIsPreSale() == null) {
            product.setIsPreSale(0);
        } else if (!product.getIsPreSale().equals(0) && !product.getIsPreSale().equals(1)) {
            throw new RuntimeException("ERROR:isPreSale is not in (0,1)!");
        }

        if (product.getPostType() == null) {
            product.setPostType(0);
        } else if (!product.getPostType().equals(0) && !product.getPostType().equals(1)
                && !product.getPostType().equals(2)) {
            throw new RuntimeException("ERROR:postType is not in (0,1,2)!");
        }

        if (product.getParentId() == null) {
            product.setParentId(0);
        } else if (product.getParentId().compareTo(0) == -1) {
            throw new RuntimeException("ERROR:parentId < 0!");
        }

        if (product.getProductType() == null) {
            product.setProductType(0);
        } else if (product.getProductType().compareTo(0) == -1) {
            throw new RuntimeException("ERROR:productType < 0!");
        }
    }

    private void executeLimitStock(final Order order) {
        ThreadPoolUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                limitPostStock(order);
            }
        });
    }

    private void limitPostStock(Order order) {
        List<LimitPostQueue> limitPostQueueList = new ArrayList<>();
        try {
            Long orderId = order.getOrderId();
            Date orderTime = order.getPostTime();
            Map<String, LimitPostQueue> limitMap = new HashMap<>();
            for (Product p : order.getProductList()) {
                LimitPostQueue limitPostQueue = new LimitPostQueue();
                if (p.getSourceIds() != null && !p.getSourceIds().trim().isEmpty()) {
                    limitPostQueue.setSourceIds(p.getSourceIds());
                    limitPostQueue.setSource(SOURCE_CUXIAO);
                } else {
                    limitPostQueue.setSource(SOURCE_COMMON);
                }
                String key = p.getProductId() + "_" + p.getWarehouseId() + "_" + orderId + "_";
                //经分析，只要是促销品且没有父ID品，则需要插入limit_post_queue
                //此情况包括虚拟母品与无父ID品，排除有父ID的子品
                if (limitPostQueue.getSource().equals(SOURCE_CUXIAO) && p.getParentId().equals(0)) {
                    if (limitMap.containsKey(key)) {
                        Integer num = limitMap.get(key).getOpNum();
                        limitMap.get(key).setOpNum(num + p.getOpNum());
                    } else {
                        limitMap.put(key, limitPostQueue);
                        limitPostQueueList.add(limitPostQueue);
                    }
                } else {
                    continue;
                }
                limitPostQueue.setOrderId(orderId);
                limitPostQueue.setOpNum(p.getOpNum());
                limitPostQueue.setOrderTime(orderTime);
                limitPostQueue.setProductId(p.getProductId());
                limitPostQueue.setWarehouseId(p.getWarehouseId());
                limitPostQueue.setStockTypeId(p.getStockTypeId());
            }
            limitPostQueueDao.insertLimitQueueBatch(limitPostQueueList);
            limitLog.info("Insert limit_post_queue successful!orderId:{}", order.getOrderId());
        } catch (Exception e) {
            ErrorInfo.builder().message("Insert limit_post_queue failed!")
                    .module(SystemConstant.MODULE_NAME)
                    .addParam("limitPostQueueList", limitPostQueueList + "")
                    .exception(e).build().error(log);
        }
    }

    @Transactional
    public Integer mainPostStock(Order order) {
        long startTime = System.currentTimeMillis();
        int lockStockCount = 0;
        List<ProductWStock> productWStockList = new ArrayList<>();
        List<PostSTQueue> postSTQueueList = new ArrayList<>();
        Map<String, PostSTQueue> queueMap = new HashMap<>();
        Map<String, ProductWStock> stockMap = new HashMap<>();
        try {
            Long orderId = order.getOrderId();
            int num;
            for (Product p : order.getProductList()) {
                //假如是促销品，且为虚拟母品，则过滤
                if (p.getSourceIds() != null && !p.getSourceIds().trim().isEmpty() &&
                        VIRTUAL_PRODUCT_TYPE.equals(p.getProductType())) {
                    continue;
                }
                PostSTQueue postSTQueue = new PostSTQueue();
                ProductWStock productWStock = new ProductWStock();
                int stockTypeId = p.getStockTypeId();

                productWStock.setProductId(p.getProductId());
                productWStock.setWarehouseId(p.getWarehouseId());
                productWStock.setOpNum(p.getOpNum());
                productWStock.setStockTypeId(stockTypeId);

                postSTQueue.setOrderId(orderId);
                postSTQueue.setProductId(p.getProductId());
                postSTQueue.setWarehouseId(p.getWarehouseId());
                postSTQueue.setOpNum(p.getOpNum());
                postSTQueue.setStockTypeId(stockTypeId);
                postSTQueue.setCartPostStockId(order.getCartPostStockId());

                if (order.getCartPostStockId().equals("-1")) {
                    Integer isPreSale = p.getIsPreSale() == null ? 0 : p.getIsPreSale();
                    Integer isPostType = p.getPostType() == null ? 0 : p.getPostType();
                    if (isPreSale == 0) {
                        if (isPostType == 1 || isPostType == 2) {
                            postSTQueue.setEffectPostStatus(0);
                            productWStock.setEffectPostQuantity(0);
                        } else {
                            postSTQueue.setEffectPostStatus(1);
                            productWStock.setEffectPostQuantity(p.getOpNum());
                        }
                    } else {
                        productWStock.setEffectPostQuantity(0);
                    }
                } else {
                    productWStock.setEffectPostQuantity(0);
                }

                String key = p.getProductId() + "_" + p.getWarehouseId() + "_" + orderId + "_";
                if (stockMap.containsKey(key)) {
                    num = stockMap.get(key).getOpNum();
                    stockMap.get(key).setOpNum(num + p.getOpNum());
                } else {
                    if (stockTypeId > 0) {
                        lockStockCount++;
                    }
                    stockMap.put(key, productWStock);
                    productWStockList.add(productWStock);
                }

                if (queueMap.containsKey(key)) {
                    num = queueMap.get(key).getOpNum();
                    queueMap.get(key).setOpNum(num + p.getOpNum());
                } else {
                    queueMap.put(key, postSTQueue);
                    postSTQueueList.add(postSTQueue);
                }
            }
            int[] resultArr = productWarehouseStockDao.updateStockBatch(productWStockList);
            int count =0;
            for (int i : resultArr) {
                count += i;
            }
            if (count != productWStockList.size() + lockStockCount) {
                mainLog.info("FAILED!update p_ware_stock failed,useTime:{}ms,orderId:{},{}"
                        , System.currentTimeMillis() - startTime, orderId, order.getProductList());
                throw new RuntimeException("RunExp200");
            }
            //insert post_stock_trigger_queue
            postStockTriggerQueueDao.insertTriggerQueueBatch(postSTQueueList);
            mainLog.info("orderId:{},main process successful!",orderId);
            return RESP_OK;
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                mainLog.info("DuplicateKeyException:orderId:{}", order.getOrderId());
                throw new RuntimeException("DupKey201");
            } else {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}



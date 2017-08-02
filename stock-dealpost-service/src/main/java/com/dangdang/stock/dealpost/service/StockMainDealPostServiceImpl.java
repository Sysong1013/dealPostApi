package com.dangdang.stock.dealpost.service;

import com.alibaba.dubbo.rpc.RpcContext;
import com.dangdang.modules.hestia.logger.AlertChannel;
import com.dangdang.modules.hestia.logger.AppType;
import com.dangdang.modules.hestia.logger.ErrorInfo;
import com.dangdang.stock.api.StockDealPostService;
import com.dangdang.stock.dealpost.common.ApplicationContextProvider;
import com.dangdang.stock.dealpost.common.BizException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
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
    private final static int LIMIT_NUM = 200;

    //main process timeout,time_unit:ms
    // for productList count in (0,50]
    private final static int TIME_OUT_LOW = 100;
    // for productList count in (50,100]
    private final static int TIME_OUT_MID = 300;
    // for productList count (100,~]
    private final static int TIME_OUT_HIG = 500;
    //virtual product type
    private final static int VIRTUAL_PRODUCT_TYPE = 9;

    //the type of post stock's source,101:promotion 102:common
    private final static String SOURCE_PROMOTION = "101";

    //response success code
    private final static int RESP_OK = 1;
    //response failed code
    private final static int RESP_FAILED = 0;
    //response success message
    private final static String RESP_OK_MSG = "SUCCESS";
    // pattern int>0
    private final static Pattern PATTERN_POSITIVE_NUMBER = Pattern.compile("[1-9][0-9]*");

    // switch of post stock synchronized
    @Value("${application.syncSwitch}")
    private int syncSwitch;

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
        mainLog.info("ApiParameter--->>>{},ClientIp:{}", order, RpcContext.getContext().getRemoteHost());
        //异步调用时,为了@Transactional注解起作用,应该使用通过getBean()方法获取的Proxy对象调用mainPostStock()方法
        //由于afterPropertiesSet()方法中无法获取Proxy对象(因为还没有构建出), 所以写到该方法中
        //第一次执行时多个并发的线程有可能同时进入if语句中,但是因为获取的都是相同的对象,顶多是重复初始化而已,不需要为此加锁
        if (selfService == null) {
            selfService = ApplicationContextProvider.getContext().getBean(getClass());
        }
        boolean successFlag = true;
        int timeout = 0;
        try {
            checkOrder(order);
            sortProducts(order);
            timeout = getTimeoutTimes(order);

            //submit mainPostStock and get result
            Future<Void> result = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    selfService.mainPostStock(order);
                    return null;
                }
            });

            if (syncSwitch == 0) {
                result.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                result.get();
            }

            //asynchronized insert limit_post_queue task task
            executeLimitPostStockAsync(order);

            return new ResponseDTO(RESP_OK, RESP_OK_MSG);
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                mainLog.info("TimeoutException!orderId:{},useTimes>{}ms", order.getOrderId(), timeout);
                executeLimitPostStockAsync(order);
                return new ResponseDTO(RESP_OK, RESP_OK_MSG);
            } else if (e instanceof BizException) {
                if (((BizException) e).getCode() == BizException.PARAMETER_INVALID_EXCEPTION) {
                    //request parameters exception
                    mainLog.info("Invalid parameter!orderId:{},{}", order.getOrderId(), e.getMessage());
                } else if (((BizException) e).getCode() == BizException.DUPLICATE_KEY_EXCEPTION) {
                    return new ResponseDTO(RESP_OK, RESP_OK_MSG);
                }
            } else {
                //exclude mainPostStock BizException
                if (!(e.getCause() != null && e.getCause() instanceof BizException)) {
                    ErrorInfo.builder().message("deal_post_stock receive an Exception")
                            .module(SystemConstant.MODULE_NAME)
                            .addParam("orderId", order.getOrderId() + "")
                            .exception(e).build().error(log);
                }
            }
            successFlag = false;
            return new ResponseDTO(RESP_FAILED
                    , "FAILURE!orderId:" + order.getOrderId() + ","
                    + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
        } finally {
            mainLog.info("ApiResponse--->>>orderId:{},successFlag:{},useTimes:{}ms"
                    , order.getOrderId(), successFlag, System.currentTimeMillis() - startTime);
            slaLog.info("API-SLA-ANALYSIS[ statType=provider apiURL=dealpostservice.dubbo " +
                    "responseTime={} successFlag={} ]", System.currentTimeMillis() - startTime, successFlag);
        }
    }


    private void sortProducts(Order order) {
        Collections.sort(order.getProductList(), new Comparator<Product>() {
            @Override
            public int compare(Product o1, Product o2) {
                long p1 = o1.getProductId();
                long p2 = o2.getProductId();
                if (p1 != p2) {
                    return (p1 < p2) ? -1 : 1;
                }
                int w1 = o1.getWarehouseId();
                int w2 = o2.getWarehouseId();
                if (w1 != w2) {
                    return (w1 < w2) ? -1 : 1;
                }
                return 0;
            }
        });
    }

    private int getTimeoutTimes(Order order) {
        int count = order.getProductList().size();
        if (count <= 50) {
            return TIME_OUT_LOW;
        } else if (count <= 100) {
            return TIME_OUT_MID;
        } else {
            return TIME_OUT_HIG;
        }
    }

    private void checkOrder(Order order) {

        if (order == null) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "order is null!");
        }

        if (order.getOrderId() <= 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "orderId <= 0!");
        }

        if (order.getPostTime() == null) {
            order.setPostTime(new Date());
        }

        if (order.getCartPostStockId() == null || order.getCartPostStockId().trim().length() == 0) {
            order.setCartPostStockId("-1");
        }

        List<Product> productList = order.getProductList();
        if (productList == null || productList.size() > LIMIT_NUM) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "productList is null or counts >" + LIMIT_NUM);
        }

        for (Product product : order.getProductList()) {
            if (product == null) {
                throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "product is null!");
            }
            checkProduct(product);
        }
    }

    private boolean isValidNumeric(String str) {
        Matcher isNum = PATTERN_POSITIVE_NUMBER.matcher(str);
        return isNum.matches();
    }

    private void checkProduct(Product product) {
        if (product.getProductId() <= 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "productId:" + product.getProductId() + " <= 0!");
        }
        if (product.getWarehouseId() <= 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "warehouseId:" + product.getWarehouseId() + " <= 0!");
        }

        if (product.getOpNum() <= 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "opNum:" + product.getOpNum() + " <= 0!");
        }

        if (product.getStockTypeId() < 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "stockTypeId:" + product.getStockTypeId() + " < 0!");
        }


        if (product.getSourceIds() == null) {
            product.setSourceIds("");
        } else {
            String[] sourceIdArr = product.getSourceIds().split(",");
            for (String sourceId : sourceIdArr) {
                if (!isValidNumeric(sourceId)) {
                    throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "sourceIds:" + product.getSourceIds() + " invalid!");
                }
            }
        }


        if (product.getParentId() < 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "parentId:" + product.getParentId() + " < 0!");
        }

        if (product.getProductType() < 0) {
            throw new BizException(BizException.PARAMETER_INVALID_EXCEPTION, "productType:" + product.getProductType() + " < 0!");
        }
    }

    private void executeLimitPostStockAsync(final Order order) {
        ThreadPoolUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                limitPostStock(order);
            }
        });
    }

    private void limitPostStock(Order order) {
        long startTime = System.currentTimeMillis();
        List<LimitPostQueue> limitPostQueueList = new ArrayList<>();
        try {
            long orderId = order.getOrderId();
            Date orderTime = order.getPostTime();
            Map<String, LimitPostQueue> limitMap = new HashMap<>();
            for (Product p : order.getProductList()) {
                LimitPostQueue limitPostQueue = new LimitPostQueue();
                if (p.getSourceIds().length() > 0) {
                    limitPostQueue.setSourceIds(p.getSourceIds());
                    limitPostQueue.setSource(SOURCE_PROMOTION);

                    String key = p.getProductId() + "_" + p.getWarehouseId() + "_" + orderId + "_";
                    //经分析，只要是促销品且没有父ID品，则需要插入limit_post_queue
                    //此情况包括虚拟母品与无父ID品，排除有父ID的子品
                    if (p.getParentId() == 0) {
                        limitPostQueue.setOrderId(orderId);
                        limitPostQueue.setOpNum(p.getOpNum());
                        limitPostQueue.setOrderTime(orderTime);
                        limitPostQueue.setProductId(p.getProductId());
                        limitPostQueue.setWarehouseId(p.getWarehouseId());
                        limitPostQueue.setStockTypeId(p.getStockTypeId());
                        if (limitMap.containsKey(key)) {
                            limitMap.get(key).setOpNum(limitMap.get(key).getOpNum() + p.getOpNum());
                        } else {
                            limitMap.put(key, limitPostQueue);
                            limitPostQueueList.add(limitPostQueue);
                        }
                    }
                }
            }
            limitPostQueueDao.insertLimitQueueBatch(limitPostQueueList);
            limitLog.info("SUCCESS!orderId:{},insert limit_post_queue successful,useTimes:{}ms"
                    , orderId, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            ErrorInfo.builder().message("Insert limit_post_queue failed!")
                    .module(SystemConstant.MODULE_NAME)
                    .addParam("limitPostQueueList", limitPostQueueList + "")
                    .addParam("useTime", (System.currentTimeMillis() - startTime) + "")
                    .exception(e).build().error(log);
        }
    }

    @Transactional
    public void mainPostStock(Order order) {
        long startTime = System.currentTimeMillis();
        int lockStockCount = 0;
        List<ProductWStock> productWStockList = new ArrayList<>();
        List<PostSTQueue> postSTQueueList = new ArrayList<>();
        Map<String, PostSTQueue> queueMap = new HashMap<>();
        Map<String, ProductWStock> stockMap = new HashMap<>();
        try {
            long orderId = order.getOrderId();
            for (Product p : order.getProductList()) {
                //如果为促销品，且为虚拟母品，则过滤
                if (p.getSourceIds().length() > 0 && p.getProductType() == VIRTUAL_PRODUCT_TYPE) {
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
                    if (!p.isPreSale()) {
                        if (p.isTsOrAllotType()) {
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
                    stockMap.get(key).setOpNum(stockMap.get(key).getOpNum() + p.getOpNum());
                } else {
                    if (stockTypeId > 0) {
                        lockStockCount++;
                    }
                    stockMap.put(key, productWStock);
                    productWStockList.add(productWStock);
                }

                if (queueMap.containsKey(key)) {
                    queueMap.get(key).setOpNum(queueMap.get(key).getOpNum() + p.getOpNum());
                } else {
                    queueMap.put(key, postSTQueue);
                    postSTQueueList.add(postSTQueue);
                }
            }
            int[] resultArr = productWarehouseStockDao.updateStockBatch(productWStockList, order.isNoConsistency());
            int count = 0;
            for (int i : resultArr) {
                count += i;
            }
            if (count != productWStockList.size() + lockStockCount) {
                mainLog.info("FAILED!orderId:{},update p_ware_stock failed,useTime:{}ms"
                        , orderId, System.currentTimeMillis() - startTime);
                throw new BizException(BizException.MAIN_POST_EXCEPTION, "mainPost stock failed");
            }
            //insert post_stock_trigger_queue
            postStockTriggerQueueDao.insertTriggerQueueBatch(postSTQueueList);
            mainLog.info("SUCCESS!orderId:{},mainPost stock successful,useTime:{}ms"
                    , orderId, System.currentTimeMillis() - startTime);
        } catch (RuntimeException e) {
            if (e instanceof DuplicateKeyException) {
                mainLog.info("DuplicateKeyException!orderId:{}", order.getOrderId());
                throw new BizException(BizException.DUPLICATE_KEY_EXCEPTION, "mainPost stock failed");
            } else {
                throw e;
            }
        }
    }
}



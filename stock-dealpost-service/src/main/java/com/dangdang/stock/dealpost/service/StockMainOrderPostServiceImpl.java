package com.dangdang.stock.dealpost.service;

import com.alibaba.dubbo.rpc.RpcContext;
import com.dangdang.modules.hestia.logger.AlertChannel;
import com.dangdang.modules.hestia.logger.AppType;
import com.dangdang.modules.hestia.logger.ErrorInfo;
import com.dangdang.stock.api.StockDealPostService;
import com.dangdang.stock.dealpost.common.ApplicationContextProvider;
import com.dangdang.stock.dealpost.common.SlaLog;
import com.dangdang.stock.dealpost.dao.impl.LimitPostQueueDao;
import com.dangdang.stock.dealpost.dao.impl.PostStockTriggerQueueDao;
import com.dangdang.stock.dealpost.dao.impl.ProductWarehouseStockDao;
import com.dangdang.stock.dealpost.dto.Order;
import com.dangdang.stock.dealpost.dto.Product;
import com.dangdang.stock.dealpost.dto.ResponseDTO;
import com.dangdang.stock.dealpost.util.SystemConstant;
import com.dangdang.stock.dealpost.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
@Slf4j
@Service("stockDealPostService")
public class StockMainOrderPostServiceImpl implements StockDealPostService, InitializingBean {


    //productList count limit
    private final static Integer LIMIT_NUM = 200;

    //main process timeout,time_unit:ms
    // for productList count in (0,50]
    private final static Integer TIME_OUT_LOW = 100;
    // for productList count in (50,100]
    private final static Integer TIME_OUT_MID = 200;
    // for productList count (100,200]
    private final static Integer TIME_OUT_HIG = 500;

    private ExecutorService executorService;

    @Resource
    private SlaLog slaLog;

    @Resource
    private ProductWarehouseStockDao productWarehouseStockDao;

    @Resource
    private LimitPostQueueDao limitPostQueueDao;

    @Resource
    private PostStockTriggerQueueDao postStockTriggerQueueDao;


    @Override
    public void afterPropertiesSet() throws Exception {
        ErrorInfo.setGlobalDepartmentAppName(SystemConstant.DEPARTMENT_NAME, SystemConstant.APP_NAME
                , AppType.WEB_SERVER, new HashMap<AlertChannel, Collection<String>>());
        executorService = ThreadPoolUtil.getThreadPool();

    }

    public ResponseDTO postStock(Order order) {


        long startTime = System.currentTimeMillis();
        log.info("RequestParameter:{},ClientIp:{}", order, RpcContext.getContext().getRemoteHost());
        boolean successFlag = true;
        int timeout = 0;
        try {
            //check order
            checkAndInitOrder(order);

            //prepare data
            timeout = prepareData(order);

            //get main task and submit
            MainStockPostService mainService = getMainService(order);
            Future<Integer> result = executorService.submit(mainService);
            //get result
            Integer code = result.get(timeout, TimeUnit.MILLISECONDS);

            //execute successful
            if (code.equals(SystemConstant.RESP_OK)) {
                //execute insert limit_post_queue task task
                executeLimitPostService(order);
                return new ResponseDTO(SystemConstant.RESP_OK, "SUCCESS");
            } else {
                //post stock failed
                return new ResponseDTO(SystemConstant.RESP_FAILED, "post stock failed!");
            }
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                log.info("orderId:{},main process time greater than {}ms,happen timeoutException!"
                        , order.getOrderId(), timeout);
                //execute insert limit_post_queue task task
                executeLimitPostService(order);
                return new ResponseDTO(SystemConstant.RESP_OK, "SUCCESS");
            }
            ErrorInfo.builder().module(SystemConstant.MODULE_NAME).addParam("order", order + "")
                    .exception(e).build().error(log);
            successFlag = false;
            return new ResponseDTO(SystemConstant.RESP_FAILED, e.getMessage());
        } finally {
            long useTime = System.currentTimeMillis() - startTime;
            log.info("apiResponse-->>{},successFlag:{},useTimes:{}ms", order, successFlag, useTime);
            slaLog.log("API-SLA-ANALYSIS[ statType=provider apiURL=dealpostservice.dubbo " +
                    "responseTime={} successFlag={} ]", useTime, successFlag);
        }
    }

    private MainStockPostService getMainService(Order order) {
        MainStockPostService mainStockPostService = ApplicationContextProvider.getContext()
                .getBean(MainStockPostService.class);
        mainStockPostService.setOrder(order);
        mainStockPostService.setPostStockTriggerQueueDao(postStockTriggerQueueDao);
        mainStockPostService.setProductWarehouseStockDao(productWarehouseStockDao);
        return mainStockPostService;
    }

    private void executeLimitPostService(Order order) {
        LimitPostQueueService limitPostQueueService = ApplicationContextProvider.getContext()
                .getBean(LimitPostQueueService.class);
        limitPostQueueService.setOrder(order);
        limitPostQueueService.setLimitPostQueueDao(limitPostQueueDao);
        executorService.execute(limitPostQueueService);
    }

    private Integer prepareData(Order order) {
        //sort productList for avoiding dead lock with write db
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

        if (order.getCartPostStockId() == null || order.getCartPostStockId().trim().isEmpty()) {
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

    private boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[\\-]?[0-9]*");
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
        if (sourceIds != null && !sourceIds.trim().isEmpty()) {
            String[] sourceIdArr = sourceIds.split(",");
            for (String sourceId : sourceIdArr) {
                if (!isNumeric(sourceId)) {
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
}



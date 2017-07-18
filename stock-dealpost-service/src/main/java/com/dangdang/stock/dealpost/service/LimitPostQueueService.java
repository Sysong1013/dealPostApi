package com.dangdang.stock.dealpost.service;

import com.dangdang.modules.hestia.logger.ErrorInfo;
import com.dangdang.stock.dealpost.dao.impl.LimitPostQueueDao;
import com.dangdang.stock.dealpost.dto.Order;
import com.dangdang.stock.dealpost.dto.Product;
import com.dangdang.stock.dealpost.model.LimitPostQueue;
import com.dangdang.stock.dealpost.util.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 11:56 2017/7/13
 * @Company: Dangdang
 * @Modified By:
 */
@Slf4j
@Service
@Scope("prototype")
public class LimitPostQueueService implements Runnable {


    private LimitPostQueueDao limitPostQueueDao;

    private List<LimitPostQueue> limitPostQueueList;

    private Order order;

    //占库存来源类型，101：促销占库存，102：普通占库存，100：尾品汇购物车占库存
    private final static String SOURCE_CUXIAO = "101";
    private final static String SOURCE_COMMON = "102";

    public LimitPostQueueService() {
        this.limitPostQueueList = new LinkedList<>();
    }


    public void setLimitPostQueueDao(LimitPostQueueDao limitPostQueueDao) {
        this.limitPostQueueDao = limitPostQueueDao;
    }


    public void setOrder(Order order) {
        this.order = order;
    }

    @Override
    public void run() {
        try {
            buildSqlData();
            this.limitPostQueueDao.insertQueueBatch(this.limitPostQueueList);
        } catch (Exception e) {
            ErrorInfo.builder().module(SystemConstant.MODULE_NAME)
                    .addParam("limitPostQueueList", this.limitPostQueueList + "")
                    .exception(e).build().error(log);
        }
    }

    private void buildSqlData() {
        Long orderId = this.order.getOrderId();
        Date orderTime = this.order.getPostTime();
        Map<String, LimitPostQueue> limitMap = new HashMap<>();
        for (Product p : this.order.getProductList()) {
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
    }
}

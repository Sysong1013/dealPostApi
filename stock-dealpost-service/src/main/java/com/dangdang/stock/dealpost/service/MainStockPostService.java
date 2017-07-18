package com.dangdang.stock.dealpost.service;

import com.dangdang.modules.hestia.logger.ErrorInfo;
import com.dangdang.stock.dealpost.dao.impl.PostStockTriggerQueueDao;
import com.dangdang.stock.dealpost.dao.impl.ProductWarehouseStockDao;
import com.dangdang.stock.dealpost.dto.Order;
import com.dangdang.stock.dealpost.dto.Product;
import com.dangdang.stock.dealpost.model.PostSTQueue;
import com.dangdang.stock.dealpost.model.ProductWStock;
import com.dangdang.stock.dealpost.util.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 20:05 2017/7/11
 * @Company: Dangdang
 * @Modified By:
 */
@Slf4j
@Service
@Scope("prototype")
public class MainStockPostService implements Callable<Integer> {

    private Order order;
    private int lockStockCount;
    private List<ProductWStock> productWStockList;
    private List<PostSTQueue> postSTQueueList;
    private PostStockTriggerQueueDao postStockTriggerQueueDao;
    private ProductWarehouseStockDao productWarehouseStockDao;

    public MainStockPostService() {
        this.lockStockCount = 0;
        this.productWStockList = new LinkedList<>();
        this.postSTQueueList = new LinkedList<>();
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    //虚拟捆绑商品类型
    private final static Integer VIRTUAL_PRODUCT_TYPE = 9;


    public void setPostStockTriggerQueueDao(PostStockTriggerQueueDao postStockTriggerQueueDao) {
        this.postStockTriggerQueueDao = postStockTriggerQueueDao;
    }


    public void setProductWarehouseStockDao(ProductWarehouseStockDao productWarehouseStockDao) {
        this.productWarehouseStockDao = productWarehouseStockDao;
    }

    @Override
    public Integer call() throws Exception {
        try {
            return postStock(this.order);
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                log.info("orderId:{} had already be inserted into post_stock_trigger_queue!" + this.order.getOrderId());
                return SystemConstant.RESP_OK;
            }
            ErrorInfo.builder().module(SystemConstant.MODULE_NAME)
                    .addParam("order", order + "")
                    .exception(e).build().error(log);
            throw new RuntimeException(e);
        }
    }


    @Transactional
    public Integer postStock(Order order) {
        buildSqlData(order);
        int count = this.postStockTriggerQueueDao.insertQueueBatch(this.postSTQueueList);
        if (count < this.postSTQueueList.size()) {
            throw new RuntimeException("orderId:" + order.getOrderId() + ",insert post_stock_trigger_queue failed!");
        }
        count = this.productWarehouseStockDao.updateStockBatch(this.productWStockList);
        if (count < this.productWStockList.size() + this.lockStockCount) {
            throw new RuntimeException("orderId:" + order.getOrderId() + ",update product_warehouse_stock failed!");
        }
        return SystemConstant.RESP_OK;
    }


    private void buildSqlData(Order order) {
        Map<String, PostSTQueue> queueMap = new HashMap<>();
        Map<String, ProductWStock> stockMap = new HashMap<>();
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
    }
}



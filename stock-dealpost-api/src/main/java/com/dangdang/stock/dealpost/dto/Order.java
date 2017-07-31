package com.dangdang.stock.dealpost.dto;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
public class Order implements Serializable {

    private long orderId;
    private Date postTime;
    private String cartPostStockId;
    private boolean isNoConsistency;
    private List<Product> productList;

    /**
     * Getter for property 'orderId'.
     *
     * @return Value for property 'orderId'.
     */
    public long getOrderId() {
        return orderId;
    }

    /**
     * Setter for property 'orderId'.
     *
     * @param orderId Value to set for property 'orderId'.
     */
    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    /**
     * Getter for property 'postTime'.
     *
     * @return Value for property 'postTime'.
     */
    public Date getPostTime() {
        return postTime;
    }

    /**
     * Setter for property 'postTime'.
     *
     * @param postTime Value to set for property 'postTime'.
     */
    public void setPostTime(Date postTime) {
        this.postTime = postTime;
    }

    /**
     * Getter for property 'cartPostStockId'.
     *
     * @return Value for property 'cartPostStockId'.
     */
    public String getCartPostStockId() {
        return cartPostStockId;
    }

    /**
     * Setter for property 'cartPostStockId'.
     *
     * @param cartPostStockId Value to set for property 'cartPostStockId'.
     */
    public void setCartPostStockId(String cartPostStockId) {
        this.cartPostStockId = cartPostStockId;
    }

    /**
     * Getter for property 'noConsistency'.
     *
     * @return Value for property 'noConsistency'.
     */
    public boolean isNoConsistency() {
        return isNoConsistency;
    }

    /**
     * Setter for property 'noConsistency'.
     *
     * @param noConsistency Value to set for property 'noConsistency'.
     */
    public void setNoConsistency(boolean noConsistency) {
        isNoConsistency = noConsistency;
    }

    /**
     * Getter for property 'productList'.
     *
     * @return Value for property 'productList'.
     */
    public List<Product> getProductList() {
        return productList;
    }

    /**
     * Setter for property 'productList'.
     *
     * @param productList Value to set for property 'productList'.
     */
    public void setProductList(List<Product> productList) {
        this.productList = productList;
    }

    @Override
    public String toString() {
        String postTimeStr = "";
        if (postTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            postTimeStr = sdf.format(postTime);
        }
        return "Order{" +
                "orderId=" + orderId +
                ", postTime=" + postTimeStr +
                ", cartPostStockId='" + cartPostStockId + '\'' +
                ", isNoConsistency=" + isNoConsistency +
                ", productList=" + productList +
                '}';
    }
}

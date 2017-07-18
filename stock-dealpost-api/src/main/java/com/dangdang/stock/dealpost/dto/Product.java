package com.dangdang.stock.dealpost.dto;

import java.io.Serializable;

/**
 * Created by zhanghaihua on 2017/3/2.
 */


public class Product implements Serializable, Comparable<Product> {
    private long productId;
    private int warehouseId;
    private int opNum;
    private Integer stockTypeId;
    private String sourceIds;
    private Integer isPreSale;
    private Integer postType;
    private Integer productType;
    private Integer parentId;

    /**
     * Getter for property 'productId'.
     *
     * @return Value for property 'productId'.
     */
    public long getProductId() {
        return productId;
    }

    /**
     * Setter for property 'productId'.
     *
     * @param productId Value to set for property 'productId'.
     */
    public void setProductId(long productId) {
        this.productId = productId;
    }

    /**
     * Getter for property 'warehouseId'.
     *
     * @return Value for property 'warehouseId'.
     */
    public int getWarehouseId() {
        return warehouseId;
    }

    /**
     * Setter for property 'warehouseId'.
     *
     * @param warehouseId Value to set for property 'warehouseId'.
     */
    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    /**
     * Getter for property 'opNum'.
     *
     * @return Value for property 'opNum'.
     */
    public int getOpNum() {
        return opNum;
    }

    /**
     * Setter for property 'opNum'.
     *
     * @param opNum Value to set for property 'opNum'.
     */
    public void setOpNum(int opNum) {
        this.opNum = opNum;
    }

    /**
     * Getter for property 'stockTypeId'.
     *
     * @return Value for property 'stockTypeId'.
     */
    public Integer getStockTypeId() {
        return stockTypeId;
    }

    /**
     * Setter for property 'stockTypeId'.
     *
     * @param stockTypeId Value to set for property 'stockTypeId'.
     */
    public void setStockTypeId(Integer stockTypeId) {
        this.stockTypeId = stockTypeId;
    }

    /**
     * Getter for property 'sourceIds'.
     *
     * @return Value for property 'sourceIds'.
     */
    public String getSourceIds() {
        return sourceIds;
    }

    /**
     * Setter for property 'sourceIds'.
     *
     * @param sourceIds Value to set for property 'sourceIds'.
     */
    public void setSourceIds(String sourceIds) {
        this.sourceIds = sourceIds;
    }

    /**
     * Getter for property 'isPreSale'.
     *
     * @return Value for property 'isPreSale'.
     */
    public Integer getIsPreSale() {
        return isPreSale;
    }

    /**
     * Setter for property 'isPreSale'.
     *
     * @param isPreSale Value to set for property 'isPreSale'.
     */
    public void setIsPreSale(Integer isPreSale) {
        this.isPreSale = isPreSale;
    }

    /**
     * Getter for property 'postType'.
     *
     * @return Value for property 'postType'.
     */
    public Integer getPostType() {
        return postType;
    }

    /**
     * Setter for property 'postType'.
     *
     * @param postType Value to set for property 'postType'.
     */
    public void setPostType(Integer postType) {
        this.postType = postType;
    }

    /**
     * Getter for property 'productType'.
     *
     * @return Value for property 'productType'.
     */
    public Integer getProductType() {
        return productType;
    }

    /**
     * Setter for property 'productType'.
     *
     * @param productType Value to set for property 'productType'.
     */
    public void setProductType(Integer productType) {
        this.productType = productType;
    }

    /**
     * Getter for property 'parentId'.
     *
     * @return Value for property 'parentId'.
     */
    public Integer getParentId() {
        return parentId;
    }

    /**
     * Setter for property 'parentId'.
     *
     * @param parentId Value to set for property 'parentId'.
     */
    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", warehouseId=" + warehouseId +
                ", opNum=" + opNum +
                ", stockTypeId=" + stockTypeId +
                ", sourceIds='" + sourceIds + '\'' +
                ", isPreSale=" + isPreSale +
                ", postType=" + postType +
                ", productType=" + productType +
                ", parentId=" + parentId +
                '}';
    }

    @Override
    public int compareTo(Product o) {
        if (this.productId != o.getProductId()) {
            return (this.productId < o.getProductId()) ? -1 : ((this.productId == o.getProductId()) ? 0 : 1);
        }
        if (this.warehouseId != o.getWarehouseId()) {
            return (this.warehouseId < o.getWarehouseId()) ? -1 : ((this.warehouseId == o.getWarehouseId()) ? 0 : 1);
        }
        return 0;
    }
}

package com.dangdang.stock.dealpost.dto;

import java.io.Serializable;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
public class Product implements Serializable {
    private long productId;
    private int warehouseId;
    private int opNum;
    private int stockTypeId;
    private String sourceIds;
    private boolean isPreSale;
    private boolean isTsOrAllotType;
    private int productType;
    private int parentId;

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
    public int getStockTypeId() {
        return stockTypeId;
    }

    /**
     * Setter for property 'stockTypeId'.
     *
     * @param stockTypeId Value to set for property 'stockTypeId'.
     */
    public void setStockTypeId(int stockTypeId) {
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
     * Getter for property 'preSale'.
     *
     * @return Value for property 'preSale'.
     */
    public boolean isPreSale() {
        return isPreSale;
    }

    /**
     * Setter for property 'preSale'.
     *
     * @param preSale Value to set for property 'preSale'.
     */
    public void setPreSale(boolean preSale) {
        isPreSale = preSale;
    }

    /**
     * Getter for property 'tsOrAllotType'.
     *
     * @return Value for property 'tsOrAllotType'.
     */
    public boolean isTsOrAllotType() {
        return isTsOrAllotType;
    }

    /**
     * Setter for property 'tsOrAllotType'.
     *
     * @param tsOrAllotType Value to set for property 'tsOrAllotType'.
     */
    public void setTsOrAllotType(boolean tsOrAllotType) {
        isTsOrAllotType = tsOrAllotType;
    }

    /**
     * Getter for property 'productType'.
     *
     * @return Value for property 'productType'.
     */
    public int getProductType() {
        return productType;
    }

    /**
     * Setter for property 'productType'.
     *
     * @param productType Value to set for property 'productType'.
     */
    public void setProductType(int productType) {
        this.productType = productType;
    }

    /**
     * Getter for property 'parentId'.
     *
     * @return Value for property 'parentId'.
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * Setter for property 'parentId'.
     *
     * @param parentId Value to set for property 'parentId'.
     */
    public void setParentId(int parentId) {
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
                ", isTsOrAllotType=" + isTsOrAllotType +
                ", productType=" + productType +
                ", parentId=" + parentId +
                '}';
    }
}

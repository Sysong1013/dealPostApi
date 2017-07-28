package com.dangdang.stock.dealpost.dto;

import java.io.Serializable;

/**
 * Created by zhanghaihua on 2017/3/2.
 */

public class ResponseDTO implements Serializable {

    private int errorCode;
    private String errorMessage;

    public ResponseDTO() {
    }

    public ResponseDTO(Integer errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Getter for property 'errorCode'.
     *
     * @return Value for property 'errorCode'.
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * Setter for property 'errorCode'.
     *
     * @param errorCode Value to set for property 'errorCode'.
     */
    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Getter for property 'errorMessage'.
     *
     * @return Value for property 'errorMessage'.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Setter for property 'errorMessage'.
     *
     * @param errorMessage Value to set for property 'errorMessage'.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ResponseDTO{" +
                "errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

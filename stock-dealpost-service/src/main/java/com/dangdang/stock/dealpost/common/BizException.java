package com.dangdang.stock.dealpost.common;

import lombok.Getter;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 11:59 2017/7/28
 * @Company: Dangdang
 * @Modified By:
 */
@Getter
public class BizException extends RuntimeException{
    private int code;
    private String message;

    public final static int MAIN_POST_EXCEPTION =100;
    public final static int PARAMETER_INVALID_EXCEPTION = 101;

    public BizException(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

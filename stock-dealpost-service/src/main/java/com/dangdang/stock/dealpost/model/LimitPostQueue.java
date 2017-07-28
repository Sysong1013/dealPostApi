package com.dangdang.stock.dealpost.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 11:58 2017/7/13
 * @Company: Dangdang
 * @Modified By:
 */
@Setter
@Getter
@ToString
public class LimitPostQueue {
    private long orderId;
    private long productId;
    private int warehouseId;
    private int opNum;
    private int stockTypeId;
    private Date orderTime;
    private String source;
    private String sourceIds;
}

package com.dangdang.stock.dealpost.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
@Setter
@Getter
@ToString
public class PostSTQueue {
    private Long orderId;
    private Long productId;
    private Integer warehouseId;
    private Integer opNum;
    private Integer stockTypeId;
    private String cartPostStockId;
    private Integer effectPostStatus;
}

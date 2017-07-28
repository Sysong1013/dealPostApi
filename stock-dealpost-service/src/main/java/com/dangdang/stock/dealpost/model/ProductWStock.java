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
public class ProductWStock {
    private long productId;
    private int warehouseId;
    private int opNum;
    private int stockTypeId;
    private int effectPostQuantity;
}

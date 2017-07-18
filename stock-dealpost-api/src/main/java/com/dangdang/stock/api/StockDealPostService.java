package com.dangdang.stock.api;


import com.dangdang.stock.dealpost.dto.Order;
import com.dangdang.stock.dealpost.dto.ResponseDTO;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
public interface StockDealPostService {

    ResponseDTO postStock(Order order);
}

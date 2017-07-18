package com.dangdang.stock.dealpost.dao;


import com.dangdang.stock.dealpost.model.ProductWStock;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
@Repository
public interface ProductWarehouseStockMapper {
    int updateStockBatch(List<ProductWStock> productWStockList);
}

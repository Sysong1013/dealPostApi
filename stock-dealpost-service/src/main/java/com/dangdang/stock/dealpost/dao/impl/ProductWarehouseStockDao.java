package com.dangdang.stock.dealpost.dao.impl;

import com.dangdang.stock.dealpost.dao.ProductWarehouseStockMapper;
import com.dangdang.stock.dealpost.model.ProductWStock;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 12:44 2017/7/17
 * @Company: Dangdang
 * @Modified By:
 */
@Component
public class ProductWarehouseStockDao implements ProductWarehouseStockMapper {
    @Resource ProductWarehouseStockMapper productWarehouseStockMapper;
    @Override
    public int updateStockBatch(List<ProductWStock> productWStockList) {
        return productWarehouseStockMapper.updateStockBatch(productWStockList);
    }
}

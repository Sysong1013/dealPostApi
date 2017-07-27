package com.dangdang.stock.dealpost.dao;


import com.dangdang.stock.dealpost.model.ProductWStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 12:44 2017/7/17
 * @Company: Dangdang
 * @Modified By:
 */
@Repository
public class ProductWarehouseStockDao {
    @Resource
    private JdbcTemplate jdbcTemplate;

    private final String sql1 =
            "UPDATE product_warehouse_stock \n" +
                    "SET post_stock_quantity = IFNULL(post_stock_quantity, 0) + %d," +
                    "   effect_post_quantity = IFNULL(effect_post_quantity,0) + %d," +
                    "   post_last_changed_date = now(),last_changed_date = now() \n" +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = 0 " +
                    "   AND (stock_quantity + IFNULL(stock_quantity_ts,0) + IFNULL(stock_quantity_allot,0) " +
                    "- IFNULL(post_stock_quantity,0) >= %d)";

    private final String sql2 =
            "UPDATE product_warehouse_stock " +
                    "SET effect_post_quantity = IFNULL(effect_post_quantity,0) + %d " +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = 0";

    private final String sql3 =
            "UPDATE product_warehouse_stock " +
                    "SET post_stock_quantity = IFNULL(post_stock_quantity,0) + %d" +
                    "   ,post_last_changed_date = now(),last_changed_date = now() " +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = %d";

    public int[] updateStockBatch(List<ProductWStock> productWStockList) {
        return jdbcTemplate.batchUpdate(buildSql(productWStockList));
    }


    private String[] buildSql(List<ProductWStock> productWStockList) {
        ArrayList<String> strArr = new ArrayList<>();
        for (ProductWStock productWStock : productWStockList) {
            Long productId = productWStock.getProductId();
            Integer warehouseId = productWStock.getWarehouseId();
            Integer opNum = productWStock.getOpNum();
            Integer effectPostQuantity = productWStock.getEffectPostQuantity();
            Integer stockTypeId = productWStock.getStockTypeId();
            if (stockTypeId.equals(0)) {
                strArr.add(String.format(sql1, opNum, effectPostQuantity, productId, warehouseId, opNum));
            } else if (stockTypeId.compareTo(0) == 1) {
                strArr.add(String.format(sql2, effectPostQuantity, productId, warehouseId));
                strArr.add(String.format(sql3, opNum, productId, warehouseId, stockTypeId));
            }
        }
        return strArr.toArray(new String[strArr.size()]);
    }


}

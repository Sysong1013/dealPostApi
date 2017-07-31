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

    private final static String SQL1 =
            "UPDATE product_warehouse_stock " +
                    "SET post_stock_quantity = IFNULL(post_stock_quantity, 0) + %d," +
                    "   effect_post_quantity = IFNULL(effect_post_quantity,0) + %d," +
                    "   post_last_changed_date = now(),last_changed_date = now() " +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = 0 " +
                    "   AND (stock_quantity + IFNULL(stock_quantity_ts,0) + IFNULL(stock_quantity_allot,0) " +
                    "- IFNULL(post_stock_quantity,0) >= %d)";

    private final static String SQL1_NO_CONSISTENCY =
            "UPDATE product_warehouse_stock " +
                    "SET post_stock_quantity = IFNULL(post_stock_quantity, 0) + %d," +
                    "   effect_post_quantity = IFNULL(effect_post_quantity,0) + %d," +
                    "   post_last_changed_date = now(),last_changed_date = now() " +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = 0";

    private final static String SQL2 =
            "UPDATE product_warehouse_stock " +
                    "SET effect_post_quantity = IFNULL(effect_post_quantity,0) + %d " +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = 0";

    private final static String SQL3 =
            "UPDATE product_warehouse_stock " +
                    "SET post_stock_quantity = IFNULL(post_stock_quantity,0) + %d" +
                    "   ,post_last_changed_date = now(),last_changed_date = now() " +
                    "WHERE product_id = %d AND warehouse_id = %d AND stock_type_id = %d";

    public int[] updateStockBatch(List<ProductWStock> productWStockList, boolean isNoConsistency) {
        ArrayList<String> strArr = new ArrayList<>();
        for (ProductWStock productWStock : productWStockList) {
            long productId = productWStock.getProductId();
            int warehouseId = productWStock.getWarehouseId();
            int opNum = productWStock.getOpNum();
            int effectPostQuantity = productWStock.getEffectPostQuantity();
            int stockTypeId = productWStock.getStockTypeId();
            if (stockTypeId == 0) {
                if (!isNoConsistency) {
                    strArr.add(String.format(SQL1, opNum, effectPostQuantity, productId, warehouseId, opNum));
                } else {
                    strArr.add(String.format(SQL1_NO_CONSISTENCY, opNum, effectPostQuantity, productId, warehouseId));
                }
            } else {
                strArr.add(String.format(SQL2, effectPostQuantity, productId, warehouseId));
                strArr.add(String.format(SQL3, opNum, productId, warehouseId, stockTypeId));
            }
        }
        return jdbcTemplate.batchUpdate(strArr.toArray(new String[strArr.size()]));
    }

}

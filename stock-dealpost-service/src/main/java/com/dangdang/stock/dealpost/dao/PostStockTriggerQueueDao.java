package com.dangdang.stock.dealpost.dao;

import com.dangdang.stock.dealpost.model.PostSTQueue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 12:44 2017/7/17
 * @Company: Dangdang
 * @Modified By:
 */
@Repository
public class PostStockTriggerQueueDao {
    @Resource
    private JdbcTemplate jdbcTemplate;

    private final String sql = "INSERT INTO " +
            "post_stock_trigger_queue_bak (order_id,product_id,warehouse_id,op_num,op_source_id,cart_post_stock_id" +
            ",cart_post_status,stock_type_id,creation_date,last_changed_date,cart_post_date,effect_post_status) " +
            "VALUES ";

    public void insertTriggerQueueBatch(List<PostSTQueue> postSTQueueList) {
        jdbcTemplate.execute(buildSql(postSTQueueList));
    }

    private String buildSql(List<PostSTQueue> postSTQueueList) {
        StringBuilder sb = new StringBuilder();
        int size = postSTQueueList.size();
        int index = 0;
        for (PostSTQueue postSTQueue : postSTQueueList) {
            index++;
            sb.append("(");
            sb.append(postSTQueue.getOrderId());
            sb.append(",").append(postSTQueue.getProductId());
            sb.append(",").append(postSTQueue.getWarehouseId());
            sb.append(",").append(postSTQueue.getOpNum());
            sb.append(",2");
            sb.append(",").append(postSTQueue.getCartPostStockId());
            sb.append(",0");
            sb.append(",").append(postSTQueue.getStockTypeId());
            sb.append(",now(),now(),now()");
            sb.append(",").append(postSTQueue.getEffectPostStatus());
            if (index < size) {
                sb.append("),");
            } else {
                sb.append(")");
            }
        }
        return sql + sb.toString();
    }
}

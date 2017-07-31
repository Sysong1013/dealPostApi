package com.dangdang.stock.dealpost.dao;

import com.dangdang.stock.dealpost.model.PostSTQueue;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    private final static String SQL = "INSERT INTO " +
            "post_stock_trigger_queue_bak (order_id,product_id,warehouse_id,op_num,op_source_id,cart_post_stock_id" +
            ",cart_post_status,stock_type_id,creation_date,last_changed_date,cart_post_date,effect_post_status) " +
            "VALUES (?,?,?,?,2,?,0,?,now(),now(),now(),?)";

    public void insertTriggerQueueBatch(final List<PostSTQueue> postSTQueueList) {

        jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                PostSTQueue postSTQueue = postSTQueueList.get(i);
                preparedStatement.setLong(1, postSTQueue.getOrderId());
                preparedStatement.setLong(2, postSTQueue.getProductId());
                preparedStatement.setInt(3, postSTQueue.getWarehouseId());
                preparedStatement.setInt(4, postSTQueue.getOpNum());
                preparedStatement.setString(5, postSTQueue.getCartPostStockId());
                preparedStatement.setInt(6, postSTQueue.getStockTypeId());
                preparedStatement.setInt(7, postSTQueue.getEffectPostStatus());
            }

            @Override
            public int getBatchSize() {
                return postSTQueueList.size();
            }
        });
    }

}

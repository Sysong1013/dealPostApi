package com.dangdang.stock.dealpost.dao;

import com.dangdang.stock.dealpost.model.LimitPostQueue;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 12:44 2017/7/17
 * @Company: Dangdang
 * @Modified By:
 */
@Repository
public class LimitPostQueueDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private final static String SQL = "INSERT INTO limit_post_queue " +
            "(order_id,product_id,warehouse_id,op_num,stock_type_id,order_time,source,source_id,delmark,creation_date)" +
            "VALUES (?,?,?,?,?,?,?,?,0,now())";

    public void insertLimitQueueBatch(final List<LimitPostQueue> limitPostQueueList) {

        jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                LimitPostQueue limitPostQueue = limitPostQueueList.get(i);
                preparedStatement.setLong(1, limitPostQueue.getOrderId());
                preparedStatement.setLong(2, limitPostQueue.getProductId());
                preparedStatement.setInt(3, limitPostQueue.getWarehouseId());
                preparedStatement.setInt(4, limitPostQueue.getOpNum());
                preparedStatement.setInt(5, limitPostQueue.getStockTypeId());
                preparedStatement.setTimestamp(6, new Timestamp(limitPostQueue.getOrderTime().getTime()));
                preparedStatement.setString(7, limitPostQueue.getSource());
                preparedStatement.setString(8, limitPostQueue.getSourceIds());
            }

            @Override
            public int getBatchSize() {
                return limitPostQueueList.size();
            }

        });
    }


}

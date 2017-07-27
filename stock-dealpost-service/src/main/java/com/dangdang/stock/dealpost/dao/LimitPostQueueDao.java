package com.dangdang.stock.dealpost.dao;

import com.dangdang.stock.dealpost.model.LimitPostQueue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
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

    private final String sql = "INSERT INTO limit_post_queue " +
            "(order_id,product_id,warehouse_id,op_num,stock_type_id,order_time,source,source_id,delmark,creation_date)" +
            "VALUES ";

    public void insertLimitQueueBatch(List<LimitPostQueue> limitPostQueueList) {
        jdbcTemplate.execute(buildSql(limitPostQueueList));
    }

    private String buildSql(List<LimitPostQueue> limitPostQueueList) {
        StringBuilder sb = new StringBuilder();
        int size = limitPostQueueList.size();
        int index = 0;
        for (LimitPostQueue limitPostQueue : limitPostQueueList) {
            index++;
            sb.append("(");
            sb.append(limitPostQueue.getOrderId());
            sb.append(",").append(limitPostQueue.getProductId());
            sb.append(",").append(limitPostQueue.getWarehouseId());
            sb.append(",").append(limitPostQueue.getOpNum());
            sb.append(",").append(limitPostQueue.getStockTypeId());

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sb.append(",'").append(simpleDateFormat.format(limitPostQueue.getOrderTime()));
            sb.append("',").append(limitPostQueue.getSource());
            sb.append(",").append(limitPostQueue.getSourceIds());
            sb.append(",0,now()");
            if (index < size) {
                sb.append("),");
            } else {
                sb.append(")");
            }
        }
        return sql + sb.toString();
    }
}

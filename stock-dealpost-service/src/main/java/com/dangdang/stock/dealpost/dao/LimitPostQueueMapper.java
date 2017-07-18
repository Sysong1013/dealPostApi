package com.dangdang.stock.dealpost.dao;

import com.dangdang.stock.dealpost.model.LimitPostQueue;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 15:23 2017/7/6
 * @Company: Dangdang
 * @Modified By:
 */
@Repository
public interface LimitPostQueueMapper {
    int insertQueueBatch(List<LimitPostQueue> limitPostQueueList);
}

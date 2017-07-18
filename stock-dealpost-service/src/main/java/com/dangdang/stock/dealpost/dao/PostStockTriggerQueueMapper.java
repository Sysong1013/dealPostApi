package com.dangdang.stock.dealpost.dao;


import com.dangdang.stock.dealpost.model.PostSTQueue;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by zhanghaihua on 2017/3/2.
 */
@Repository
public interface PostStockTriggerQueueMapper {
    int insertQueueBatch(List<PostSTQueue> postSTQueueList);
}

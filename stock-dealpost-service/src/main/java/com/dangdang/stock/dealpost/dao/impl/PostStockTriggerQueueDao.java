package com.dangdang.stock.dealpost.dao.impl;

import com.dangdang.stock.dealpost.dao.PostStockTriggerQueueMapper;
import com.dangdang.stock.dealpost.model.PostSTQueue;
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
public class PostStockTriggerQueueDao implements PostStockTriggerQueueMapper {
    @Resource PostStockTriggerQueueMapper postStockTriggerQueueMapper;

    @Override
    public int insertQueueBatch(List<PostSTQueue> postSTQueueList) {
       return postStockTriggerQueueMapper.insertQueueBatch(postSTQueueList);
    }
}

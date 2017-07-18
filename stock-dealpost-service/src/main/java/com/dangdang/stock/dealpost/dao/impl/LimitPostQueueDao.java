package com.dangdang.stock.dealpost.dao.impl;

import com.dangdang.stock.dealpost.dao.LimitPostQueueMapper;
import com.dangdang.stock.dealpost.model.LimitPostQueue;
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
public class LimitPostQueueDao implements LimitPostQueueMapper {
    @Resource LimitPostQueueMapper limitPostQueueMapper;


    @Override
    public int insertQueueBatch(List<LimitPostQueue> limitPostQueueList) {
        return limitPostQueueMapper.insertQueueBatch(limitPostQueueList);
    }
}

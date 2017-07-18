package com.dangdang.stock.dealpost.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 14:25 2017/6/26
 * @Company: Dangdang
 * @Modified By:
 */
@Slf4j
@Component
public class SlaLog {
    public void log(String str, Object... objects) {
        log.info(str, objects);
    }
}

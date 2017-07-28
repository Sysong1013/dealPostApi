package com.dangdang.stock.dealpost.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 21:23 2017/7/14
 * @Company: Dangdang
 * @Modified By:
 */
@Component
public class ApplicationContextProvider  implements ApplicationContextAware {

    private static ApplicationContext context = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }
}

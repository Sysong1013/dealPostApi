package com.dangdang.stock.dealpost.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 19:25 2017/7/11
 * @Company: Dangdang
 * @Modified By:
 */
public class ThreadPoolUtil
{

    private static int CORE_POOL_SIZE =5;
    private static int MAX_SIZE = 20;
    private static int KEEP_ALIVE_TIME = 30;
    private ThreadPoolUtil(){};

    private volatile static ThreadPoolExecutor threadPoolExecutor;

    public static ThreadPoolExecutor getThreadPool(){
        if(threadPoolExecutor == null){
            synchronized (ThreadPoolUtil.class){
                if(threadPoolExecutor == null){
                    threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,MAX_SIZE,KEEP_ALIVE_TIME
                            , TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(2*CORE_POOL_SIZE)
                            , new ThreadPoolExecutor.CallerRunsPolicy());
                }
            }
        }
        return threadPoolExecutor;
    }
}

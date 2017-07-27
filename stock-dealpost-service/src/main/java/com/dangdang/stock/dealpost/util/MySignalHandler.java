package com.dangdang.stock.dealpost.util;

import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 17:08 2017/7/17
 * @Company: Dangdang
 * @Modified By:
 */
@Slf4j
public class MySignalHandler {
    public void init() {
        SubSignalHandler subSignalHandler = new SubSignalHandler();
        // 注册对指定信号的处理
        Signal.handle(new Signal("TERM"), subSignalHandler);    // kill or kill -15
        Signal.handle(new Signal("INT"), subSignalHandler);     // kill -2
    }

    @SuppressWarnings("restriction")
    class SubSignalHandler implements SignalHandler {

        @Override
        public void handle(Signal signal) {
            for (ExchangeServer server : DubboProtocol.getDubboProtocol().getServers()) {
                server.close();
            }
            // signal name
            String name = signal.getName();
            // signal number
            int number = signal.getNumber();
            log.info("Received signal: {}  == kill -{}", name, number);
            if (name.equals("TERM") || name.equals("INT")) {
                ThreadPoolExecutor executor = ThreadPoolUtil.getThreadPool();
                executor.shutdown();
                try {
                    executor.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
                if (!executor.isTerminated()) {
                    executor.shutdownNow();
                }
                System.exit(0);
            }
        }
    }
}



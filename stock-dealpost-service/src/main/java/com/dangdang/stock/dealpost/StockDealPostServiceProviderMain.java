package com.dangdang.stock.dealpost;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.container.Container;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class StockDealPostServiceProviderMain {

    public static final String CONTAINER_KEY = "dubbo.container";

    public static final String SHUTDOWN_HOOK_KEY = "dubbo.shutdown.hook";

    private static final ExtensionLoader<Container> loader = ExtensionLoader.getExtensionLoader(Container.class);

    private static volatile boolean running = true;

    private static Class CLAZZ = StockDealPostServiceProviderMain.class;

    public StockDealPostServiceProviderMain() {
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                String config = ConfigUtils.getProperty(CONTAINER_KEY, loader.getDefaultExtensionName());
                args = Constants.COMMA_SPLIT_PATTERN.split(config);
            }

            final List<com.alibaba.dubbo.container.Container> containers = new ArrayList<Container>();
            for (int i = 0; i < args.length; i++) {
                containers.add(loader.getExtension(args[i]));
            }
            log.info("Use container type(" + Arrays.toString(args) + ") to run dubbo serivce.");

            if ("true".equals(System.getProperty(SHUTDOWN_HOOK_KEY))) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        for (com.alibaba.dubbo.container.Container container : containers) {
                            try {
                                container.stop();
                                log.info("Dubbo " + container.getClass().getSimpleName() + " stopped!");
                            } catch (Throwable t) {
                                log.error(t.getMessage(), t);
                            }
                            synchronized (CLAZZ) {
                                running = false;
                                CLAZZ.notify();
                            }
                        }
                    }
                });
            }

            for (com.alibaba.dubbo.container.Container container : containers) {
                container.start();
                log.info("####################### Dubbo " + container.getClass().getSimpleName()
                        + " started!#######################");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            System.exit(1);
        }
        synchronized (CLAZZ) {
            while (running) {
                try {
                    CLAZZ.wait();
                } catch (Throwable e) {
                }
            }
        }
    }
}

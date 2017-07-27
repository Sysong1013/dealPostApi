package com.dangdang.stock.dealpost.common;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xnio.Options;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 18:05 2017/5/23
 * @Company: Dangdang
 * @Modified By:
 */
@Component
public class HealthCheck {

    @Value("${healthCheck.port}")
    private int port;

    private final static String HEALTH_CHECK_URL = "/dealpost/healthcheck";

    public void init() {
        PathHandler handler = new PathHandler();
        handler.addExactPath(HEALTH_CHECK_URL, new HttpHandler() {

            final Date uptime = new Date();

            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                }

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain;charset=UTF-8");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'Z");
                Date now = new Date();
                String str = String.format("服务器正常. 启动时间: %s, 当前时间: %s, 累计运行时间: %d 秒\n",
                        sdf.format(uptime), sdf.format(now), (now.getTime() - uptime.getTime()) / 1000);
                Runtime rt = Runtime.getRuntime();
                str += String.format("JVM内存情况: 总共: %.1f MB, 剩余: %.1f MB, 最大可用: %.1f MB\n",
                        rt.totalMemory() / 1048576.0, rt.freeMemory() / 1048576.0, rt.maxMemory() / 1048576.0);
                exchange.getResponseSender().send(str);
            }
        });

        final Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setWorkerOption(Options.THREAD_DAEMON, true)
                .setIoThreads(1)
                .setWorkerThreads(3)
                .setHandler(handler).build();
        server.start();

    }
}

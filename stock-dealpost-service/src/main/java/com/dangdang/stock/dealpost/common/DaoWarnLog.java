package com.dangdang.stock.dealpost.common;

import com.dangdang.modules.hestia.logger.ErrorInfo;
import com.dangdang.stock.dealpost.util.SystemConstant;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

/**
 * @Author: zhanghaihua
 * @Discription:
 * @Date: Created in 16:09 2017/5/26
 * @Company: Dangdang
 * @Modified By:
 */
@Setter
@Slf4j
public class DaoWarnLog {

    @Value("${application.dao.warnTime}")
    private long warnTime;

    public Object warnLog(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        String args = Arrays.toString(pjp.getArgs());
        Object result = pjp.proceed();
        String methodName = pjp.getSignature().getName();
        long useTime = System.currentTimeMillis() - startTime;
        if (useTime >= warnTime) {
            ErrorInfo.builder().message(String.format("DAO operated using too long times!"))
                    .module(SystemConstant.APP_NAME)
                    .addParam("methodName", methodName)
                    .addParam("args", args)
                    .addParam("warnTime", warnTime + "")
                    .addParam("useTimes", useTime + "")
                    .addParam("signature", pjp.getSignature().toString())
                    .build().warn(log);
        }
        return result;
    }

}

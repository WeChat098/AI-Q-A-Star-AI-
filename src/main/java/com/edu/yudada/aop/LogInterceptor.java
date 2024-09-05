package com.edu.yudada.aop;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求响应日志 AOP  日志校验的AOP
 *

 **/
@Aspect // 将该类定义成一个AOP切面用于定义拦截规则
@Component
@Slf4j
public class LogInterceptor {

    /**
     * 执行拦截
     */
    // 定义一个环绕通知 会在目标方法执行前后被调用 当前包下面的所有方法都会被进行拦截
    @Around("execution(* com.edu.yudada.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 获取请求路径
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 生成请求唯一 id
        String requestId = UUID.randomUUID().toString(); //生成一个唯一的ID，用于标识当前的请求
        String url = httpServletRequest.getRequestURI(); // 获取请求的URl
        // 获取请求参数
        Object[] args = point.getArgs(); // 获取请求参数
        String reqParam = "[" + StringUtils.join(args, ", ") + "]"; // 将请求参数转换为Json字符串
        // 输出请求日志
        log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
                httpServletRequest.getRemoteHost(), reqParam);
        // 执行原方法
        Object result = point.proceed();
        // 输出响应日志
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
        return result;
    }
}


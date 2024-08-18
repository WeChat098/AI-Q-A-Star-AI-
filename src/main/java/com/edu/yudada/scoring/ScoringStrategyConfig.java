package com.edu.yudada.scoring;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) //只有类上可以使用这个注解
@Retention(RetentionPolicy.RUNTIME)//这个注解在程序运行是可以通过反射获得该类的一些信息
@Component
public @interface ScoringStrategyConfig {// 对应评分策略的自定义注解

    /**
     * 应用类型
     * @return
     */
    int appType();

    /**
     * 评分策略
     * @return
     */
    int scoringStrategy();
}
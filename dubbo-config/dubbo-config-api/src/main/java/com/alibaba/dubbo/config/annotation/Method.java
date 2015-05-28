package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.*;

/**
 * Created by Lubin.Xuan on 2015/5/28.
 * ie.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Method {
    int timeout() default 1000;//单位毫秒
    int actives() default -1;// 最大并发调用
    int retries() default 0;// 重试次数
}
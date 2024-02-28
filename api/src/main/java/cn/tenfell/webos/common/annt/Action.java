package cn.tenfell.webos.common.annt;

import java.lang.annotation.*;

/**
 * 行为注解
 * 和@Bean配合实现url路由
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Action {
    String val();

    int type() default 0;
}

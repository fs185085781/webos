package cn.tenfell.webos.common.annt;

import java.lang.annotation.*;

/**
 * 登录控制注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Login {
    boolean val() default true;
}

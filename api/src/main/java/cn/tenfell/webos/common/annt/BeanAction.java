package cn.tenfell.webos.common.annt;

import java.lang.annotation.*;

/**
 * 实例注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeanAction {
    String val();
}

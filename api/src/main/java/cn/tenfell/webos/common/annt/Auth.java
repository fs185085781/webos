package cn.tenfell.webos.common.annt;

import java.lang.annotation.*;

/**
 * 权限标识注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auth {
    String[] val();
}

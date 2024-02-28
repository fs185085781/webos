package cn.tenfell.webos.common.annt;

import cn.hutool.db.transaction.TransactionLevel;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {
    TransactionLevel level() default TransactionLevel.READ_COMMITTED;
}

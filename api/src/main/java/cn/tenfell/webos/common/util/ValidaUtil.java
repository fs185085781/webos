package cn.tenfell.webos.common.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.Map;

/**
 * 校验工具类
 */
public class ValidaUtil {
    public static ValidaCheck init(Object obj) {
        ValidaCheck check = new ValidaCheck();
        check.setData(obj);
        return check;
    }

    @Data
    public static class ValidaCheck {
        private Object data;

        public ValidaCheck notBlank(String field, String name, boolean ignore) {
            if (ignore) {
                return this;
            }
            this.notBlank(field, name);
            return this;
        }

        public ValidaCheck notBlank(String field, String name) {
            Object val = ValidaUtil.getFieldValue(this.data, field);
            Assert.notNull(val, StrUtil.format("{}不可为空", name));
            Assert.notBlank(val.toString(), StrUtil.format("{}不可为空", name));
            return this;
        }

        public ValidaCheck notNull(String field, String name) {
            Object val = ValidaUtil.getFieldValue(this.data, field);
            Assert.notNull(val, StrUtil.format("{}不可为空", name));
            return this;
        }

        public ValidaCheck greater(String field, double val2, String name) {
            Object val = ValidaUtil.getFieldValue(this.data, field);
            Assert.notNull(val, StrUtil.format("{}不可为空", name));
            try {
                Double dv = Convert.toDouble(val);
                Assert.notNull(dv, StrUtil.format("{}必须为数值", name));
                Assert.isTrue(dv > val2, "{}必须大于{}", name, val2);
            } catch (Exception e) {
                Assert.isTrue(false, StrUtil.format("{}必须为数值", name));
            }
            return this;
        }
    }

    private static Object getFieldValue(Object obj, String field) {
        if (obj instanceof Map) {
            return ((Map) obj).get(field);
        } else {
            return ReflectUtil.getFieldValue(obj, field);
        }
    }
}

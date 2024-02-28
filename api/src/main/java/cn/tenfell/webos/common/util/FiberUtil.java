package cn.tenfell.webos.common.util;

import cn.hutool.core.thread.ThreadUtil;
import lombok.Data;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * 模拟协程,节省内存开销
 */
public class FiberUtil {
    private static final ExecutorService es;

    static {
        es = ThreadUtil.newExecutor(10, 20);
    }

    public static void run(Supplier action, FiberCallBack callback) {
        es.execute(() -> {
            if (callback != null) {
                FiberResult result = new FiberResult();
                try {
                    Object obj = action.get();
                    result.setRes(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.setError(e);
                }
                callback.result(result);
            } else {
                try {
                    action.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void run(Supplier action) {
        run(action, null);
    }

    @Data
    public static class FiberResult {
        private Exception error;
        private Object res;
    }

    public interface FiberCallBack {
        void result(FiberResult result);
    }
}

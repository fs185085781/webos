package cn.tenfell.webos.common.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.func.VoidFunc1;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.transaction.TransactionLevel;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.tenfell.webos.common.annt.*;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.modules.entity.SysUser;
import lombok.Data;
import org.noear.solon.core.handle.Context;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 代理类,用于实现事务,权限控制
 */
public class BeanProxy {
    private static final Log log = Log.get();
    private static boolean hasInit = false;
    private static final Map<String, BeanDefine> pool = new ConcurrentHashMap<>(32);

    @Data
    private static class BeanDefine {
        private Object bean;
        private String name;
        private Class type;
        private Map<String, MethodDefine> methodPool;
    }

    @Data
    private static class MethodDefine {
        private Method method;
        private int type;
        private boolean needLogin;
        private boolean needTransactional;
        private TransactionLevel transactionLevel;
        private Set<String> auths;
    }

    public static synchronized void init(Set<Class<?>> beans) {
        if (hasInit) {
            return;
        }
        for (Class clazz : beans) {
            BeanAction beanAction = (BeanAction) clazz.getAnnotation(BeanAction.class);
            if (beanAction == null || StrUtil.isBlank(beanAction.val())) {
                continue;
            }
            if (pool.get(beanAction.val()) != null) {
                throw new RuntimeException(beanAction.val() + "模块已经注册过,不允许重复注册");
            }
            BeanDefine proxyBean = new BeanDefine();
            proxyBean.setBean(ReflectUtil.newInstanceIfPossible(clazz));
            proxyBean.setName(beanAction.val());
            proxyBean.setType(clazz);
            Map<String, MethodDefine> methodPool = new ConcurrentHashMap<>();
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                Action action = method.getAnnotation(Action.class);
                if (action == null) {
                    continue;
                }
                if (methodPool.get(action.val()) != null) {
                    throw new RuntimeException(action.val() + "动作已经注册过,不允许重复注册");
                }
                MethodDefine methodDefine = new MethodDefine();
                methodDefine.setMethod(method);
                methodDefine.setType(action.type());
                Login login = method.getAnnotation(Login.class);
                methodDefine.setNeedLogin(login == null || login.val());
                Transactional transactional = method.getAnnotation(Transactional.class);
                methodDefine.setNeedTransactional(transactional != null);
                if (transactional != null) {
                    methodDefine.setTransactionLevel(transactional.level());
                }
                Auth auth = method.getAnnotation(Auth.class);
                if (auth != null && auth.val().length > 0) {
                    Set<String> auths = new HashSet<>();
                    String[] methodAuths = auth.val();
                    for (String methodAuth : methodAuths) {
                        auths.add(methodAuth);
                    }
                    methodDefine.setAuths(auths);
                }
                methodDefine.setNeedTransactional(transactional != null);
                methodPool.put(action.val(), methodDefine);
            }
            proxyBean.setMethodPool(methodPool);
            pool.put(beanAction.val(), proxyBean);
        }
        hasInit = true;
    }

    public static Object invoke(String module, String action, Context ctx) {
        try {
            return invokeThrow(module, action, ctx);
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
            return R.failed("服务器异常");
        }
    }

    public static Object invokeThrow(String module, String action, Context ctx) throws Exception{
        if (!ProjectUtil.hasInstall && !StrUtil.equals(module, "install")) {
            return R.noInstall();
        }
        if (StrUtil.isBlank(module) || StrUtil.isBlank(action)) {
            return R.noPath();
        }
        BeanDefine bean = pool.get(module);
        if (bean == null) {
            return R.noPath();
        }
        MethodDefine method = bean.methodPool.get(action);
        if (method == null) {
            return R.noPath();
        }
        if (method.isNeedLogin() && !isLogin()) {
            return R.noLogin();
        }
        if (!hasAuth(method.getAuths())) {
            return R.noAuth();
        }
        Class[] paramTypes = method.getMethod().getParameterTypes();
        if (paramTypes.length > 1) {
            return R.failed("参数错误");
        }
        Supplier invoke;
        if (paramTypes.length > 0) {
            Object param;
            Class clazz = paramTypes[0];
            if (method.getType() == 0) {
                param = ctx;
            } else if (method.getType() == 1) {
                String body = ctx.body();
                if (StrUtil.isNotBlank(body)) {
                    if (ClassUtil.isAssignable(Collection.class, clazz)) {
                        //集合
                        Class entityClass = (Class) ((ParameterizedTypeImpl) method.getMethod().getGenericParameterTypes()[0]).getActualTypeArguments()[0];
                        param = JSONUtil.toList(body, entityClass);
                    } else {
                        //非集合
                        param = JSONUtil.toBean(body, clazz);
                    }
                } else {
                    param = null;
                }
            } else if (method.getType() == 2) {
                Map<String, String> params = ctx.paramMap();
                param = BeanUtil.fillBeanWithMap(params, ReflectUtil.newInstanceIfPossible(clazz), true);
            } else {
                return R.failed("此类型暂不支持");
            }
            invoke = () -> {
                try {
                    return method.getMethod().invoke(bean.getBean(), param);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } else {
            invoke = () -> {
                try {
                    return method.getMethod().invoke(bean.getBean());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
        try {
            if (method.isNeedTransactional()) {
                //事务模式
                AtomicReference<Object> ares = new AtomicReference<>();
                DbUtil.get().tx(method.getTransactionLevel(), (VoidFunc1<Db>) db -> {
                    DbUtil.set(db);
                    ares.set(invoke.get());
                });
                return ares.get();
            } else {
                //非事务模式
                return invoke.get();
            }
        } catch (Exception e) {
            if (ProjectUtil.startConfig.getBool("debug")) {
                ProjectUtil.showConsoleErr(e);
            }
            return errorHandle(e);
        }
    }

    public static boolean isLogin() {
        SysUser user = LoginAuthUtil.getUser();
        return user != null;
    }

    public static boolean hasAuth(Set<String> methodAuths) {
        if (methodAuths == null || methodAuths.size() == 0) {
            return true;
        }
        if (!isLogin()) {
            return false;
        }
        List<String> userAuths = LoginAuthUtil.getUserAuths();
        if (CollUtil.isEmpty(userAuths)) {
            return false;
        }
        for (String methodAuth : methodAuths) {
            if (userAuths.contains(methodAuth)) {
                return true;
            }
        }
        return false;
    }

    private static R errorHandle(Throwable error) {
        Throwable root = ExceptionUtil.getRootCause(error);
        if (!(root instanceof IllegalArgumentException)) {
            log.error(root);
        }
        return R.error(root);
    }
}

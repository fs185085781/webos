package cn.tenfell.webos.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.setting.Setting;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DbUtil {
    private static boolean hasInit = false;
    private static ThreadLocal<Db> THREAD_LOCAL_DB = new ThreadLocal<>();
    //默认数据库缓存时间30分钟
    private static int DEFAULT_CACHE_TIME = 1800;

    public static Db get() {
        Db db = THREAD_LOCAL_DB.get();
        if (db == null) {
            db = Db.use();
            THREAD_LOCAL_DB.set(db);
        }
        Assert.notNull(db,"数据库不可为空");
        return db;
    }

    public static void set(Db db) {
        THREAD_LOCAL_DB.set(db);
    }

    public static void initMysql(String host, Integer port, String database, String user, String password) {
        if (hasInit) {
            return;
        }
        initSqlPool(StrUtil.format("jdbc:mysql://{}:{}/{}?useSSL=false&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8", host, port, database), user, password, "com.mysql.jdbc.Driver");
    }

    private static void initSqlPool(String url, String user, String password, String driver) {
        Setting setting = new Setting();
        setting.set("url", url);
        if (StrUtil.isNotBlank(user)) {
            setting.set("username", user);
        }
        if (StrUtil.isNotBlank(password)) {
            setting.set("password", password);
        }
        if (StrUtil.isNotBlank(driver)) {
            setting.set("driver", driver);
        }
        setting.set("autoCommit", "true");
        setting.set("connectionTimeout", "30000");
        setting.set("idleTimeout", "600000");
        setting.set("maxLifetime", "1800000");
        setting.set("connectionTestQuery", "SELECT 1");
        setting.set("minimumIdle", "1");
        setting.set("maximumPoolSize", "5");
        if (ProjectUtil.startConfig.getBool("showSql")) {
            setting.set(SqlLog.KEY_SHOW_SQL, "true");
            setting.set(SqlLog.KEY_SHOW_PARAMS, "true");
            setting.set(SqlLog.KEY_FORMAT_SQL, "true");
        }
        GlobalDSFactory.set(DSFactory.create(setting));
        hasInit = true;
    }

    public static void initSqlite(String path) {
        if (hasInit) {
            return;
        }
        initSqlPool(StrUtil.format("jdbc:sqlite:{}{}", ProjectUtil.rootPath, path), null, null, null);
    }

    private interface ActionExec<T> {
        T action() throws Exception;
    }

    private static <T> T tryAction(ActionExec<T> exec, String key, int secondOut, Class clazz) {
        try {
            if (StrUtil.isNotBlank(key) && secondOut > 0) {
                return CacheUtil.getCacheData(redisKey(clazz, key), () -> {
                    try {
                        return exec.action();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, secondOut);
            } else {
                T t = exec.action();
                if (secondOut == -1) {
                    DbUtil.removeCache(clazz);
                }
                return t;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String redisKey(Class clazz, String cacheKey) {
        if (!StrUtil.equals(cacheKey, "*")) {
            cacheKey = SecureUtil.md5(cacheKey);
        }
        String redisKey = "sql:" + clazz.getSimpleName().toLowerCase() + ":" + cacheKey;
        return redisKey;
    }

    private static String cacheKey(String sql, Object... params) {
        StringBuilder sb = new StringBuilder(sql);
        if (params != null) {
            for (Object param : params) {
                sb.append(param);
            }
        }
        return sb.toString();
    }

    public static Entity queryOne(String sql, Class clazz, Object... params) {
        return tryAction(() -> DbUtil.get().queryOne(sql, params), cacheKey(sql, params), DEFAULT_CACHE_TIME, clazz);
    }

    public static <T> T queryObject(String sql, Class<T> clazz, Object... params) {
        Entity item = tryAction(() -> DbUtil.get().queryOne(sql, params), cacheKey(sql, params), DEFAULT_CACHE_TIME, clazz);
        if (item != null) {
            return item.toBean(clazz);
        }
        return null;
    }

    public static Long queryLong(String sql, Class clazz, Object... params) {
        Number number = tryAction(() -> DbUtil.get().queryNumber(sql, params), cacheKey(sql, params), DEFAULT_CACHE_TIME, clazz);
        if (number == null) {
            return 0L;
        }
        return number.longValue();
    }

    public static int delete(String sql, Class clazz, Object... params) {
        return tryAction(() -> DbUtil.get().execute(sql, params), null, -1, clazz);
    }

    public static String queryString(String sql, Class clazz, Object... params) {
        return tryAction(() -> DbUtil.get().queryString(sql, params), cacheKey(sql, params), DEFAULT_CACHE_TIME, clazz);
    }

    public static List<Entity> query(String sql, Class clazz, Object... params) {
        return tryAction(() -> DbUtil.get().query(sql, params), cacheKey(sql, params), DEFAULT_CACHE_TIME, clazz);
    }

    public static <T> List<T> queryList(String sql, Class<T> clazz, Object... params) {
        List<Entity> tmpList = query(sql, clazz, params);
        if (tmpList == null) {
            return null;
        }
        List<T> list = new ArrayList<>();
        for (Entity tmp : tmpList) {
            list.add(tmp.toBean(clazz));
        }
        return list;
    }

    public static boolean insert(Entity entity, Class clazz) {
        return tryAction(() -> DbUtil.get().insert(entity) > 0, null, -1, clazz);
    }

    public static boolean insertObject(Object object) {
        Entity entity = Entity.parse(object, true, false);
        return insert(entity, object.getClass());
    }

    public static boolean[] insertColl(Collection<Entity> colls, Class clazz) {
        return tryAction(() -> {
            int[] items = DbUtil.get().insert(colls);
            boolean[] flags = new boolean[items.length];
            for (int i = 0; i < items.length; i++) {
                flags[i] = items[i] > 0;
            }
            return flags;
        }, null, -1, clazz);
    }

    public static boolean[] insertObjectColl(Collection<Object> objects) {
        List<Entity> list = new ArrayList<>();
        Class clazz = null;
        for (Object obj : objects) {
            Entity entity = Entity.parse(obj, true, false);
            list.add(entity);
            if (clazz == null) {
                clazz = obj.getClass();
            }
        }
        if (clazz == null) {
            return new boolean[0];
        }
        return insertColl(list, clazz);
    }

    public static int update(Entity entity, Entity where, Class clazz) {
        return tryAction(() -> DbUtil.get().update(entity, where), null, -1, clazz);
    }

    public static int updateObject(Object obj, Entity where) {
        Entity entity = Entity.parse(obj, true, false);
        return update(entity, where, obj.getClass());
    }

    public static int upsert(Entity entity, Class clazz, String... keys) {
        return tryAction(() -> DbUtil.get().upsert(entity, keys), null, -1, clazz);
    }

    public static int upsertObject(Object obj, String... keys) {
        Entity entity = Entity.parse(obj, true, false);
        return upsert(entity, obj.getClass(), keys);
    }

    /*public static void removeCache(String sql,Class clazz, Object... params) {
        String cacheKey = cacheKey(sql, params);
        String redisKey = redisKey(clazz,cacheKey);
        CacheUtil.delCacheData(redisKey);
    }*/
    public static void removeCache(Class clazz) {
        String redisKey = redisKey(clazz, "*");
        CacheUtil.delCacheData(redisKey);
    }

    /**
     * @param queryData   如 select *
     * @param whereSql    如 from t1 where n = ?
     * @param currentPage
     * @param pageSize
     * @param params
     * @return
     */
    public static CommonBean.PageRes<Entity> page(String queryData, String whereSql, Class clazz, int currentPage, int pageSize, Object... params) {
        List<Object> keyList = CollUtil.newArrayList(params);
        keyList.add(currentPage);
        keyList.add(pageSize);
        int offset = (currentPage - 1) * pageSize;
        String dataSql = queryData + " " + whereSql + " limit " + pageSize + " offset " + offset;
        String countSql = "SELECT count(1) " + whereSql;
        CommonBean.PageRes<Entity> res = tryAction(() -> {
            List<Entity> data = DbUtil.query(dataSql, clazz, params);
            long count = DbUtil.queryLong(countSql, clazz, params);
            CommonBean.PageRes<Entity> tmp = new CommonBean.PageRes<>();
            tmp.setCount(count);
            tmp.setData(data);
            long pages = count / pageSize;
            if (count % pageSize != 0) {
                pages += 1L;
            }
            tmp.setPages(pages);
            return tmp;
        }, cacheKey(dataSql, ArrayUtil.toArray(keyList, Object.class)), DEFAULT_CACHE_TIME, clazz);
        return res;
    }

    public static <T> CommonBean.PageRes<T> pageObject(String queryData, String whereSql, Class<T> clazz, int currentPage, int pageSize, Object... params) {
        CommonBean.PageRes<Entity> pageResult = page(queryData, whereSql, clazz, currentPage, pageSize, params);
        CommonBean.PageRes<T> objRes = new CommonBean.PageRes<>();
        objRes.setPages(pageResult.getPages());
        objRes.setCount(pageResult.getCount());
        objRes.setData(new ArrayList<>());
        for (Entity entity : pageResult.getData()) {
            objRes.getData().add(entity.toBean(clazz));
        }
        return objRes;
    }

    public static R commonEdit(Object data) {
        boolean flag;
        String id = (String) ReflectUtil.getFieldValue(data, "id");
        if (StrUtil.isNotBlank(id)) {
            flag = DbUtil.updateObject(data, Entity.create().set("id", id)) > 0;
        } else {
            ReflectUtil.setFieldValue(data, "id", IdUtil.fastSimpleUUID());
            flag = DbUtil.insertObject(data);
        }
        Assert.isTrue(flag, "操作失败");
        return R.ok();
    }

    public static void execSql(String sql) {
        try {
            DbUtil.get().execute(sql);
        } catch (Exception e) {

        }
    }
}

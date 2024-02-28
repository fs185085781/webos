package cn.tenfell.webos.common.util;

import cn.hutool.cache.Cache;
import cn.hutool.cache.file.LRUFileCache;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.SerializeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.nosql.redis.RedisDS;
import cn.hutool.setting.Setting;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.params.SetParams;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CacheUtil {
    private static RedisDS redisDS;
    //0redis 1file
    private static int type;
    private static FileCache fileCache;
    private static long nextClearTime = 0;

    private static Jedis getJedis() {
        return redisDS.getJedis();
    }

    public static <T> void setValue(String key, T value) {
        setValue(key, value, 0);
    }

    public static <T> void setValue(String key, T value, int secondOut) {
        if (value == null) {
            return;
        }
        if (type == 0) {
            Jedis jedis = getJedis();
            try {
                byte[] bytes = SerializeUtil.serialize(value);
                if (secondOut > 0) {
                    jedis.set(key.getBytes(CharsetUtil.CHARSET_UTF_8), bytes, new SetParams().px(secondOut * 1000L));
                } else {
                    jedis.set(key.getBytes(CharsetUtil.CHARSET_UTF_8), bytes);
                }
            } finally {
                jedis.close();
            }
        } else {
            fileCache.setValue(key, value, secondOut * 1000L);
        }
    }

    public static <T> T getCacheData(String key, Supplier<T> supplier, int secondOut) {
        if (secondOut > 0) {
            Dict data = getValue(key);
            if (data == null) {
                data = Dict.create().set("data", supplier.get());
                setValue(key, data, secondOut);
            }
            return data.getBean("data");
        } else {
            return supplier.get();
        }

    }

    /**
     * 检索key最后的字符串必须为:*
     *
     * @param keyx
     * @return
     */

    public static List<String> scan(String keyx) {
        if (type == 0) {
            List<String> list = new ArrayList<>();
            ScanParams params = new ScanParams();
            params.match(keyx);
            params.count(1000);
            String cursor = "0";
            Jedis jedis = getJedis();
            try {
                while (true) {
                    ScanResult scanResult = jedis.scan(cursor, params);
                    List<String> elements = scanResult.getResult();
                    if (elements != null && elements.size() > 0) {
                        list.addAll(elements);
                    }
                    cursor = scanResult.getCursor();
                    if ("0".equals(cursor)) {
                        break;
                    }
                }
            } finally {
                jedis.close();
            }
            return list;
        } else {
            return fileCache.scan(keyx);
        }

    }

    /**
     * 支持以:*结尾的key
     *
     * @param keyx
     */
    public static void delCacheData(String keyx) {
        if (keyx.endsWith(":*")) {
            List<String> keys = scan(keyx);
            for (String key : keys) {
                delValue(key);
            }
        } else {
            delValue(keyx);
        }
    }

    public static <T> T getValue(String key) {
        if (type == 0) {
            Jedis jedis = getJedis();
            byte[] bytes;
            try {
                bytes = jedis.get(key.getBytes(CharsetUtil.CHARSET_UTF_8));
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
            } finally {
                jedis.close();
            }
            return SerializeUtil.deserialize(bytes);
        } else {
            return (T) fileCache.getValue(key);
        }
    }

    public static void delValue(String key) {
        try {
            if (type == 0) {
                Jedis jedis = getJedis();
                try {
                    jedis.del(key.getBytes(CharsetUtil.CHARSET_UTF_8));
                } finally {
                    jedis.close();
                }
            } else {
                fileCache.remove(key);
            }
        } catch (Exception e) {

        }
    }

    public static void changeRedisCache(String host, Integer port, Integer database, String password) {
        CacheUtil.type = 0;
        Setting redisConfig = new Setting();
        redisConfig.setByGroup("host", "main", StrUtil.isNotBlank(host) ? host : null);
        redisConfig.setByGroup("port", "main", port == null ? null : Convert.toStr(port));
        redisConfig.setByGroup("password", "main", StrUtil.isNotBlank(password) ? password : null);
        redisConfig.setByGroup("database", "main", database == null ? null : Convert.toStr(database));
        redisConfig.setByGroup("timeout", "main", "2000");
        if (redisDS != null) {
            redisDS.close();
        }
        if (fileCache != null) {
            fileCache.clear();
        }
        redisDS = RedisDS.create(redisConfig, "main");
    }

    public static void changeFileCache(String path) {
        CacheUtil.type = 1;
        if (redisDS != null) {
            redisDS.close();
        }
        if (fileCache != null) {
            fileCache.clear();
        }
        fileCache = new FileCache(StrUtil.format("{}{}", ProjectUtil.rootPath, path));
    }

    /**
     * 每隔2小时检查一下本地过期缓存
     */
    public static void clearExpireCache() {
        if (type == 0) {
            return;
        }
        if (nextClearTime > System.currentTimeMillis()) {
            return;
        }
        List<String> keys = fileCache.scan("*");
        for (String key : keys) {
            getValue(key);
        }
        nextClearTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000;
    }

    private static class FileCache {
        private LRUFileCache lruFileCache;
        private String rootPath;

        public FileCache(String rootPath) {
            this.rootPath = rootPath;
            //每隔60秒去文件读
            this.lruFileCache = new LRUFileCache(10 * 1024 * 1024, 5 * 1024 * 1024, 60000);
        }

        public Object getValue(String key) {
            try {
                File file = key2file(key);
                byte[] sz = this.lruFileCache.getFileBytes(file);
                if (sz == null || sz.length == 0) {
                    return null;
                }
                Dict dict = SerializeUtil.deserialize(sz);
                long time = dict.getLong("time");
                Object data = dict.get("data");
                if (time == 0) {
                    return data;
                } else {
                    if (System.currentTimeMillis() > time) {
                        //过期
                        FileUtil.del(file);
                        getCache().remove(file);
                        return null;
                    } else {
                        return data;
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        public void setValue(String key, Object data, long timeout) {
            Dict dict = Dict.create().set("data", data);
            if (timeout > 0) {
                dict.set("time", System.currentTimeMillis() + timeout);
            } else {
                dict.set("time", 0);
            }
            File file = key2file(key);
            byte[] cache = SerializeUtil.serialize(dict);
            //先覆盖文件
            FileUtil.writeBytes(cache, file);
            //后覆盖内存
            getCache().put(file, cache);
        }

        public void remove(String key) {
            File file = key2file(key);
            FileUtil.del(file);
            getCache().remove(file);
        }

        public void clear() {
            FileUtil.del(this.rootPath);
            this.lruFileCache.clear();
        }

        private File key2file(String key) {
            File file = new File(this.rootPath + "/" + StrUtil.replace(key, ":", "/") + "cache.data");
            return file;
        }

        private Cache<File, byte[]> getCache() {
            return (Cache<File, byte[]>) ReflectUtil.getFieldValue(lruFileCache, "cache");
        }

        public List<String> scan(String key) {
            String pathPrefix = StrUtil.replace(this.rootPath, "\\", "/") + "/";
            String path = pathPrefix + StrUtil.replace(key, ":", "/");
            path = path.replace("/*", "");
            List<File> files = FileUtil.loopFiles(path);
            List<String> keys = new ArrayList<>();
            for (File file : files) {
                String tmp = StrUtil.replace(file.getAbsolutePath(), "\\", "/");
                tmp = StrUtil.removeAny(tmp, "cache.data", pathPrefix);
                tmp = StrUtil.replace(tmp, "/", ":");
                keys.add(tmp);
            }
            return keys;
        }
    }
}

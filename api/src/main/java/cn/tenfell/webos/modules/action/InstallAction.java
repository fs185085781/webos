package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.func.VoidFunc1;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.transaction.TransactionLevel;
import cn.hutool.json.JSONObject;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.modules.entity.IoDrive;
import cn.tenfell.webos.modules.entity.IoUserDrive;
import cn.tenfell.webos.modules.entity.SysUser;
import redis.clients.jedis.Jedis;

import javax.sql.DataSource;
import java.util.List;

@BeanAction(val = "install")
public class InstallAction {
    @Action(val = "save", type = 1)
    @Login(val = false)
    public R save(Dict dict) {
        Assert.isFalse(ProjectUtil.hasInstall || FileUtil.exist(ProjectUtil.projectConfigPath),
                "当前系统已经安装，请勿重复安装");
        List<Integer> list = CollUtil.newArrayList(1, 2);
        for (Integer i : list) {
            dict.set("type", i);
            try {
                R r = check(dict);
                if (Convert.toInt(r.get("code")) != 0) {
                    return r;
                }
            } catch (Exception e) {
                return R.failed();
            }
        }
        JSONObject data = dict.getBean("data");
        //先写入配置
        FileUtil.writeUtf8String(data.toString(), ProjectUtil.projectConfigPath);
        //初始化配置
        R f = ProjectUtil.install();
        if (Convert.toInt(f.get("code"),-1) != 0) {
            return f;
        }
        //写入用户数据
        JSONObject user = dict.getBean("user");
        try {
            DbUtil.get().tx(TransactionLevel.READ_COMMITTED, (VoidFunc1<Db>) db -> {
                DbUtil.set(db);
                SysUser userDb = UserAction.createMain(user.getStr("username"), user.getStr("password"), 1);
                IoDrive param = new IoDrive();
                param.setName("默认存储");
                param.setDriveType(FileSystemUtil.LOCAL_DRIVE_TYPE);
                param.setMaxSize(0);
                param.setUserDriveName("默认存储");
                String path = ProjectUtil.rootPath + "/" + IdUtil.fastSimpleUUID();
                param.setPath(path);
                param.setSecondTransmission(1);
                param.setRealFilePath(ProjectUtil.rootPath);
                IoDrive ioDb = IoDriveAction.saveIoDrive(param, userDb.getId());
                IoUserDrive param2 = new IoUserDrive();
                param2.setUserId(userDb.getId());
                param2.setDriveId(ioDb.getId());
                param2.setMaxSize(0);
                param2.setName("默认存储");
                param2.setValid(1);
                IoUserDriveAction.saveIoUserDrive(param2, ioDb, userDb);
            });
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
            ProjectUtil.uninstall();
            return R.error(e);
        }
        return R.ok();
    }

    @Action(val = "check", type = 1)
    @Login(val = false)
    public R check(Dict dict) throws Exception {
        Assert.isFalse(ProjectUtil.hasInstall || FileUtil.exist(ProjectUtil.projectConfigPath),
                "当前系统已经安装，请勿重复安装");
        JSONObject data = dict.getBean("data");
        int type = dict.getInt("type");
        if (type == 1) {
            //检查数据库
            DataSource dataSource = null;
            String sqlType = data.getStr("sqlType");
            if (StrUtil.equals(sqlType, "sqlite")) {
                JSONObject sqlite = data.getJSONObject("sqlite");
                String path = ProjectUtil.rootPath + sqlite.getStr("path");
                FileUtil.mkParentDirs(path);
                dataSource = new SimpleDataSource(StrUtil.format("jdbc:sqlite:{}", path), null, null, null);
            } else if (StrUtil.equals(sqlType, "mysql")) {
                JSONObject mysql = data.getJSONObject("mysql");
                String host = mysql.getStr("host");
                Integer port = mysql.getInt("port");
                String database = mysql.getStr("database");
                String user = mysql.getStr("user");
                String password = mysql.getStr("password");
                dataSource = new SimpleDataSource(StrUtil.format("jdbc:mysql://{}:{}/{}?useSSL=false&serverTimezone=GMT%2B8", host, port, database), user, password, "com.mysql.jdbc.Driver");
            }
            if (dataSource == null) {
                return R.failed();
            }
            if (!dataSource.getConnection().isClosed()) {
                return R.ok();
            }
        } else if (type == 2) {
            //检查缓存
            String cacheType = data.getStr("cacheType");
            if (StrUtil.equals(cacheType, "file")) {
                JSONObject file = data.getJSONObject("file");
                String dir = StrUtil.format("{}{}", ProjectUtil.rootPath, file.getStr("path"));
                FileUtil.mkdir(dir);
                if (FileUtil.exist(dir)) {
                    return R.ok();
                } else {
                    return R.failed("当前目录不存在");
                }
            } else if (StrUtil.equals(cacheType, "redis")) {
                JSONObject redis = data.getJSONObject("redis");
                String host = redis.getStr("host");
                Integer port = redis.getInt("port");
                Integer database = redis.getInt("database");
                String password = redis.getStr("password");
                Jedis jedis = new Jedis(host, port);
                jedis.auth(password);
                jedis.select(database);
                if (jedis.isConnected()) {
                    return R.ok();
                }
            }
        } else if (type == 4) {
            //检查接口
            return R.okData(Dict.create().set("rootPath", ProjectUtil.rootPath));
        } else {
            return R.failed();
        }
        return R.failed();
    }
}

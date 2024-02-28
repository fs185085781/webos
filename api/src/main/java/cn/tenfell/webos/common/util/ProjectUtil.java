package cn.tenfell.webos.common.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.noear.solon.Solon;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.message.Listener;
import org.noear.solon.core.message.Message;
import org.noear.solon.core.message.Session;
import org.noear.solon.web.staticfiles.StaticMappings;
import org.noear.solon.web.staticfiles.StaticMimes;
import org.noear.solon.web.staticfiles.repository.FileStaticRepository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.tenfell.webos.WebOsApp;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.filesystem.LocalFileSystem;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.ssh.WebSSHService;
import cn.tenfell.webos.common.ssh.WebSSHServiceImpl;
import cn.tenfell.webos.common.webdav.WebdavUtil;

public class ProjectUtil {
    private static Log log = LogFactory.get();
    public static String jmPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCyCKDubntmD36NAS0EqUY6re+GVCVZr2y/poNdC0AG9sTYURtt6qmfF2SBgJydceyYKV8AZ7zV3QKpa2ieqmTBLRsB4JIEQovzcQyNVWY90XPd10LG6f9WshWdCArwBp2VchKhL86fm5pypvqtIih+kFGfOoocQ+NAafOwVrD3JwIDAQAB";
    public static JSONObject softVersion;
    private static final ThreadLocal<ProjectContext> currentContext = new ThreadLocal<>();
    // 配置文件路径
    public static String projectConfigPath;
    public static JSONObject startConfig;
    public static String rootPath;
    // 系统是否安装
    public static boolean hasInstall = false;
    public static JSONConfig jsonConfig;
    public static String webPath;

    public static ProjectContext getContext() {
        return currentContext.get();
    }

    public static void showConsoleErr(Throwable e) {
        authRestart(e);
        if (!startConfig.getBool("debug")) {
            return;
        }
        log.error(e);
    }

    private static void settingKaijiAction() {
        boolean isWin = System.getProperty("os.name").toLowerCase().indexOf("window") != -1;
        if (!isWin) {
            // linux
            String jb = "cd " + ProjectUtil.rootPath + " && sh restart.sh";
            String rc = "/etc/rc.d/rc.local";
            String cmd = "grep -q \"" + jb + "\" " + rc + " || echo \"" + jb + "\" >> " + rc;
            FileUtil.writeUtf8String(cmd, ProjectUtil.rootPath + "/linuxauto.sh");
            RuntimeUtil.exec(null, new File(ProjectUtil.rootPath), "sh linuxauto.sh");
            FileUtil.del(ProjectUtil.rootPath + "/linuxauto.sh");
            String autoPath = "/etc/profile.d/" + ProjectUtil.startConfig.getStr("projectName") + ".sh";
            if (FileUtil.exist(autoPath)) {
                FileUtil.del(autoPath);
            }
        } else {
            // win系统
            StringBuilder sb = new StringBuilder();
            sb.append("title tenfell-webos\n");
            String jarPath = ProjectUtil.rootPath + "/webos.jar";
            sb.append("java -javaagent:" + jarPath + " -jar " + jarPath + "\n");
            FileUtil.writeUtf8String(sb.toString(), ProjectUtil.rootPath + "/winauto.bat");
        }
    }

    public static void authRestart(Throwable e) {
        if(e != null){
            e = ExceptionUtil.getRootCause(e);
            if (!"InaccessibleObjectException".equals(e.getClass().getSimpleName())) {
                return;
            }
        }
        int version = Convert.toInt(System.getProperty("java.version").split("_")[0].split("\\.")[0], 1);
        if (version < 9) {
            return;
        }
        String oldStr = "-Dfile.encoding=UTF-8 -jar";
        String[] psz = { "java.lang", "java.util", "java.nio", "sun.nio.ch", "jdk.internal.loader", "java.net",
                "sun.net.www.protocol.https" };
        StringBuilder newStr = new StringBuilder("-Dfile.encoding=UTF-8 ");
        for (String p : psz) {
            newStr.append("--add-opens java.base/").append(p).append("=ALL-UNNAMED ");
        }
        newStr.append("-jar");
        List<Dict> paths = CollUtil.newArrayList(
                Dict.create().set("path", "restart.sh").set("charset", CharsetUtil.UTF_8),
                Dict.create().set("path", "restart.bat").set("charset", CharsetUtil.UTF_8),
                Dict.create().set("path", "start.bat").set("charset", CharsetUtil.UTF_8));
        boolean has = false;
        for (Dict item : paths) {
            if (!FileUtil.exist(ProjectUtil.rootPath + "/" + item.getStr("path"))) {
                continue;
            }
            String str = FileUtil.readString(ProjectUtil.rootPath + "/" + item.getStr("path"),
                    CharsetUtil.charset(item.getStr("charset")));
            if (str.indexOf(oldStr) == -1) {
                continue;
            }
            has = true;
            str = str.replace(oldStr, newStr);
            FileUtil.writeString(str, ProjectUtil.rootPath + "/" + item.getStr("path"), item.getStr("charset"));
        }
        if (!has) {
            return;
        }
        restartServer();
    }

    public static void restartServer() {
        new Thread(() -> {
            boolean isWin = System.getProperty("os.name").toLowerCase().indexOf("window") != -1;
            if (isWin) {
                // win
                RuntimeUtil.exec(null, new File(ProjectUtil.rootPath), "restart.bat");
            } else {
                // linux系统或mac系统
                RuntimeUtil.exec(null, new File(ProjectUtil.rootPath), "sh restart.sh");
            }
        }).start();
    }

    public static void init() {
        String configPath = StrUtil.replace(WebOsApp.class.getResource("").getPath(), "\\", "/");
        rootPath = StrUtil.replace(FileUtil.getParent(configPath, 4), "\\", "/");
        FileUtil.writeUtf8String(Convert.toStr(RuntimeUtil.getPid()), rootPath + "/pidfile.txt");
        authRestart(null);
        projectConfigPath = rootPath + "/project_config.json";
        hasInstall = FileUtil.exist(projectConfigPath);
        String startConfigPath = rootPath + "/start_config.json";
        if (FileUtil.exist(startConfigPath)) {
            startConfig = JSONUtil.parseObj(FileUtil.readUtf8String(startConfigPath));
        } else {
            startConfig = JSONUtil.createObj();
        }
        boolean needSave = false;
        if (startConfig.getBool("debug") == null) {
            startConfig.set("debug", false);
            needSave = true;
        }
        if (startConfig.getStr("defaultPwd") == null) {
            startConfig.set("defaultPwd", "123456");
            needSave = true;
        }
        if (startConfig.getBool("showSql") == null) {
            startConfig.set("showSql", false);
            needSave = true;
        }
        if (startConfig.getBool("rangeFilter") == null) {
            startConfig.set("rangeFilter", true);
            needSave = true;
        }
        if (startConfig.getInt("port", 0) == 0) {
            startConfig.set("port", 8088);
            needSave = true;
        }
        if (StrUtil.isBlank(startConfig.getStr("zlPublic")) || StrUtil.isBlank(startConfig.getStr("zlPrivate"))) {
            RSA rsa = SecureUtil.rsa();
            startConfig.set("zlPrivate", rsa.getPrivateKeyBase64());
            startConfig.set("zlPublic", rsa.getPublicKeyBase64());
            needSave = true;
        }
        if (StrUtil.isBlank(startConfig.getStr("projectName"))) {
            startConfig.set("projectName", "webos" + IdUtil.getSnowflakeNextId());
            needSave = true;
        }
        if (needSave) {
            FileUtil.writeUtf8String(startConfig.toString(), startConfigPath);
        }
        noErrorMethod(ProjectUtil::settingKaijiAction);
        noErrorMethod(FrameStartUtil::pringInfo);
        log.info("webos started in jdk:{}", System.getProperty("java.version"));
        jsonConfig = new JSONConfig();
        jsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
        AtomicReference<String> webRootPath = new AtomicReference<>(startConfig.getStr("webRootPath"));
        if (StrUtil.isNotBlank(webRootPath.get())) {
            // 如果项目移动了,尝试自动修复
            String checkfile = webRootPath.get() + "/common/sdk/check.json";
            if (!FileUtil.exist(checkfile)) {
                // 不存在的时候
                String tmpPath = CommonUtil.getParentPath(ProjectUtil.rootPath);
                tmpPath = tmpPath + "/web";
                if (FileUtil.exist(tmpPath + "/common/sdk/check.json")) {
                    startConfig.set("webRootPath", tmpPath);
                    needSave = true;
                    webRootPath.set(tmpPath);
                }
            }
        }
        if (StrUtil.isNotBlank(webRootPath.get())) {
            FileUtil.writeBytes(FrameStartUtil.logo, webRootPath.get() + "/imgs/logo.png");
            FileUtil.writeBytes(FrameStartUtil.logo, webRootPath.get() + "/common/smart-ui/expand/logo_32.png");
        }
        int port = startConfig.getInt("port");
        Solon.start(ProjectUtil.class, ArrayUtil.append(new String[0], "--server.port=" + port), app -> {
            app.filter((ctx, chain) -> {
                try {
                    if (ctx.uri().getPath().startsWith("/webdav")) {
                        ctx.attrSet("page-id", "webdav");
                        currentContext.set(ProjectContext.init(ctx));
                        WebdavUtil.servlet(ctx);
                        ctx.setHandled(true);
                    } else {
                        MethodType.valueOf(ctx.method());
                        chain.doFilter(ctx);
                    }
                } catch (Exception e) {
                    System.out.println("");
                }
            });
            app.http("/api", ctx -> {
                try {
                    String module = ctx.param("module");
                    String action = ctx.param("action");
                    if (ctx.method().equals("OPTIONS")) {
                        return;
                    }
                    currentContext.set(ProjectContext.init(ctx));
                    Object resData = BeanProxy.invoke(module, action, ctx);
                    if (resData == null) {

                    } else if (resData instanceof InputStream) {
                        InputStream in = (InputStream) resData;
                        try {
                            IoUtil.copy(in, ctx.outputStream());
                        } catch (Exception e) {

                        } finally {
                            IoUtil.close(in);
                        }
                    } else if (resData.getClass().isArray()
                            && ClassUtil.isAssignable(Byte.class, resData.getClass().getComponentType())) {
                        ctx.output((byte[]) resData);
                    } else {
                        ctx.output(JSONUtil.toJsonStr(resData, jsonConfig));
                    }
                } catch (Exception e) {
                    showConsoleErr(e);
                }
            });
            app.http("/init", ctx -> {
                String path = ctx.param("path");
                if (StrUtil.isNotBlank(path)) {
                    // 设置
                    R r;
                    JSONObject tmpConfig;
                    if (FileUtil.exist(startConfigPath)) {
                        tmpConfig = JSONUtil.parseObj(FileUtil.readUtf8String(startConfigPath));
                    } else {
                        tmpConfig = JSONUtil.createObj();
                    }
                    if (StrUtil.isNotBlank(tmpConfig.getStr("webRootPath"))) {
                        r = R.failed("已经设置,请删除start_config.json后重试");
                    } else {
                        path = StrUtil.replace(path, "\\", "/");
                        if (path.endsWith("/")) {
                            path = path.substring(0, path.length() - 1);
                        }
                        String checkfile = path + "/common/sdk/check.json";
                        if (FileUtil.exist(checkfile)) {
                            startConfig.set("webRootPath", path);
                            webRootPath.set(path);
                            FileUtil.writeUtf8String(startConfig.toString(), startConfigPath);
                            StaticMappings.add("/", false, new FileStaticRepository(webRootPath.get()));
                            r = R.ok();
                        } else {
                            r = R.failed("此目录并非前端程序目录,请检查后重试");
                        }
                    }
                    ctx.output(JSONUtil.toJsonStr(r, jsonConfig));
                } else {
                    String html = IoUtil.readUtf8(WebOsApp.class.getResource("/install/index.html").openStream());
                    String webPath = CommonUtil.getParentPath(ProjectUtil.rootPath) + "/web";
                    html = html.replace("{{path}}", webPath);
                    ctx.outputAsHtml(html);
                }
            });
            app.enableWebSocket(true);
            final WebSSHService sshService = new WebSSHServiceImpl();
            app.ws("/websocket", new Listener() {
                @Override
                public void onOpen(Session session) {
                    sshService.initConnection(session);
                }

                @Override
                public void onMessage(Session session, Message message) {
                    message.setHandled(true);
                    sshService.recvHandle(message.bodyAsString(), session);
                }

                @Override
                public void onClose(Session session) {
                    sshService.close(session);
                }
            });
            app.get("/", ctx -> {
                if (StrUtil.isNotBlank(webRootPath.get())) {
                    ctx.redirect("/index.html");
                } else {
                    ctx.redirect("/init");
                }
            });
            if (StrUtil.isNotBlank(webRootPath.get())) {
                StaticMappings.add("/", false, new FileStaticRepository(webRootPath.get()));
            }
            StaticMimes.add(".webp", "image/webp");
        });
        log.info("webos started on 【0.0.0.0:{}】", port);
        Set<Class<?>> beans = ClassUtil.scanPackageByAnnotation("cn.tenfell.webos", BeanAction.class);
        BeanProxy.init(beans);
        initOsConfig();
    }

    public static R install() {
        try {
            hasInstall = FileUtil.exist(projectConfigPath);
            initOsConfig();
            // 写入sql文件
            JSONObject config = JSONUtil.parseObj(FileUtil.readString(projectConfigPath, CharsetUtil.CHARSET_UTF_8));
            InputStream in = null;
            if (StrUtil.equals(config.getStr("sqlType"), "mysql")) {
                in = WebOsApp.class.getResource("/config/mysql.sql").openStream();
            } else if (StrUtil.equals(config.getStr("sqlType"), "sqlite")) {
                in = WebOsApp.class.getResource("/config/sqlite.sql").openStream();
            }
            Assert.notNull(in, "文件流不存在");
            String content = IoUtil.readUtf8(in);
            List<String> list = SqlUtil.splitSqlScript(content);
            int[] data = DbUtil.get().executeBatch(list);
            int count = 0;
            for (int i : data) {
                count += i;
            }
            Assert.isTrue(count > 0, "数据库安装失败");
            return R.ok();
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
            uninstall();
            return R.error(e);
        }
    }

    public static void uninstall() {
        FileUtil.del(projectConfigPath);
        hasInstall = false;
    }

    public static void initOsConfig() {
        if (!hasInstall) {
            return;
        }
        String webRootPath = startConfig.getStr("webRootPath");
        Assert.notBlank(webRootPath, "请先经过init初始化前端位置");
        // 初始化配置文件
        JSONObject config = JSONUtil.parseObj(FileUtil.readString(projectConfigPath, CharsetUtil.CHARSET_UTF_8));
        webPath = StrUtil.replace(webRootPath, "\\", "/");
        if (webPath.endsWith("/")) {
            webPath = webPath.substring(0, webPath.length() - 1);
        }
        // 初始化数据库
        if (StrUtil.equals(config.getStr("sqlType"), "mysql")) {
            JSONObject mysql = config.getJSONObject("mysql");
            DbUtil.initMysql(mysql.getStr("host"), mysql.getInt("port"), mysql.getStr("database"), mysql.getStr("user"),
                    mysql.getStr("password"));
        } else if (StrUtil.equals(config.getStr("sqlType"), "sqlite")) {
            JSONObject sqlite = config.getJSONObject("sqlite");
            DbUtil.initSqlite(sqlite.getStr("path"));
        } else {
            Assert.isTrue(false, "数据库配置失败");
        }
        log.info("db is ok,current db is 【{}】", config.getStr("sqlType"));
        // 初始化缓存
        if (StrUtil.equals(config.getStr("cacheType"), "redis")) {
            JSONObject redis = config.getJSONObject("redis");
            CacheUtil.changeRedisCache(redis.getStr("host"), redis.getInt("port"), redis.getInt("database"),
                    redis.getStr("password"));
        } else if (StrUtil.equals(config.getStr("cacheType"), "file")) {
            JSONObject file = config.getJSONObject("file");
            CacheUtil.changeFileCache(file.getStr("path"));
        } else {
            Assert.isTrue(false, "缓存配置失败");
        }
        log.info("cache is ok,current cache is 【{}】", config.getStr("cacheType"));
        try {
            InputStream in = WebOsApp.class.getResource("/version.json").openStream();
            softVersion = JSONUtil.parseObj(IoUtil.readUtf8(in));
        } catch (Exception e) {

        }
        String path = ProjectUtil.rootPath + "/updateAction.json";
        JSONObject map;
        if (FileUtil.exist(path)) {
            map = JSONUtil.parseObj(FileUtil.readUtf8String(path));
        } else {
            map = JSONUtil.createObj();
        }
        if (softVersion != null) {
            String version_num = softVersion.getStr("version_num");
            if (!map.getBool(version_num, false)) {
                String old_version_num = softVersion.getStr("old_version_num");
                if (map.getBool(old_version_num, false)) {
                    try {
                        updateAction();
                        map.set(version_num, true);
                    } catch (Exception e) {
                        map.set(version_num, false);
                    }
                } else {
                    map.set(version_num, true);
                }
            }
        }
        FileUtil.writeUtf8String(map.toString(), path);
        FiberUtil.run(() -> {
            while (true) {
                scheduledTask();
                ThreadUtil.sleep(300000L);
            }
        });
    }

    public static void log(String format, Object... arguments) {
        log.info(format, arguments);
    }

    private interface Func {
        void action();
    }

    private static void noErrorMethod(Func func) {
        try {
            func.action();
        } catch (Exception e) {
            showConsoleErr(e);
        }
    }

    /**
     * 定时任务入口
     * 所有定时任务均从此处进
     * 每隔5分钟执行一次
     */
    public static void scheduledTask() {
        // 刷新token
        noErrorMethod(TokenDataUtil::refreshToken);
        // 清除本地缓存
        noErrorMethod(CacheUtil::clearExpireCache);
        // 清除上传缓存
        noErrorMethod(LocalFileSystem::clearExpireTmpUploadFile);
        // 清除缩略缓存
        noErrorMethod(CommonUtil::clearExpireTmpFile);
    }

    /**
     * 更新必备的方法
     */
    private static void updateAction() {
    }
}
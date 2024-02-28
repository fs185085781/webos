package cn.tenfell.webos.modules.action;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.net.multipart.UploadFile;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.annt.Transactional;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.util.ProjectContext;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.modules.entity.IoFileAss;
import cn.tenfell.webos.modules.entity.SoftUser;
import cn.tenfell.webos.modules.entity.SysUser;
import org.noear.solon.core.handle.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@BeanAction(val = "softUser")
public class SoftUserAction {

    /**
     * 已安装列表
     *
     * @return
     */
    @Action(val = "hasList", type = 1)
    public R hasList(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        List<SoftUser> list = DbUtil.queryList("select * from soft_user where user_id = ?", SoftUser.class, user.getId());
        return R.okData(list);
    }

    /**
     * 添加轻应用
     *
     * @return
     */
    @Action(val = "addIframe", type = 1)
    public R addIframe(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        SoftUser su = new SoftUser();
        su.setImgPath(param.getStr("icon"));
        su.setName(param.getStr("name"));
        su.setCode(SecureUtil.md5(param.getStr("url")));
        su.setDescr(param.getStr("name"));
        su.setScreenShots("[]");
        su.setVersion("1.0.0");
        su.setAuthor("手动添加");
        su.setEffect(param.getStr("name"));
        su.setType(1);
        su.setIframeUrl(param.getStr("url"));
        su.setUserId(user.getId());
        su.setIsStore(0);
        R r = DbUtil.commonEdit(su);
        return r;
    }

    /**
     * 添加软件
     *
     * @return
     */
    @Action(val = "addSoft", type = 0)
    @Transactional
    public R addSoft(Context ctx) {
        String dir = ProjectUtil.webPath + "/apps/" + IdUtil.fastSimpleUUID();
        try {
            String zipPath = dir + "/1.zip";
            UploadFile uFile = ProjectContext.getMultipart(ctx).getFile("file");
            FileUtil.writeFromStream(uFile.getFileInputStream(), zipPath);
            ZipUtil.unzip(zipPath, dir);
            FileUtil.del(zipPath);
            String indexJsonPath = dir + "/index.json";
            Assert.isTrue(FileUtil.exist(indexJsonPath), "程序缺少index.json文件");
            JSONObject appRes = JSONUtil.parseObj(FileUtil.readString(indexJsonPath, CharsetUtil.CHARSET_UTF_8));
            String code = appRes.getStr("code");
            Assert.notBlank(code, "程序编码不存在");
            Assert.isFalse(FileUtil.exist(ProjectUtil.webPath + "/apps/" + code), "此程序已存在");
            SoftUser su = appRes.toBean(SoftUser.class);
            su.setType(0);
            su.setUserId(LoginAuthUtil.getUser().getId());
            su.setIsStore(0);
            R r = DbUtil.commonEdit(su);
            createFileAss(appRes, su.getId(), su.getUserId(), appRes.getStr("code"));
            FileUtil.rename(new File(dir), code, true);
            return r;
        } catch (Exception e) {
            FileUtil.del(dir);
            return R.error(e);
        }
    }

    /**
     * 本地软件库(包含从商城安装的)
     */
    @Action(val = "list", type = 1)
    public R list(Dict param) {
        List<CommonBean.SoftStore> localAppList = new ArrayList<>();
        String appPath = ProjectUtil.webPath + "/apps";
        File[] codeFiles = new File(appPath).listFiles();
        if (codeFiles == null || codeFiles.length == 0) {
            return R.okData(localAppList);
        }
        for (File codeFile : codeFiles) {
            if (!codeFile.isDirectory()) {
                continue;
            }
            String jsonPath = appPath + "/" + codeFile.getName() + "/index.json";
            if (!FileUtil.exist(jsonPath)) {
                continue;
            }
            JSONObject appRes = JSONUtil.parseObj(FileUtil.readString(jsonPath, CharsetUtil.CHARSET_UTF_8));
            String effect = appRes.getStr("effect");
            if (StrUtil.isNotBlank(effect)) {
                String tag = "script";
                appRes.set("effect", effect.replaceAll("(?i)<" + tag, "<no" + tag).replaceAll("(?i)</" + tag + ">", "</no" + tag + ">"));
            }
            if (!StrUtil.equals(appRes.getStr("code"), codeFile.getName())) {
                continue;
            }
            appRes.set("type", 0);
            localAppList.add(appRes.toBean(CommonBean.SoftStore.class));
        }
        return R.okData(localAppList);
    }

    /**
     * 先卸载软件
     *
     * @param data
     * @return
     */
    @Action(val = "uninstall", type = 1)
    @Transactional
    public R uninstall(SoftUser data) {
        SysUser user = LoginAuthUtil.getUser();
        Assert.notBlank(data.getCode(), "参数不足");
        if (StrUtil.isBlank(data.getId())) {
            SoftUser db = DbUtil.queryObject("select * from soft_user where code = ? and user_id = ?", SoftUser.class, data.getCode(), user.getId());
            if (db == null) {
                return R.ok();
            }
            data.setId(db.getId());
        }
        DbUtil.delete("delete from io_file_ass where user_id = ? and soft_user_id = ?", IoFileAss.class, user.getId(), data.getId());
        DbUtil.delete("delete from soft_user where id = ? and user_id = ?", SoftUser.class, data.getId(), user.getId());
        return R.ok();
    }

    /**
     * 安装软件
     *
     * @param param
     * @return
     */
    @Action(val = "install", type = 1)
    @Transactional
    public R install(CommonBean.SoftStore param) {
        Assert.notBlank(param.getCode(), "编码不可为空,软件安装失败");
        if (param.getIsLocal() == null || param.getIsLocal() != 1) {
            //安装远程软件
            if (param.getType() == 0) {
                //插件
                String dir = ProjectUtil.webPath + "/apps/" + param.getCode();
                String zipPath = dir + "/1.zip";
                HttpUtil.downloadFile(param.getDownloadUrl(), zipPath);
                ZipUtil.unzip(zipPath, dir);
                FileUtil.del(zipPath);
            }
        }
        SysUser user = LoginAuthUtil.getUser();
        SoftUser db = DbUtil.queryObject("select * from soft_user where code = ? and user_id = ? ", SoftUser.class, param.getCode(), user.getId());
        if (db != null) {
            uninstall(db);
        }
        SoftUser su = new SoftUser();
        su.setImgPath(param.getImgPath());
        su.setName(param.getName());
        su.setCode(param.getCode());
        su.setDescr(param.getDescr());
        su.setScreenShots(param.getScreenShots());
        su.setVersion(param.getVersion());
        su.setAuthor(param.getAuthor());
        su.setEffect(param.getEffect());
        su.setType(param.getType());
        su.setIframeUrl(param.getIframeUrl());
        su.setUserId(user.getId());
        su.setDownloadUrl(param.getDownloadUrl());
        if (param.getIsLocal() == null || param.getIsLocal() != 1) {
            //远程
            su.setStoreId(param.getId());
            su.setIsStore(1);
        } else {
            //本地
            su.setIsStore(0);
        }
        R r = DbUtil.commonEdit(su);
        if (param.getType() == 0) {
            //插件
            String dir = ProjectUtil.webPath + "/apps/" + param.getCode();
            String indexJsonPath = dir + "/index.json";
            Assert.isTrue(FileUtil.exist(indexJsonPath), "插件缺少index.json文件");
            JSONObject appRes = JSONUtil.parseObj(FileUtil.readString(indexJsonPath, CharsetUtil.CHARSET_UTF_8));
            createFileAss(appRes, su.getId(), user.getId(), param.getCode());
        }
        return r;
    }

    private void createFileAss(JSONObject appRes, String suId, String userId, String code) {
        createFileAssByType(appRes, suId, userId, code, "rightMenuAddAll", "addall");
        createFileAssByType(appRes, suId, userId, code, "rightMenuOpenWith", "openwith");
        createFileAssByType(appRes, suId, userId, code, "rightMenuNew", "new");
    }

    private void createFileAssByType(JSONObject appRes, String suId, String userId, String code,
                                     String menuType, String dbType) {
        JSONArray rightMenuNew = appRes.getJSONArray(menuType);
        if (rightMenuNew != null && rightMenuNew.size() > 0) {
            for (int i = 0; i < rightMenuNew.size(); i++) {
                JSONObject newData = rightMenuNew.getJSONObject(i);
                String ext = newData.getStr("ext");
                if (StrUtil.isBlank(ext)) {
                    continue;
                }
                IoFileAss ifa = new IoFileAss();
                ifa.setId(IdUtil.fastSimpleUUID());
                ifa.setSoftUserId(suId);
                ifa.setUserId(userId);
                ifa.setExt(ext);
                ifa.setIconUrl(newData.getStr("icon"));
                ifa.setAction(dbType);
                ifa.setActionName(newData.getStr("name"));
                ifa.setExpAction(newData.getStr("expAction"));
                ifa.setSortNum(0);
                ifa.setUrl("apps/" + code + "/index.html");
                ifa.setAppName(appRes.getStr("name"));
                Assert.isTrue(DbUtil.insertObject(ifa), "插入文件关联成功");
            }
        }
    }

    /**
     * 升级检查
     *
     * @param param
     * @return
     */
    @Action(val = "checkUpdate", type = 1)
    public R checkUpdate(Dict param) {
        String code = param.getStr("code");
        String version = param.getStr("version");
        if (StrUtil.isBlank(code) || StrUtil.isBlank(version)) {
            return R.failed("无需升级");
        }
        String appPath = ProjectUtil.webPath + "/apps";
        String jsonPath = appPath + "/" + code + "/index.json";
        if (!FileUtil.exist(jsonPath)) {
            return R.failed("无需升级");
        }
        JSONObject res = JSONUtil.parseObj(FileUtil.readUtf8String(jsonPath));
        if (!StrUtil.equals(res.getStr("version"), version)) {
            return R.ok();
        }
        return R.failed("无需升级");
    }

    @Action(val = "update", type = 1)
    @Transactional
    public R update(CommonBean.SoftStore param) {
        Assert.notBlank(param.getCode(), "编码不可为空,软件安装失败");
        if (param.getIsLocal() == null || param.getIsLocal() == 1) {
            return R.failed("非远程软件暂停升级");
        }
        return install(param);
    }

    @Action(val = "hasInstall", type = 1)
    @Login(val = false)
    public R hasInstall(Dict param) {
        Assert.notBlank(param.getStr("code"), "编码不可为空");
        String appPath = ProjectUtil.webPath + "/apps";
        String jsonPath = appPath + "/" + param.getStr("code") + "/index.json";
        if (FileUtil.exist(jsonPath)) {
            return R.ok();
        }
        return R.failed("暂未安装");
    }
}

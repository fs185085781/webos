package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.annt.Transactional;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.modules.entity.SoftUserData;
import cn.tenfell.webos.modules.entity.SoftUserOffice;
import cn.tenfell.webos.modules.entity.SysUser;
import lombok.Data;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@BeanAction(val = "office")
public class OfficeAction {
    private static String CACHE_BIND_SQL = "select * from soft_user_office where id = ?";
    private static String CACHE_COOKIE_APPCODE = "tenfellOfficeCookie";
    private static String CACHE_COOKIE_SQL = "select * from soft_user_data where user_id = ? and app_code = '" + CACHE_COOKIE_APPCODE + "'";

    @Action(val = "save", type = 1)
    @Login(val = false)
    public R save(Dict param) {
        SoftUserOfficeData suod = getSoftUserOfficeByPath(param);
        SoftUserOffice suo = suod.getSoftUserOffice();
        if (suo == null) {
            Assert.isTrue(false, "该绑定不存在或已过期,保存失败");
        }
        SysUser user = DbUtil.queryObject(LoginAuthUtil.userCacheSql, SysUser.class, suo.getUserId());
        Assert.notNull(user, "文件用户异常");
        SoftUserData sud = DbUtil.queryObject(CACHE_COOKIE_SQL, SoftUserData.class, suo.getUserId());
        Assert.notNull(sud, "当前用户cookie已过期,请重新扫码");
        String cookie = sud.getData();
        Assert.notBlank(cookie, "当前用户cookie已过期,请重新扫码");
        String resStr = HttpUtil.createGet("https://drive.kdocs.cn/api/v5/groups/" + suo.getGroupId() + "/files/" + suo.getJinShanId() + "/download?isblocks=false&support_checksums=md5,sha1,sha224,sha256,sha384,sha512").cookie(cookie).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        String url = res.getStr("url");
        Assert.notBlank(url, res.getStr("msg"));
        InputStream in = getInputStream(url);
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByUser(suo.getParentPath(), "", "", user);
        String newFile = FileSystemUtil.ACTION.uploadByServer(plainPath, suo.getName(), in, null, null);
        Assert.notBlank(newFile, "文件上传失败");
        return R.ok();
    }

    private InputStream getInputStream(String url) {
        HttpRequest req = HttpUtil.createGet(url);
        req.header("Referer", "https://www.kdocs.cn/", true);
        return req.executeAsync().bodyStream();
    }

    private SoftUserOfficeData getSoftUserOfficeByPath(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        String path = param.getStr("path");
        boolean hasFileAuth = false;
        FileSystemInface.PlainPath plainPath = null;
        if (user != null) {
            try {
                plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
                hasFileAuth = true;
            } catch (Exception e) {
            }
        }
        if (plainPath == null) {
            try {
                String shareCode = param.getStr("shareCode");
                String sharePwd = param.getStr("sharePwd");
                plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, shareCode, sharePwd);
            } catch (Exception e) {

            }
        }
        if (plainPath == null) {
            Assert.isTrue(false, "权限不足");
        }
        CommonBean.PathInfo fileInfo = FileSystemUtil.ACTION.fileInfo(plainPath);
        String name = fileInfo.getName();
        //返回 1.文件链接  2.分享链接 3.扫码+阿里云 4.阿里云
        String id = SecureUtil.md5(FileUtil.getParent("/" + plainPath.getRealPath(), 1) + "/" + name);
        SoftUserOffice suo = DbUtil.queryObject(CACHE_BIND_SQL, SoftUserOffice.class, id);
        return SoftUserOfficeData.init(suo, hasFileAuth, id, path, plainPath, fileInfo);
    }

    @Data
    private static class SoftUserOfficeData {
        private SoftUserOffice softUserOffice;
        private Boolean hasFileAuth;
        private String suoId;
        private String path;
        private FileSystemInface.PlainPath plainPath;
        private CommonBean.PathInfo fileInfo;

        public static SoftUserOfficeData init(SoftUserOffice suo, boolean hasFileAuth, String suoId, String path, FileSystemInface.PlainPath plainPath, CommonBean.PathInfo fileInfo) {
            SoftUserOfficeData suod = new SoftUserOfficeData();
            suod.setSoftUserOffice(suo);
            suod.setHasFileAuth(hasFileAuth);
            suod.setSuoId(suoId);
            suod.setPath(path);
            suod.setPlainPath(plainPath);
            suod.setFileInfo(fileInfo);
            return suod;
        }
    }

    /**
     * 获取访问链接
     *
     * @param param
     * @return
     */
    @Action(val = "url", type = 1)
    @Login(val = false)
    public R url(Dict param) {
        SoftUserOfficeData suod = getSoftUserOfficeByPath(param);
        SoftUserOffice suo = suod.getSoftUserOffice();
        String cookieUserId = null;
        if (suo != null) {
            //数据库绑定
            cookieUserId = suo.getUserId();
        } else {
            //数据库未绑定
            if (suod.getHasFileAuth()) {
                cookieUserId = LoginAuthUtil.getUser().getId();
            }
        }
        String cookie = null;
        if (StrUtil.isNotBlank(cookieUserId)) {
            SoftUserData sud = DbUtil.queryObject(CACHE_COOKIE_SQL, SoftUserData.class, cookieUserId);
            if (sud != null) {
                String resStr = HttpUtil.createGet("https://account.wps.cn/p/signin/login_twice_verify/status").cookie(sud.getData()).execute().body();
                JSONObject res = JSONUtil.parseObj(resStr);
                if (StrUtil.equals(res.getStr("result"), "ok")) {
                    cookie = sud.getData();
                }
            }
        }
        if (StrUtil.isBlank(cookie)) {
            // 数据库绑定,cookie无效/数据库未绑定,cookie无效
            if (suod.getHasFileAuth()) {
                //主人
                return R.okData(CommonBean.OfficeUrlData.init(3));
            } else {
                //非主人
                return R.okData(CommonBean.OfficeUrlData.init(4));
            }
        }
        //cookie是有效的
        JSONObject jsFileInfo = null;
        if (suo != null) {
            String resStr = HttpUtil.createGet("https://drive.kdocs.cn/api/v5/links/batch/query?file_ids=" + suo.getJinShanId() + "&with_sharer=true").cookie(cookie).execute().body();
            JSONObject res = JSONUtil.parseObj(resStr);
            JSONArray list = res.getJSONArray("link_list");
            if (list.size() > 0) {
                //file_id,group_id,link_id,link_url
                jsFileInfo = list.getJSONObject(0);
            }
        }
        if (jsFileInfo == null) {
            //文档不存在
            if (!suod.getHasFileAuth()) {
                //非主人 数据库未绑定,cookie无效 / 数据库绑定,cookie有效,金山文档不存在
                return R.okData(CommonBean.OfficeUrlData.init(4));
            }
            boolean isCreate = false;
            if (suo == null) {
                //绑定数据
                isCreate = true;
                suo = new SoftUserOffice();
                suo.setId(suod.getSuoId());
            }
            suo.setExpireTime(LocalDateTime.now().plusDays(30));
            suo.setPath(suod.getPath());
            suo.setUserId(cookieUserId);
            String[] sz = suod.getPath().split("/");
            String parent = ArrayUtil.join(ArrayUtil.remove(sz, sz.length - 1), "/");
            suo.setParentPath(parent);
            suo.setName(suod.getFileInfo().getName());
            jsFileInfo = uplaodFileToKdocs(suod.getPlainPath(), suod.getFileInfo(), cookie);
            suo.setGroupId(jsFileInfo.getStr("group_id"));
            Assert.notNull(jsFileInfo, "当前账号金山文档上传失败");
            Assert.notBlank(jsFileInfo.getStr("file_id"), "当前账号金山文档上传失败");
            suo.setJinShanId(jsFileInfo.getStr("file_id"));
            if (isCreate) {
                DbUtil.insertObject(suo);
            } else {
                DbUtil.upsertObject(suo, "id");
            }
        }
        //文档已存在,获取分享地址
        String resStr = HttpUtil.createGet("https://drive.kdocs.cn/api/v5/links/" + jsFileInfo.getStr("file_id") + "?with_clink=true&with_corp_file_flag=true").cookie(cookie).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        JSONObject clink = res.getJSONObject("clink");
        //1.任何人可编辑 2任何人可评论  3任何人可查看  4关闭 5.未知模式
        int coordinationVal = 0;
        if (clink == null || StrUtil.isBlank(clink.getStr("link_url"))) {
            if (!suod.getHasFileAuth()) {
                // 数据库绑定,cookie有效,金山文档存在,无分享链接
                return R.okData(CommonBean.OfficeUrlData.init(4));
            }
            coordinationVal = 4;
        } else {
            if (StrUtil.equals(clink.getStr("ranges"), "anyone") && StrUtil.equals(clink.getStr("status"), "open")) {
                if (StrUtil.equals(clink.getStr("link_permission"), "write")) {
                    //编辑模式
                    coordinationVal = 1;
                } else if (StrUtil.equals(clink.getStr("link_permission"), "read")) {
                    List<String> ext_perm_list = clink.getBeanList("ext_perm_list", String.class);
                    if (ext_perm_list != null && ext_perm_list.contains("comment")) {
                        coordinationVal = 2;
                    } else {
                        coordinationVal = 3;
                    }
                }
            } else {
                coordinationVal = 5;
            }
        }
        if (suod.getHasFileAuth()) {
            //主人返回文件链接
            return R.okData(CommonBean.OfficeUrlData.init(jsFileInfo.getStr("link_url"),
                    suo.getExpireTime(), 1,
                    coordinationVal));
        } else {
            //非主人返回分享链接
            return R.okData(CommonBean.OfficeUrlData.init(clink.getStr("link_url"),
                    suo.getExpireTime(), 2,
                    coordinationVal
            ));
        }
    }

    private String getCsrfByCookie(String cookie) {
        String csrf = null;
        String[] sz = cookie.split(";");
        for (String oneCookie : sz) {
            oneCookie = oneCookie.trim();
            if (StrUtil.isBlank(oneCookie)) {
                break;
            }
            String[] sz2 = oneCookie.split("=", 2);
            if (StrUtil.equals(sz2[0].trim(), "csrf")) {
                csrf = sz2[1].trim();
                break;
            }
        }
        Assert.notBlank(csrf, "cookie中不存在csrf");
        return csrf;
    }

    private JSONObject uplaodFileToKdocs(FileSystemInface.PlainPath plainPath, CommonBean.PathInfo fileInfo, String cookie) {
        //file_id,group_id,link_id,link_url
        String group_id = getGroupByCookie(cookie);
        String csrf = getCsrfByCookie(cookie);
        File file = FileUtil.writeFromStream(FileSystemUtil.ACTION.getInputStream(plainPath, 0, 0), ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".data");
        JSONObject param = JSONUtil
                .createObj()
                .set("client_stores", "ks3,ks3sh")
                .set("csrfmiddlewaretoken", csrf)
                .set("groupid", group_id)
                .set("name", fileInfo.getName())
                .set("parent_path", new ArrayList<>())
                .set("parentid", 0)
                .set("req_by_internal", false)
                .set("size", fileInfo.getSize())
                .set("startswithfilename", fileInfo.getName())
                .set("successactionstatus", 201);
        String resStr = HttpUtil.createPost("https://drive.kdocs.cn/api/v5/files/upload/create")
                .cookie(cookie)
                .body(param.toString())
                .execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.isTrue(StrUtil.equals("ok", res.getStr("result")), StrUtil.isNotBlank(res.getStr("msg")) ? res.getStr("msg") : "新建文件失败");
        JSONObject uploadinfo = res.getJSONObject("uploadinfo");
        String putFileUrl = uploadinfo.getStr("url");
        JSONObject params = uploadinfo.getJSONObject("params");
        String store = uploadinfo.getStr("store");
        HttpResponse resp = HttpUtil.createPost(putFileUrl).cookie(cookie).form(params).form("file", file).execute();
        String etag = StrUtil.replace(resp.header("ETag"), "\"", "").trim();
        String newfilename = resp.header("newfilename");
        param = JSONUtil.createObj()
                .set("csrfmiddlewaretoken", csrf)
                .set("etag", etag)
                .set("groupid", group_id)
                .set("isUpNewVer", false)
                .set("key", "temp_" + IdUtil.fastSimpleUUID())
                .set("must_create", true)
                .set("name", fileInfo.getName())
                .set("parent_path", new ArrayList<>())
                .set("parentid", 0)
                .set("sha1", newfilename)
                .set("size", fileInfo.getSize())
                .set("store", store);
        resStr = HttpUtil.createPost("https://drive.kdocs.cn/api/v5/files/file")
                .header("referer", "https://www.kdocs.cn/")
                .cookie(cookie).body(param.toString()).execute().body();
        res = JSONUtil.parseObj(resStr);
        String file_id = res.getStr("id");
        Assert.notBlank(file_id, "文件上传失败");
        return JSONUtil.createObj()
                .set("file_id", file_id)
                .set("group_id", res.getStr("groupid"))
                .set("link_id", res.getStr("link_id"))
                .set("link_url", res.getStr("link_url"));
    }

    private String getGroupByCookie(String cookie) {
        String resStr = HttpUtil.createGet("https://t.kdocs.cn/kdteam/api/v1/teams?offset=0&count=50").cookie(cookie).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        if (res.getInt("code") == 0) {
            JSONObject data = res.getJSONObject("data");
            JSONArray teams = data.getJSONArray("teams");
            for (int i = 0; i < teams.size(); i++) {
                JSONObject one = teams.getJSONObject(i);
                if (StrUtil.equals(one.getStr("name"), "Webos Office")) {
                    return one.getStr("id");
                }
            }
        }
        String csrf = getCsrfByCookie(cookie);
        JSONObject param = JSONUtil.createObj().set("corp_id", 0).set("csrfmiddlewaretoken", csrf).set("name", "Webos Office").set("version", 1);
        resStr = HttpUtil.createPost("https://t.kdocs.cn/kdteam/api/v1/teams").cookie(cookie).body(param.toString()).execute().body();
        res = JSONUtil.parseObj(resStr);
        Assert.isTrue(res.getInt("code", -1) == 0, "创建团队失败");
        JSONObject data = res.getJSONObject("data");
        Assert.notNull(data, "创建团队失败");
        Assert.notBlank(data.getStr("id"), "创建团队失败");
        return data.getStr("id");
    }

    @Action(val = "saveCookie", type = 1)
    public R saveCookie(Dict param) {
        //https://www.kdocs.cn/group/1975076871?from=docs
        SysUser user = LoginAuthUtil.getUser();
        String cookie = param.getStr("cookie");
        String url = "https://www.kdocs.cn/?from=docs&show=all";
        HttpResponse resp = HttpUtil.createGet(url).cookie(cookie).execute();
        String wpsua = "";
        List<String> list = resp.headers().get("Set-Cookie");
        for (String str : list) {
            if (str.indexOf("wpsua") != -1) {
                String[] sz = str.split(";");
                for (String str1 : sz) {
                    str1 = str1.trim();
                    if (str1.indexOf("wpsua=") != -1) {
                        wpsua = str1;
                    }
                }
            }
        }
        Assert.notBlank(wpsua, "当前cookie存在问题");
        cookie += ";" + wpsua;
        SoftUserData sud = DbUtil.queryObject(CACHE_COOKIE_SQL, SoftUserData.class, user.getId());
        if (sud == null) {
            sud = new SoftUserData();
            sud.setUserId(user.getId());
            sud.setAppCode(CACHE_COOKIE_APPCODE);
        }
        sud.setData(cookie);
        DbUtil.commonEdit(sud);
        return R.ok();
    }

    @Action(val = "logOut", type = 1)
    @Transactional
    public R logOut(Dict param) {
        SoftUserOfficeData suod = getSoftUserOfficeByPath(param);
        SoftUserOffice suo = suod.getSoftUserOffice();
        Assert.notNull(suo, "未正确读取文件数据");
        SoftUserData sud = DbUtil.queryObject(CACHE_COOKIE_SQL, SoftUserData.class, suo.getUserId());
        Assert.notNull(sud, "未正确读取cookie数据");
        String cookie = sud.getData();
        String csrf = getCsrfByCookie(cookie);
        JSONObject data = JSONUtil
                .createObj()
                .set("csrfmiddlewaretoken", csrf);
        String resStr = HttpUtil.createRequest(Method.DELETE, "https://drive.kdocs.cn/api/v3/groups/" + suo.getGroupId() + "/files/" + suo.getJinShanId()).cookie(cookie).body(data.toString()).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.isTrue(StrUtil.equals(res.getStr("result"), "ok"), "删除文件失败");
        DbUtil.delete("delete from soft_user_office where id = ?", SoftUserOffice.class, suo.getId());
        DbUtil.delete("delete from soft_user_data where user_id = ? and app_code = '" + CACHE_COOKIE_APPCODE + "'", SoftUserData.class, suo.getUserId());
        return R.ok();
    }

    @Action(val = "renewal", type = 1)
    public R renewal(Dict param) {
        SoftUserOfficeData suod = getSoftUserOfficeByPath(param);
        SoftUserOffice suo = suod.getSoftUserOffice();
        Assert.notNull(suo, "未正确读取文件数据");
        LocalDateTime expireTime = LocalDateTime.now().plusDays(30);
        suo.setExpireTime(expireTime);
        DbUtil.upsertObject(suo, "id");
        return R.ok(expireTime, "续期成功");
    }

    @Action(val = "coordination", type = 1)
    @Transactional
    public R coordination(Dict param) {
        //1.任何人可编辑 2任何人可评论  3任何人可查看  4关闭
        Integer value = param.getInt("value");
        SoftUserOfficeData suod = getSoftUserOfficeByPath(param);
        SoftUserOffice suo = suod.getSoftUserOffice();
        Assert.notNull(suo, "未正确读取文件数据");
        SoftUserData sud = DbUtil.queryObject(CACHE_COOKIE_SQL, SoftUserData.class, suo.getUserId());
        Assert.notNull(sud, "未正确读取cookie数据");
        String cookie = sud.getData();
        String csrf = getCsrfByCookie(cookie);
        if (value == 4) {
            String resStr = HttpUtil.createRequest(Method.DELETE, "https://drive.kdocs.cn/api/v5/groups/" + suo.getGroupId() + "/files/" + suo.getJinShanId() + "/share")
                    .cookie(cookie)
                    .body(JSONUtil.createObj().set("csrfmiddlewaretoken", csrf).toString())
                    .execute().body();
            JSONObject res = JSONUtil.parseObj(resStr);
            Assert.isTrue(StrUtil.equals("ok", res.getStr("result")), StrUtil.isNotBlank(res.getStr("msg")) ? res.getStr("msg") : "关闭协同功能失败");
            return R.ok();
        }
        String resStr = HttpUtil.createGet("https://drive.kdocs.cn/api/v5/links/" + suo.getJinShanId() + "?with_clink=true&with_corp_file_flag=true").cookie(cookie).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        JSONObject clink = res.getJSONObject("clink");
        String url;
        Method method;
        JSONObject body = JSONUtil.createObj()
                .set("range", "anyone")
                .set("csrfmiddlewaretoken", csrf)
                .set("clink", true);
        if (value == 1) {
            body.set("permission", "write");
        } else if (value == 2) {
            body.set("permission", "read").set("ext_perm_list", CollUtil.newArrayList("comment"));
        } else if (value == 3) {
            body.set("permission", "read");
        } else {
            Assert.isTrue(false, "参数错误");
        }
        boolean has = false;
        if (clink != null && StrUtil.isNotBlank(clink.getStr("sid"))) {
            if (!StrUtil.equals(clink.getStr("ranges"), "anyone") || !StrUtil.equals(clink.getStr("status"), "open")) {
                HttpUtil.createPost("https://drive.kdocs.cn/api/v5/links")
                        .cookie(cookie)
                        .body(JSONUtil.createObj()
                                .set("clink", true)
                                .set("csrfmiddlewaretoken", csrf)
                                .set("fileid", suo.getJinShanId())
                                .set("range", "anyone")
                                .toString()).execute();
            }
            //存在
            url = "https://drive.kdocs.cn/api/v3/links/" + clink.getStr("sid");
            method = Method.PUT;
            body.set("sid", clink.getStr("sid"))
                    .set("status", "open");
            has = true;
        } else {
            //不存在
            url = "https://drive.kdocs.cn/api/v5/links";
            method = Method.POST;
            body.set("fileid", suo.getJinShanId());
        }
        resStr = HttpUtil.createRequest(method, url).cookie(cookie).body(body.toString()).execute().body();
        res = JSONUtil.parseObj(resStr);
        if (has) {
            Assert.isTrue(res.getLong("id") > 0, "开启协同功能失败");
        } else {
            Assert.isTrue(StrUtil.equals("ok", res.getStr("result")), StrUtil.isNotBlank(res.getStr("msg")) ? res.getStr("msg") : "开启协同功能失败");
        }
        return R.ok();
    }


}

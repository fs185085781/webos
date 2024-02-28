package cn.tenfell.webos.common.filesystem;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.net.multipart.MultipartFormData;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.*;
import cn.tenfell.webos.modules.entity.IoTokenData;
import cn.tenfell.webos.modules.entity.IoUserRecycle;
import org.noear.solon.core.handle.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 可道云挂载
 */
public class KodBoxFileSystem implements FileSystemInface {
    @Override
    public String copy(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path) {
        copyMoveDel(sourceParent, sourceChildren, sourceTypes, path, "copy");
        return "1";
    }

    private void copyMoveDel(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path, String type) {
        String uri = "";
        if (StrUtil.equals(type, "copy")) {
            uri = "explorer/index/pathCopyTo";
        } else if (StrUtil.equals(type, "move")) {
            uri = "explorer/index/pathCuteTo";
        } else if (StrUtil.equals(type, "remove")) {
            uri = "explorer/index/pathDelete";
        }
        Assert.notBlank(uri, "此操作不支持");
        List<Dict> list = new ArrayList<>();
        for (int i = 0; i < sourceChildren.size(); i++) {
            String child = sourceChildren.get(i);
            String pathStr = realPath2kodPath(sourceParent.getRealPath() + "/" + child);
            list.add(Dict.create().set("path", pathStr).set("type", sourceTypes.get(i) == 2 ? "folder" : "file"));
        }
        Dict param = Dict.create().set("dataArr",
                JSONUtil.toJsonStr(list));
        if (StrUtil.equals(type, "copy") || StrUtil.equals(type, "move")) {
            param.set("path", realPath2kodPath(path.getRealPath()));
        }
        this.postData(sourceParent.getTokenId(),
                uri, param);
        this.clearCacheByParent(sourceParent.getTokenId(), realPath2kodPath(sourceParent.getRealPath()));
        if (StrUtil.equals(type, "copy") || StrUtil.equals(type, "move")) {
            this.clearCacheByParent(path.getTokenId(), realPath2kodPath(path.getRealPath()));
        }
    }

    @Override
    public String move(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path) {
        copyMoveDel(sourceParent, sourceChildren, sourceTypes, path, "move");
        return "1";
    }

    @Override
    public boolean rename(PlainPath source, String name, Integer type) {
        String path = realPath2kodPath(source.getRealPath());
        postData(source.getTokenId(), "explorer/index/pathRename", Dict.create().set("path", path).set("newName", name));
        this.clearCacheByParent(source.getTokenId(), realPath2parentKodPath(source.getRealPath()));
        return true;
    }

    @Override
    public String uploadPre(PlainPath path, String name, String expand) {
        String parentPath = realPath2kodPath(path.getRealPath());
        Dict param = JSONUtil.toBean(expand, Dict.class);
        param.set("path", parentPath);
        JSONObject res = postData(path.getTokenId(), "explorer/upload/fileUpload", param);
        if (StrUtil.equals(param.getStr("checkType"), "matchMd5")) {
            this.clearCacheByParent(path.getTokenId(), realPath2kodPath(path.getRealPath()));
            if (res.get("info") instanceof String) {
                return "1";
            } else {
                return "2";
            }
        } else {
            return res.getJSONObject("info").toString();
        }
    }

    @Override
    public String uploadUrl(PlainPath path, String name, String expand) {
        return null;
    }

    @Override
    public String uploadAfter(PlainPath path, String name, String expand) {
        return null;
    }

    @Override
    public String uploadByServer(PlainPath path, String name, InputStream in, File tmpFile, Consumer<Long> consumer) {
        boolean needDel = true;
        if (tmpFile != null) {
            needDel = false;
        } else {
            tmpFile = FileUtil.writeFromStream(in, ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".cache");
        }
        try {
            Integer sjs = RandomUtil.randomInt(100) + 1;
            File file = tmpFile;
            long fileSize = file.length();
            int fpSize = 1024 * 1024 * 5;
            long fps = fileSize / fpSize;
            if (fileSize % fpSize != 0) {
                fps++;
            }
            long modifyTime = 0;
            try {
                BasicFileAttributes basicAttr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
                FileTime updateTime = basicAttr.lastModifiedTime();
                modifyTime = updateTime.toMillis();
            } catch (IOException e) {
            }
            String hashSimple = FileSystemUtil.fileHashSimple((start, length) -> new ShardingInputStream(FileUtil.getInputStream(file), start, length), fileSize);
            String fileMd5 = SecureUtil.md5(file);
            JSONObject expand = JSONUtil.createObj()
                    .set("fullPath", "/" + name)
                    .set("name", FileUtil.getName(name))
                    .set("checkType", "matchMd5")
                    .set("checkHashSimple", hashSimple)
                    .set("checkHashMd5", fileMd5)
                    .set("size", fileSize)
                    .set("modifyTime", modifyTime)
                    .set("chunkSize", fpSize)
                    .set("chunks", 0);
            //秒传检查
            String flag = uploadPre(path, name, expand.toString());
            if (StrUtil.equals(flag, "1")) {
                this.clearCacheByParent(path.getTokenId(), realPath2kodPath(path.getRealPath()));
                return "1";
            }
            //获取已经上传的数量
            expand = JSONUtil.createObj()
                    .set("fullPath", "/" + name)
                    .set("name", FileUtil.getName(name))
                    .set("checkType", "checkHash")
                    .set("checkHashSimple", hashSimple)
                    .set("size", fileSize)
                    .set("modifyTime", modifyTime)
                    .set("chunkSize", fpSize)
                    .set("chunks", 0);
            String resStr = uploadPre(path, name, expand.toString());
            JSONObject uploadPreRes = JSONUtil.parseObj(resStr);
            if (uploadPreRes.get("checkChunkArray") instanceof JSONArray) {
                uploadPreRes.set("checkChunkArray", JSONUtil.createObj());
            }
            JSONObject hasPushMap = uploadPreRes.getJSONObject("checkChunkArray");
            for (int i = 0; i < fps; i++) {
                if (consumer != null) {
                    consumer.accept((long) i * fpSize);
                }
                int start = i * fpSize;
                long length = fpSize;
                if (fileSize - start < length) {
                    length = fileSize - start;
                }
                File tmp = FileUtil.writeFromStream(new ShardingInputStream(FileUtil.getInputStream(file), start, length),
                        ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".data");
                String uploadSimple = hasPushMap.getStr("part_" + i);
                if (StrUtil.isNotBlank(uploadSimple)) {
                    String partHashSimple = FileSystemUtil.fileHashSimple((start1, length1) -> new ShardingInputStream(FileUtil.getInputStream(tmp), start1, length1), length);
                    if (StrUtil.equals(uploadSimple, partHashSimple)) {
                        FileUtil.del(tmp);
                        continue;
                    }
                }
                try {
                    Dict param = Dict.create()
                            .set("id", "WU_FILE_" + sjs)
                            .set("name", FileUtil.getName(name))
                            .set("size", fileSize)
                            .set("chunks", fps)
                            .set("chunk", i)
                            .set("fullPath", "/" + name)
                            .set("modifyTime", modifyTime)
                            .set("checkHashSimple", hashSimple)
                            .set("chunkSize", fpSize)
                            .set("file", tmp)
                            .set("path", realPath2kodPath(path.getRealPath()));
                    JSONObject res = postData(path.getTokenId(), "explorer/upload/fileUpload", param);
                    if (res.get("info") != null) {
                        if (consumer != null) {
                            consumer.accept(fileSize);
                        }
                        this.clearCacheByParent(path.getTokenId(), realPath2kodPath(path.getRealPath()));
                        return "1";
                    }
                } catch (Exception e) {
                    ProjectUtil.showConsoleErr(e);
                } finally {
                    FileUtil.del(tmp);
                }
            }
            return "";
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
        } finally {
            if (needDel) {
                FileUtil.del(tmpFile);
            }
        }
        return "";
    }

    @Override
    public String downloadUrl(PlainPath path) {
        String pathStr = realPath2kodPath(path.getRealPath());
        JSONObject res = this.postData(path.getTokenId(), "explorer/index/pathInfo", Dict.create()
                .set("dataArr", JSONUtil.createArray().set(Dict.create().set("path", pathStr)).toString())
        );
        String str = res.getJSONObject("data").getStr("downloadPath");
        return CommonUtil.getLastUrl(str);
    }

    @Override
    public String createDir(PlainPath parentPath, String pathName) {
        String path = realPath2kodPath(parentPath.getRealPath());
        JSONObject res = this.postData(parentPath.getTokenId(), "explorer/index/mkdir", Dict.create()
                .set("path", path + "/" + pathName)
        );
        this.clearCacheByParent(parentPath.getTokenId(), path);
        return realPath2kodPath(res.getStr("info"));
    }

    private JSONObject postData(String tokenId, String uri, Dict data) {
        IoTokenData itd = TokenDataUtil.getTokenDataByIdOrToken(FileSystemUtil.KODBOX_DRIVE_TYPE, tokenId);
        JSONObject tokenData = JSONUtil.parseObj(itd.getTokenData());
        String accessToken = tokenData.getStr("accessToken");
        String host = tokenData.getStr("host");
        data.set("accessToken", accessToken);
        data.set("API_ROUTE", uri);
        String resStr = HttpUtil.post(host + "/index.php?" + uri, data);
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.isTrue(res.getBool("code", false), res.getStr("data"));
        return res;
    }

    private CommonBean.PathInfo file2info(JSONObject item) {
        CommonBean.PathInfo pi = new CommonBean.PathInfo();
        String path = item.getStr("path");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (StrUtil.isBlank(path)) {
            //root代替斜杠
            path = "{root}";
        }
        String[] sz = path.split("/");
        pi.setName(item.getStr("name"));
        pi.setPath(sz[sz.length - 1]);
        if (item.getLong("createTime") != null) {
            pi.setCreatedAt(DateUtil.date(item.getLong("createTime") * 1000L).toString());
        }
        if (item.getLong("modifyTime") != null) {
            pi.setUpdatedAt(DateUtil.date(item.getLong("modifyTime") * 1000L).toString());
        }
        if (StrUtil.equals(item.getStr("type"), "file")) {
            pi.setSize(item.getLong("size"));
            pi.setType(1);
            if (item.getJSONObject("fileInfo") != null) {
                pi.setMd5(item.getJSONObject("fileInfo").getStr("hashMd5"));
            }
            pi.setExt(item.getStr("ext"));
            pi.setThumbnail(item.getStr("fileThumb"));
        } else {
            pi.setType(2);
        }
        return pi;
    }

    private List<CommonBean.PathInfo> items2infos(JSONArray folderList, JSONArray fileList) {
        List<CommonBean.PathInfo> list = new ArrayList<>();
        if (folderList != null) {
            for (int i = 0; i < folderList.size(); i++) {
                JSONObject item = folderList.getJSONObject(i);
                CommonBean.PathInfo info = file2info(item);
                list.add(info);
            }
        }
        if (fileList != null) {
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject item = fileList.getJSONObject(i);
                CommonBean.PathInfo info = file2info(item);
                list.add(info);
            }
        }
        return list;
    }

    private String realPath2kodPath(String realPath) {
        String[] sz = realPath.split("\\{");
        String path = "{" + sz[sz.length - 1];
        path = CommonUtil.trimPath(path);
        if (StrUtil.startWith(path, "{root}")) {
            path = CommonUtil.replaceFirst(path, "{root}", "/").replaceAll("//", "/");
        }
        return path;
    }

    private String realPath2parentKodPath(String realPath) {
        return realPath2kodPath(CommonUtil.getParentPath(realPath));
    }

    private void clearCacheByParent(String tokenId, String parentPath) {
        String key = "kodbox:filelist:" + tokenId + ":" + SecureUtil.md5(parentPath) + ":*";
        CacheUtil.delCacheData(key);
    }

    @Override
    public CommonBean.Page<CommonBean.PathInfo> listFiles(PlainPath parentPath, String tmpNext) {
        String path = realPath2kodPath(parentPath.getRealPath());
        if (StrUtil.isBlank(tmpNext)) {
            tmpNext = "1";
        }
        final int current = Convert.toInt(tmpNext);
        String marker = SecureUtil.md5(current + "1");
        String redisKey = "kodbox:filelist:" + parentPath.getTokenId() + ":" + SecureUtil.md5(path) + ":" + marker;
        return CacheUtil.getCacheData(redisKey, () -> {
            JSONObject res = postData(parentPath.getTokenId(), "explorer/list/path",
                    Dict.create()
                            .set("path", path)
                            .set("page", current)
                            .set("pageNum", "500")
            );
            JSONObject data = res.getJSONObject("data");
            JSONArray folderList = data.getJSONArray("folderList");
            JSONArray fileList = data.getJSONArray("fileList");
            CommonBean.Page<CommonBean.PathInfo> page = new CommonBean.Page<>();
            int pageTotal = data.getJSONObject("pageInfo").getInt("pageTotal");
            if (pageTotal > current) {
                page.setNext(Convert.toStr(current + 1));
            } else {
                page.setNext("");
            }
            List<CommonBean.PathInfo> list = items2infos(folderList, fileList);
            page.setList(list);
            page.setType(1);
            return page;
        }, 0);
    }

    private static String usernamePassword2accessToken(String host, String username, String password) {
        String csrf = RandomUtil.randomString(16);
        HttpResponse resp = HttpUtil.createPost(host + "?user/index/loginSubmit").form(Dict.create()
                .set("name", username)
                .set("password", password)
                .set("rememberPassword", "0")
                .set("salt", "1")
                .set("CSRF_TOKEN", csrf)
                .set("API_ROUTE", "user/index/loginSubmit")
        ).execute();
        String resStr = resp.body();
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.isTrue(res.getBool("code", false), "用户名或密码不正确");
        return res.getStr("info");
    }

    private static Dict accessToken2user(String host, String accessToken) {
        String resStr = HttpUtil.createGet(host + "?user/view/options&accessToken=" + accessToken).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        JSONObject data = res.getJSONObject("data");
        if (data != null) {
            JSONObject user = data.getJSONObject("user");
            if (user != null) {
                String userId = user.getStr("userID");
                if (StrUtil.isNotBlank(userId)) {
                    return Dict.create().set("userId", userId).set("accessToken", data.getJSONObject("kod").getStr("accessToken"));
                }
            }
        }
        return null;
    }

    @Override
    public void refreshToken(String driveType, IoTokenData itd) {
        JSONObject tokenData = JSONUtil.parseObj(itd.getTokenData());
        String host = tokenData.getStr("host");
        String username = tokenData.getStr("username");
        String password = tokenData.getStr("password");
        String accessToken = tokenData.getStr("accessToken");
        String userId = null;
        boolean has = false;
        if (StrUtil.isNotBlank(accessToken)) {
            Dict user = accessToken2user(host, accessToken);
            if (user == null) {
                accessToken = null;
            } else {
                accessToken = user.getStr("accessToken");
                userId = user.getStr("userId");
                has = true;
            }
        }
        if (StrUtil.isBlank(accessToken)) {
            accessToken = usernamePassword2accessToken(host, username, password);
        }
        Assert.notBlank(accessToken, "当前账号有误,获取token失败");
        if (!has) {
            Dict user = accessToken2user(host, accessToken);
            Assert.notNull(user, "当前账号有误,获取用户信息失败");
            accessToken = user.getStr("accessToken");
            userId = user.getStr("userId");
        }
        tokenData.set("userId", userId);
        tokenData.set("accessToken", accessToken);
        itd.setTokenData(tokenData.toString());
        itd.setDriveType(driveType);
        itd.setId(SecureUtil.md5(driveType + host + userId));
        itd.setExpireTime(LocalDateTime.now().plusMinutes(30L));
    }

    @Override
    public String remove(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes) {
        copyMoveDel(sourceParent, sourceChildren, sourceTypes, null, "remove");
        return "1";
    }

    @Override
    public String pathName(PlainPath plainPath) {
        Dict pathMap = Dict.create()
                .set("{block:driver}", "网络挂载")
                .set("{block:tools}", "工具")
                .set("{block:files}", "位置")
                .set("{block:fileType}", "文件类型")
                .set("{userRecycle}", "回收站")
                .set("{userShareLink}", "外链分享")
                .set("{userFileType:photo}", "我的相册")
                .set("{userShare}", "我的协作")
                .set("{userRencent}", "最近文档")
                .set("{block:fileTag}", "标签");
        String[] paths = plainPath.getCipherPath().split("/");
        String path = realPath2kodPath(plainPath.getRealPath());
        String redisKey = "kodbox:pathName:" + SecureUtil.md5(path);
        JSONArray items = CacheUtil.getCacheData(redisKey, () -> {
            JSONObject res = postData(plainPath.getTokenId(), "explorer/list/path",
                    Dict.create()
                            .set("path", path)
                            .set("page", 1)
                            .set("pageNum", "1")
            );
            JSONObject data = res.getJSONObject("data");
            JSONObject current = data.getJSONObject("current");
            JSONArray tmp = JSONUtil.createArray();
            String pathDisplay = current.getStr("pathDisplay");
            if (StrUtil.startWith(pathDisplay, "/")) {
                pathDisplay = "根路径" + pathDisplay;
            }
            String kodPath = current.getStr("path");
            kodPath = CommonUtil.trimPath(kodPath);
            String[] pathDisplays = pathDisplay.split("/");
            String[] kodPaths = kodPath.split("/");
            for (int i = 0; i < pathDisplays.length; i++) {
                String fileId = "";
                int pathIndex = i + kodPaths.length - pathDisplays.length;
                if (pathIndex >= 0) {
                    fileId = kodPaths[pathIndex];
                }
                tmp.add(JSONUtil.createObj().set("fileId", fileId).set("name", pathDisplays[i]));
            }
            return tmp;
        }, 0);
        int length = paths.length;
        for (int i = length - 1; i > 0; i--) {
            int n = items.size() - paths.length + i;
            if (n <= 0) {
                if (pathMap.get(paths[i]) != null) {
                    paths[i] = pathMap.getStr(paths[i]);
                    continue;
                }
                if (n < 0) {
                    continue;
                }
            }
            JSONObject item = items.getJSONObject(n);
            if (StrUtil.isNotBlank(item.getStr("fileId"))) {
                Assert.isTrue(item != null && StrUtil.equals(item.getStr("fileId"), paths[i]), "路径非法");
            }
            paths[i] = item.getStr("name");
        }
        return ArrayUtil.join(paths, "/");
    }

    @Override
    public String availableMainName(PlainPath parentPath, String mainName, String ext) {
        int index = 0;
        while (true) {
            String tmpMainName = mainName;
            if (index > 0) {
                tmpMainName += "(" + index + ")";
            }
            String name = tmpMainName;
            if (StrUtil.isNotBlank(ext)) {
                name += "." + ext;
            }
            List<CommonBean.PathInfo> items = this.searchFile(parentPath, name);
            if (items.size() == 0) {
                return tmpMainName;
            }
            index++;
        }
    }

    @Override
    public String unzip(PlainPath file) {
        String path = realPath2kodPath(file.getRealPath());
        String parentPath = realPath2kodPath(CommonUtil.getParentPath(file.getRealPath()));
        postData(file.getTokenId(), "explorer/index/unzip", Dict.create().set("path", path).set("pathTo", parentPath));
        clearCacheByParent(file.getTokenId(), parentPath);
        return "1";
    }

    @Override
    public String zip(List<PlainPath> files, PlainPath dirPath) {
        List<Dict> list = new ArrayList<>();
        for (PlainPath file : files) {
            list.add(Dict.create()
                    .set("path", realPath2kodPath(file.getRealPath()))
            );
        }
        Dict param = Dict.create().set("type", "zip").set("dataArr", JSONUtil.toJsonStr(list));
        JSONObject res = postData(dirPath.getTokenId(), "explorer/index/zip", param);
        String parentPath = realPath2kodPath(dirPath.getRealPath());
        String newPath = res.getStr("info");
        String newMainName = availableMainName(dirPath, "压缩文件", "zip");
        dirPath.setRealPath(dirPath.getRealPath() + "/" + newPath);
        boolean flag = rename(dirPath, newMainName + ".zip", 1);
        if (!flag) {
            clearCacheByParent(dirPath.getTokenId(), parentPath);
        }
        return "1";
    }

    @Override
    public CommonBean.PathInfo fileInfo(PlainPath plainPath) {
        String pathStr = realPath2kodPath(plainPath.getRealPath());
        JSONObject res = this.postData(plainPath.getTokenId(), "explorer/index/pathInfo",
                Dict.create().set("dataArr", JSONUtil.createArray().set(Dict.create().set("path", pathStr)).toString()));
        return file2info(res.getJSONObject("data"));
    }

    @Override
    public CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit) {
        CommonBean.WpsUrlData data = new CommonBean.WpsUrlData();
        data.setType(2);
        return data;
    }

    @Override
    public List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name) {
        String path = "{search}/parentPath=" + URLEncodeUtil.encodeQuery(realPath2kodPath(parentPath.getRealPath())) + "@words=" + URLEncodeUtil.encodeQuery(name);
        JSONObject res = postData(parentPath.getTokenId(), "explorer/list/path", Dict.create().set("path", path).set("page", 1).set("pageNum", 500));
        JSONArray fileList = res.getJSONObject("data").getJSONArray("fileList");
        return items2infos(null, fileList);
    }

    @Override
    public InputStream getInputStream(PlainPath path, long start, long length) {
        String url = downloadUrl(path);
        HttpRequest req = HttpUtil.createGet(url).setMaxRedirectCount(100);
        if (length > 0) {
            //局部
            req.header("Range", "bytes=" + start + "-" + (length + start - 1));
        } else {
            //全部
        }
        return req.executeAsync().bodyStream();
    }

    @Override
    public String secondTransmission(PlainPath path, String name, String sourceCipherPath, long size) {
        //秒传检查
        return null;
    }

    @Override
    public String sameDriveCopy(String driveType, CommonBean.TransmissionFile file) {
        //同盘不同号,不支持
        return null;
    }

    @Override
    public String sha1(PlainPath path) {
        //不支持
        return null;
    }

    @Override
    public String md5(PlainPath path) {
        CommonBean.PathInfo info = fileInfo(path);
        if (info != null) {
            return info.getMd5();
        }
        return null;
    }

    @Override
    public boolean restore(PlainPath path, IoUserRecycle ioUserRecycle) {
        List<Dict> list = new ArrayList<>();
        list.add(Dict.create().set("path", realPath2kodPath(ioUserRecycle.getRemovePath())).set("type", ioUserRecycle.getType() == 2 ? "folder" : "file"));
        Dict param = Dict.create().set("dataArr",
                JSONUtil.toJsonStr(list));
        postData(path.getTokenId(), "explorer/index/recycleRestore", param);
        clearCacheByParent(path.getTokenId(), realPath2kodPath(CommonUtil.getParentPath(ioUserRecycle.getRemovePath())));
        return true;
    }

    @Override
    public String getRootId(String driveType) {
        return "{block:root}";
    }

    @Override
    public R commonReq(PlainPath path, Context ctx) {
        if (StrUtil.equals(ctx.param("API_ROUTE"), "explorer/upload/fileUpload")) {
            //分片上传
            String filePath = ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".data";
            try {
                MultipartFormData multiForm = ProjectContext.getMultipart(ctx);
                File file = multiForm.getFile("file").write(filePath);
                Dict param = Dict.parse(ctx.paramMap());
                param.set("file", file);
                param.remove("module");
                param.remove("action");
                param.set("path", realPath2kodPath(path.getRealPath()));
                JSONObject res = postData(path.getTokenId(), "explorer/upload/fileUpload", param);
                return R.okData(res);
            } catch (Exception e) {
                return R.error(e);
            } finally {
                FileUtil.del(filePath);
            }
        }
        return null;
    }
}

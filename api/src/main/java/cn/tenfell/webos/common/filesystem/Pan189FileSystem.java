package cn.tenfell.webos.common.filesystem;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.http.*;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.CacheUtil;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.common.util.ShardingInputStream;
import cn.tenfell.webos.common.util.TokenDataUtil;
import cn.tenfell.webos.modules.entity.IoTokenData;
import cn.tenfell.webos.modules.entity.IoUserRecycle;
import lombok.Data;
import org.noear.solon.core.handle.Context;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Pan189FileSystem implements FileSystemInface {
    String apiHost = "https://cloud.189.cn/api";

    @Override
    public String copy(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path) {
        return copyOrMoveDelete(sourceParent, sourceChildren, sourceTypes, path, "COPY", null);
    }

    @Override
    public String move(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path) {
        return copyOrMoveDelete(sourceParent, sourceChildren, sourceTypes, path, "MOVE", null);
    }

    @Data
    private static class RestoreFile {
        private String name;
        private Integer type;

        public static RestoreFile init(String name, Integer type) {
            RestoreFile rf = new RestoreFile();
            rf.setName(name);
            rf.setType(type);
            return rf;
        }
    }

    private String copyOrMoveDelete(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path, String type, RestoreFile rf) {
        Pan189FileSystem that = this;
        String oldRealPath = sourceParent.getRealPath();
        String oldCipherPath = sourceParent.getCipherPath();
        String target_file_id = "";
        if (type.equals("COPY") || type.equals("MOVE")) {
            String[] szTarget = path.getRealPath().split("/");
            target_file_id = szTarget[szTarget.length - 1];
        }
        String[] sz = sourceParent.getRealPath().split("/");
        String source_file_id = sz[sz.length - 1];
        String sourceParentReal = sourceParent.getRealPath();
        String sourceParentCipher = sourceParent.getCipherPath();
        String uri = "/open/batch/createBatchTask.action";
        List<Dict> actionDataList = new ArrayList<>();
        for (int i = 0; i < sourceChildren.size(); i++) {
            String fileId = sourceChildren.get(i);
            sourceParent.setRealPath(sourceParentReal + "/" + fileId);
            sourceParent.setCipherPath(sourceParentCipher + "/" + fileId);
            if (rf == null) {
                CommonBean.PathInfo info = this.fileInfo(sourceParent);
                actionDataList.add(Dict.create()
                        .set("fileId", fileId)
                        .set("fileName", info.getName())
                        .set("isFolder", sourceTypes.get(i) == 2 ? 1 : 0)
                );
            } else {
                actionDataList.add(Dict.create()
                        .set("fileId", fileId)
                        .set("fileName", rf.getName())
                        .set("isFolder", rf.getType() != 1 ? 1 : 0)
                );
            }

        }
        Dict data = Dict.create()
                .set("type", type)
                .set("targetFolderId", target_file_id)
                .set("taskInfos", JSONUtil.toJsonStr(actionDataList));
        JSONObject res = postData(uri, data, sourceParent.getTokenId(), 0);
        boolean flag = StrUtil.equals(res.getStr("res_code"), "0");
        if (flag) {
            String taskId = res.getStr("taskId");
            int count = 0;
            while (true) {
                uri = "/batch/checkBatchTask.action";
                data = Dict.create()
                        .set("type", type)
                        .set("taskId", taskId);
                res = postData(uri, data, sourceParent.getTokenId(), 0);
                flag = StrUtil.equals(res.getStr("res_code"), "0");
                if (res.getInt("taskStatus", 1) == 4) {
                    break;
                }
                ThreadUtil.sleep(1000L);
                count++;
                if (count > 10) {
                    sourceParent.setRealPath(oldRealPath);
                    sourceParent.setCipherPath(oldCipherPath);
                    return "0";
                }
            }
        }
        if (flag) {
            that.clearCacheByParent(sourceParent.getTokenId(), source_file_id);
            if (type.equals("COPY") || type.equals("MOVE")) {
                that.clearCacheByParent(path.getTokenId(), target_file_id);
            }
        }
        sourceParent.setRealPath(oldRealPath);
        sourceParent.setCipherPath(oldCipherPath);
        return flag ? "1" : "0";
    }

    @Override
    public boolean rename(PlainPath source, String name, Integer type) {
        String[] sz = source.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        String uri = "/open/file/renameFile.action";
        Dict map = Dict.create().set("fileId", file_id).set("destFileName", name);
        if (type == 2) {
            uri = "/open/file/renameFolder.action";
            map = Dict.create().set("folderId", file_id).set("destFolderName", name);
        }
        JSONObject res = postData(uri, map, source.getTokenId(), 0);
        boolean flag = StrUtil.equals(res.getStr("res_code"), "0");
        if (flag) {
            this.clearCacheByParent(source.getTokenId(), sz[sz.length - 2]);
        }
        return flag;
    }

    private void clearCacheByParent(String tokenId, String parent_file_id) {
        String key = "pan189:filelist:" + tokenId + ":" + parent_file_id + ":*";
        CacheUtil.delCacheData(key);
    }

    @Override
    public String uploadPre(PlainPath path, String name, String expand) {
        JSONObject exp = JSONUtil.parseObj(expand);
        Integer currentType = exp.getInt("currentType");
        JSONObject data = exp.getJSONObject("data");
        if (currentType == 1) {
            //获取上传id
            Dict parentData = this.getParentPathIdAndName(path, name);
            String parentFolderId = parentData.getStr("parentFolderId");
            Dict param = data.toBean(Dict.class);
            param.set("parentFolderId", parentFolderId);
            JSONObject res = jmGetData("/person/initMultiUpload",
                    param,
                    path.getTokenId());
            return res.getJSONObject("data").toString();
        } else if (currentType == 2) {
            //查询已上传分片数量
        } else if (currentType == 3) {
            //秒传检查
            Dict param = data.toBean(Dict.class);
            JSONObject res = jmGetData("/person/checkTransSecond",
                    param,
                    path.getTokenId());
            return res.getJSONObject("data").toString();
        }
        return "";
    }

    @Override
    public String uploadUrl(PlainPath path, String name, String expand) {
        Dict data = JSONUtil.toBean(expand, Dict.class);
        JSONObject putUrlRes = jmGetData("/person/getMultiUploadUrls", data, path.getTokenId());
        return putUrlRes.getJSONObject("uploadUrls").toString();
    }

    @Override
    public String uploadAfter(PlainPath path, String name, String expand) {
        Dict data = JSONUtil.toBean(expand, Dict.class);
        JSONObject res = jmGetData("/person/commitMultiUploadFile", data, path.getTokenId());
        JSONObject resFile = res.getJSONObject("file");
        String fileId = resFile.getStr("userFileId");
        if (StrUtil.isNotBlank(fileId)) {
            Dict parentData = this.getParentPathIdAndName(path, name);
            String parentFolderId = parentData.getStr("parentFolderId");
            String lastName = parentData.getStr("name");
            if (!StrUtil.equals(lastName, resFile.getStr("fileName"))) {
                this.coverFile(path.getTokenId(), parentFolderId, fileId, lastName);
            }
            String[] sz = path.getRealPath().split("/");
            this.clearCacheByParent(path.getTokenId(), sz[sz.length - 1]);
            return "1";
        } else {
            return "0";
        }
    }

    private Dict getParentPathIdAndName(PlainPath path, String name) {
        String[] sz = path.getRealPath().split("/");
        String parentFolderId = sz[sz.length - 1];
        String[] pathSz = name.split("/");
        String lastName = pathSz[pathSz.length - 1];
        if (pathSz.length > 1) {
            List<String> dirs = new ArrayList<>();
            for (int i = 0; i < pathSz.length - 1; i++) {
                List<String> tmp = new ArrayList<>();
                for (int j = 0; j <= i; j++) {
                    tmp.add(pathSz[j]);
                }
                dirs.add(CollUtil.join(tmp, "/"));
            }
            JSONObject map = this.createDirPl(path, dirs);
            parentFolderId = map.getStr(dirs.get(dirs.size() - 1));
        }
        return Dict.create().set("parentFolderId", parentFolderId).set("name", lastName);
    }

    @Override
    public String uploadByServer(PlainPath path, String name, InputStream in, File file, Consumer<Long> consumer) {
        boolean needDel = true;
        if (file != null) {
            needDel = false;
        } else {
            file = FileUtil.writeFromStream(in, ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".cache");
        }
        try {
            Dict parentData = this.getParentPathIdAndName(path, name);
            String parentFolderId = parentData.getStr("parentFolderId");
            String lastName = parentData.getStr("name");
            long size = file.length();
            int tenMSize = 10485760;
            long fpSize = 0;
            if (size >= 0 && size < 1000 * tenMSize) {
                //10M
                fpSize = tenMSize * 1;
            } else if (1000 * tenMSize >= 0 && size < 2000 * tenMSize) {
                //20M
                fpSize = tenMSize * 2;
            } else if (size >= 2000 * tenMSize && size < 10000 * tenMSize) {
                //50M
                fpSize = tenMSize * 5;
            } else {
                long n = size / 2000 / tenMSize;
                if (size % (2000 * tenMSize) != 0) {
                    n++;
                }
                fpSize = tenMSize * n;
            }
            long fpSl = size / fpSize;
            if (size % fpSize != 0) {
                fpSl++;
            }
            String fileMd5 = SecureUtil.md5(file);
            StringBuilder sb = new StringBuilder();
            List<Dict> fps = new ArrayList<>();
            String sliceMd5;
            if (fpSl > 1) {
                for (int i = 1; i <= fpSl; i++) {
                    long length = fpSize;
                    if (size - (i - 1) * fpSize < length) {
                        length = size - (i - 1) * fpSize;
                    }
                    if (i > 1) {
                        sb.append("\n");
                    }
                    InputStream fp = new ShardingInputStream(FileUtil.getInputStream(file), (i - 1) * fpSize, length);
                    String fpMd5 = SecureUtil.md5(fp);
                    sb.append(fpMd5.toUpperCase());
                    IoUtil.close(fp);
                    fps.add(Dict.create().set("index", i).set("fpMd5", fpMd5));
                }
                sliceMd5 = SecureUtil.md5(sb.toString());
            } else {
                sliceMd5 = fileMd5;
                fps.add(Dict.create().set("index", 1).set("fpMd5", fileMd5.toUpperCase()));
            }
            JSONObject param = JSONUtil.createObj().set("currentType", 1).set("data", Dict.create()
                    .set("fileName", name)
                    .set("fileSize", file.length())
                    .set("sliceSize", fpSize)
                    .set("fileMd5", fileMd5)
                    .set("sliceMd5", sliceMd5));
            JSONObject data = JSONUtil.parseObj(uploadPre(path, name, param.toString()));
            if (data.getInt("fileDataExists") == 0) {
                //文件不存在需要上传
                for (int n = 0; n < fps.size(); n++) {
                    if (consumer != null) {
                        consumer.accept((long) n * fpSize);
                    }
                    Dict fpData = fps.get(n);
                    int i = fpData.getInt("index");
                    String fpMd5 = fpData.getStr("fpMd5");
                    long length = fpSize;
                    if (size - (i - 1) * fpSize < length) {
                        length = size - (i - 1) * fpSize;
                    }
                    InputStream fp = new ShardingInputStream(FileUtil.getInputStream(file), (i - 1) * fpSize, length);
                    JSONObject urlsParam = JSONUtil.createObj()
                            .set("uploadFileId", data.getStr("uploadFileId"))
                            .set("partInfo", i + "-" + Base64.encode(HexUtil.decodeHex(fpMd5)));
                    JSONObject putUrlData = JSONUtil.parseObj(uploadUrl(path, name, urlsParam.toString())).getJSONObject("partNumber_" + i);
                    String putUrl = putUrlData.getStr("requestURL");
                    String putHeader = putUrlData.getStr("requestHeader");
                    String[] phsz = putHeader.split("&");
                    Map<String, String> headerMap = new HashMap<>();
                    for (String ph : phsz) {
                        String[] isz = ph.split("=", 2);
                        headerMap.put(isz[0], isz[1]);
                    }
                    headerMap.put("content-length", Convert.toStr(length));
                    putFile(putUrl, fp, headerMap, (long) n * fpSize, consumer);
                }
            }
            //文件上传完成可以进行提交
            Dict compParam = Dict.create()
                    .set("uploadFileId", data.getStr("uploadFileId"))
                    .set("lazyCheck", 0)
                    .set("fileMd5", fileMd5)
                    .set("sliceMd5", sb.toString());
            JSONObject res = jmGetData("/person/commitMultiUploadFile", compParam, path.getTokenId());
            if (consumer != null) {
                consumer.accept(size);
            }
            JSONObject resFile = res.getJSONObject("file");
            String fileId = resFile.getStr("userFileId");
            if (StrUtil.isNotBlank(fileId)) {
                if (!StrUtil.equals(resFile.getStr("fileName"), lastName)) {
                    //不相等,需要删掉旧文件,新文件改名成旧文件
                    this.coverFile(path.getTokenId(), parentFolderId, fileId, lastName);
                }
                String[] sz = path.getRealPath().split("/");
                this.clearCacheByParent(path.getTokenId(), sz[sz.length - 1]);
            }
            return fileId;
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
        } finally {
            if (needDel) {
                FileUtil.del(file);
            }
        }
        return "";
    }

    private void coverFile(String tokenId, String parentFolderId, String newFileId, String oldName) {
        JSONObject res = postData("/open/file/searchFiles.action", Dict.create()
                        .set("folderId", parentFolderId)
                        .set("pageSize", 60)
                        .set("pageNum", 1)
                        .set("fileName", oldName)
                        .set("fileType", 1)
                , tokenId, 0);
        JSONArray fileList = res.getJSONArray("fileList");
        String oldFileId = "";
        if (fileList != null) {
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject file = fileList.getJSONObject(i);
                if (StrUtil.equals(file.getStr("name"), oldName)) {
                    oldFileId = file.getStr("id");
                    break;
                }
            }
        }
        PlainPath path = new PlainPath();
        path.setTokenId(tokenId);
        if (StrUtil.isNotBlank(oldFileId)) {
            path.setRealPath(parentFolderId);
            Assert.isTrue(StrUtil.equals(this.remove(path, CollUtil.newArrayList(oldFileId), CollUtil.newArrayList(1)), "1"), "删除文件失败");
        }
        path.setRealPath(parentFolderId + "/" + newFileId);
        Assert.isTrue(this.rename(path, oldName, 1), "重命名文件失败");
    }

    private void putFile(String url, InputStream in, Map<String, String> headerMap, long start, Consumer<Long> consumer) {
        OutputStream out = null;
        try {
            byte[] fpData = IoUtil.readBytes(in);
            in = new ByteArrayInputStream(fpData);
            HttpConnection connection = HttpConnection.create(url, null)
                    .setConnectTimeout(HttpGlobalConfig.getTimeout())
                    .setReadTimeout(HttpGlobalConfig.getTimeout())
                    .setInstanceFollowRedirects(false)
                    .setMethod(Method.PUT);
            HttpURLConnection conn = (HttpURLConnection) ReflectUtil.getFieldValue(connection, "conn");
            headerMap.forEach((k, v) -> connection.header(k, v, true));
            conn.setFixedLengthStreamingMode(Convert.toLong(headerMap.get("content-length")));
            out = connection.getOutputStream();
            IoUtil.copy(in, out, NioUtil.DEFAULT_BUFFER_SIZE, new StreamProgress() {
                @Override
                public void start() {
                }

                @Override
                public void progress(long total, long progressSize) {
                    if (consumer != null) {
                        consumer.accept(start + progressSize);
                    }
                }

                @Override
                public void finish() {
                }
            });

            connection.responseCode();
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
        } finally {
            IoUtil.close(in);
            if (out != null) {
                IoUtil.close(out);
            }
        }
    }

    @Override
    public String downloadUrl(PlainPath path) {
        Pan189FileSystem that = this;
        String[] sz = path.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        return CacheUtil.getCacheData("pan189:downurl:" + file_id, () -> {
            JSONObject res = that.postData("/open/file/getFileDownloadUrl.action", Dict.create().set("fileId", file_id).set("forcedGet", "1"), path.getTokenId(), 0);
            String url = res.getStr("fileDownloadUrl");
            HttpResponse response = HttpUtil.createGet(url).execute();
            String tmpUrl = response.header("Location");
            if (StrUtil.isNotBlank(tmpUrl)) {
                url = tmpUrl;
            }
            return url;
        }, 120);
    }

    private JSONObject createDirPl(PlainPath path, List<String> dirs) {
        String[] sz = path.getRealPath().split("/");
        String parentFileId = sz[sz.length - 1];
        JSONObject res = this.postData("/portal/createFolders.action",
                Dict.create().set("opScene", 1).
                        set("folderList", JSONUtil.createObj()
                                .set("parentId", parentFileId)
                                .set("paths", dirs)
                                .toString()
                        ),
                path.getTokenId(),
                0
        );
        this.clearCacheByParent(path.getTokenId(), parentFileId);
        return res;
    }

    @Override
    public String createDir(PlainPath parentPath, String pathName) {
        return this.createDirPl(parentPath, CollUtil.newArrayList(pathName)).getStr(pathName);
    }

    public JSONObject postData(String uri, Dict map, String tokenId, int secondOut) {
        String cookie = getCookie(tokenId);
        String param = "";
        if (map != null) {
            param = JSONUtil.toJsonStr(map);
        }
        Supplier<JSONObject> supplier = () -> {
            String resstr = HttpUtil.createPost(apiHost + uri)
                    .header("accept", "application/json;charset=UTF-8").cookie(cookie)
                    .form(map)
                    .header("referer", "https://cloud.189.cn/")
                    .execute().body();
            if (StrUtil.isBlank(resstr)) {
                return null;
            }
            JSONObject json = JSONUtil.parseObj(resstr);
            Assert.isFalse(StrUtil.equals(json.getStr("errorCode"), "InvalidSessionKey"), "当前登录已失效,请重新扫码登录");
            return json;
        };
        if (secondOut > 0) {
            return CacheUtil.getCacheData("pan189:postdata:" + SecureUtil.md5(uri + param + tokenId), supplier, secondOut);
        } else {
            return supplier.get();
        }
    }

    @Override
    public CommonBean.Page<CommonBean.PathInfo> listFiles(PlainPath parentPath, String tmpNext) {
        if (StrUtil.isBlank(tmpNext)) {
            tmpNext = "1";
        }
        final int current = Convert.toInt(tmpNext);
        String[] sz = parentPath.getRealPath().split("/");
        String parent_file_id = sz[sz.length - 1];
        String marker = SecureUtil.md5(current + "1");
        String redisKey = "pan189:filelist:" + parentPath.getTokenId() + ":" + parent_file_id + ":" + marker;
        CommonBean.Page<CommonBean.PathInfo> page = CacheUtil.getCacheData(redisKey, () -> {
            int pageSize = 60;
            JSONObject res = postData("/open/file/listFiles.action?pageSize=" + pageSize + "&pageNum=" + current + "&mediaType=0&folderId=" + parent_file_id + "&iconOption=5&orderBy=lastOpTime&descending=true", null,
                    parentPath.getTokenId()
                    , 0);
            CommonBean.Page<CommonBean.PathInfo> page1 = new CommonBean.Page<>();
            JSONObject fileListAO = res.getJSONObject("fileListAO");
            int count = fileListAO.getInt("count");
            int pages = count / pageSize;
            if (count % pageSize != 0) {
                pages++;
            }
            if (pages > current) {
                page1.setNext(Convert.toStr(current + 1));
            } else {
                page1.setNext("");
            }
            List<CommonBean.PathInfo> list = items2infos(fileListAO.getJSONArray("folderList"), fileListAO.getJSONArray("fileList"));
            page1.setList(list);
            page1.setType(1);
            return page1;
        }, 120);
        return page;
    }

    @Override
    public void refreshToken(String driveType, IoTokenData itd) {
        String resStr = HttpUtil.createGet("https://cloud.189.cn/api/open/user/getUserInfoForPortal.action")
                .header("accept", "application/json;charset=UTF-8")
                .cookie(itd.getTokenData())
                .execute()
                .body();
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.isTrue(res.getInt("res_code", -1) == 0, "当前cookie有误");
        itd.setDriveType(driveType);
        itd.setId(SecureUtil.md5(driveType + res.getStr("loginName")));
        itd.setExpireTime(LocalDateTime.now().plusMinutes(30L));
    }

    private CommonBean.PathInfo file2info(JSONObject item) {
        CommonBean.PathInfo pi = new CommonBean.PathInfo();
        pi.setName(item.getStr("name"));
        pi.setPath(item.getStr("id"));
        pi.setCreatedAt(DateUtil.parse(item.getStr("createDate")).toString());
        pi.setUpdatedAt(DateUtil.parse(item.getStr("lastOpTime")).toString());
        if (StrUtil.equals(item.getStr("type"), "file")) {
            pi.setSize(item.getLong("size"));
            pi.setType(1);
            pi.setMd5(item.getStr("md5"));
            pi.setExt(FileUtil.extName(pi.getName()));
            JSONObject icon = item.getJSONObject("icon");
            if (icon != null) {
                pi.setThumbnail(icon.getStr("smallUrl"));
            }
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
                item.set("type", "folder");
                CommonBean.PathInfo info = file2info(item);
                list.add(info);
            }
        }
        if (fileList != null) {
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject item = fileList.getJSONObject(i);
                item.set("type", "file");
                CommonBean.PathInfo info = file2info(item);
                list.add(info);
            }
        }
        return list;
    }

    private String getCookie(String tokenId) {
        IoTokenData itd = TokenDataUtil.getTokenDataByIdOrToken(FileSystemUtil.PAN189_DRIVE_TYPE, tokenId);
        return itd.getTokenData();
    }

    @Override
    public String remove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes) {
        return copyOrMoveDelete(sourceParent, sourceChildren, sourceTypes, null, "DELETE", null);
    }

    @Override
    public String pathName(PlainPath plainPath) {
        String[] sz = plainPath.getRealPath().split("/");
        String fileId = sz[sz.length - 1];
        JSONObject res = postData("/portal/listFiles.action", Dict.create().set("fileId", fileId), plainPath.getTokenId(), 30);
        JSONArray items = res.getJSONArray("path");
        String[] paths = plainPath.getCipherPath().split("/");
        int length = paths.length;
        for (int i = length - 1; i > 0; i--) {
            int n = items.size() - paths.length + i;
            JSONObject item = items.getJSONObject(n);
            Assert.isTrue(item != null && StrUtil.equals(item.getStr("fileId"), paths[i]), "路径非法");
            paths[i] = item.getStr("fileName");
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
        return null;
    }

    @Override
    public String zip(List<PlainPath> files, PlainPath dirPath) {
        return null;
    }

    @Override
    public CommonBean.PathInfo fileInfo(PlainPath plainPath) {
        String[] sz = plainPath.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        JSONObject item = postData("/open/file/getFileInfo.action", Dict.create().set("fileId", file_id), plainPath.getTokenId(), 0);
        Assert.isTrue(StrUtil.equals(item.getStr("res_code"), "0"), "当前文件(夹)信息不存在,请稍后重试");
        CommonBean.PathInfo pi = new CommonBean.PathInfo();
        pi.setMd5(item.getStr("md5"));
        pi.setName(item.getStr("name"));
        pi.setPath(item.getStr("id"));
        pi.setCreatedAt(DateUtil.parse(item.getStr("createDate")).toString());
        pi.setUpdatedAt(DateUtil.parse(item.getStr("lastOpTimeStr")).toString());
        if (StrUtil.isNotBlank(pi.getMd5())) {
            pi.setSize(item.getLong("size"));
            pi.setType(1);
            pi.setExt(FileUtil.extName(pi.getName()));
            JSONObject icon = item.getJSONObject("icon");
            if (icon != null) {
                pi.setThumbnail(icon.getStr("smallUrl"));
            }
        } else {
            pi.setType(2);
        }
        return pi;
    }

    @Override
    public CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit) {
        CommonBean.WpsUrlData data = new CommonBean.WpsUrlData();
        data.setType(2);
        return data;
    }

    @Override
    public List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name) {
        String[] sz = parentPath.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        JSONObject res = this.postData("/open/file/searchFiles.action", Dict.create()
                        .set("folderId", file_id)
                        .set("fileName", name)
                        .set("pageNum", "1")
                        .set("pageSize", "100")
                , parentPath.getTokenId(), 0);
        JSONArray fileList = res.getJSONArray("fileList");
        JSONArray folderList = res.getJSONArray("folderList");
        return items2infos(folderList, fileList);
    }

    @Override
    public InputStream getInputStream(PlainPath path, long start, long length) {
        String url = downloadUrl(path);
        HttpRequest req = HttpUtil.createGet(url).setMaxRedirectCount(100);
        req.header("Referer", "https://cloud.189.cn/", true);
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
        return "0";
    }

    private JSONObject getPkData(String config) {
        return this.postData("/security/generateRsaKey.action", null, config, 3600);
    }

    private String getSessionKey(String config) {
        JSONObject res = this.postData("/portal/v2/getUserBriefInfo.action", null, config, 3600);
        return res.getStr("sessionKey");
    }

    private JSONObject jmGetData(String uri, Dict data, String config) {
        List<String> list = new ArrayList<>();
        data.forEach((s, o) -> list.add(s + "=" + o.toString()));
        String fStr = CollUtil.join(list, "&");
        long time = System.currentTimeMillis();
        String key = IdUtil.fastSimpleUUID();
        JSONObject pkData = getPkData(config);
        String paramStr = SecureUtil.aes(key.substring(0, 16).getBytes(StandardCharsets.UTF_8)).encryptHex(fStr, CharsetUtil.CHARSET_UTF_8);
        RSA rsa = SecureUtil.rsa(null, pkData.getStr("pubKey"));
        String etx = Base64.encode(rsa.encrypt(key, KeyType.PublicKey));
        String method = "GET";
        String sk = getSessionKey(config);
        String v = "SessionKey=" + sk + "&Operate=" + method + "&RequestURI=" + uri + "&Date=" + time + "&params=" + paramStr;
        String signature = SecureUtil.hmacSha1(key).digestHex(v);
        Map<String, String> headers = new HashMap<>();
        headers.put("EncryptionText", etx);
        headers.put("PkId", pkData.getStr("pkId"));
        headers.put("SessionKey", sk);
        headers.put("Signature", signature);
        headers.put("X-Request-Date", Convert.toStr(time));
        headers.put("X-Request-ID", IdUtil.fastSimpleUUID());
        String resStr = HttpUtil.createGet("https://upload.cloud.189.cn" + uri + "?params=" + paramStr).headerMap(headers, true).execute().body();
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.isTrue(StrUtil.equals(res.getStr("code"), "SUCCESS"), "执行失败{}", res);
        return res;
    }

    @Override
    public String sameDriveCopy(String driveType, CommonBean.TransmissionFile file) {
        return "";
    }

    @Override
    public String sha1(PlainPath path) {
        return null;
    }

    @Override
    public String md5(PlainPath path) {
        return fileInfo(path).getMd5();
    }

    @Override
    public boolean restore(PlainPath path, IoUserRecycle ioUserRecycle) {
        String[] realPaths = path.getRealPath().split("/");
        String fileId = realPaths[realPaths.length - 1];
        String parentId = realPaths[realPaths.length - 2];
        String realPathParent = ArrayUtil.join(ArrayUtil.remove(realPaths, realPaths.length - 1), "/");
        String[] cipherPaths = path.getCipherPath().split("/");
        String cipherPathParent = ArrayUtil.join(ArrayUtil.remove(cipherPaths, cipherPaths.length - 1), "/");
        path.setRealPath(realPathParent);
        path.setCipherPath(cipherPathParent);
        List<String> children = CollUtil.newArrayList(fileId);
        String res = copyOrMoveDelete(path, children, CollUtil.newArrayList(ioUserRecycle.getType()), null, "RESTORE", RestoreFile.init(ioUserRecycle.getName(), ioUserRecycle.getType()));
        boolean flag = StrUtil.equals(res, "1");
        if (flag) {
            this.clearCacheByParent(path.getTokenId(), parentId);
        }
        return flag;
    }

    @Override
    public String getRootId(String driveType) {
        return "-11";
    }

    @Override
    public R commonReq(PlainPath path, Context ctx) {
        return null;
    }
}

package cn.tenfell.webos.common.filesystem;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
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
import org.noear.solon.core.handle.Context;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AliyunDriveFileSystem implements FileSystemInface {
    String apiHost = "https://api.aliyundrive.com";

    @Override
    public String copy(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path) {
        return this.copyOrMove(sourceParent, sourceChildren, path, "copy");
    }

    private String copyOrMove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, FileSystemInface.PlainPath path, String type) {
        JSONObject token = getToken(sourceParent);
        String[] sz = sourceParent.getRealPath().split("/");
        String sourceParentFileId = sz[sz.length - 1];
        sz = path.getRealPath().split("/");
        String targetFileId = sz[sz.length - 1];
        List<Dict> requests = new ArrayList<>();
        for (int i = 0; i < sourceChildren.size(); i++) {
            String fileId = sourceChildren.get(i);
            Dict request = Dict.create().set("body",
                            Dict.create().set("drive_id", token.getStr("default_drive_id"))
                                    .set("file_id", fileId)
                                    .set("to_drive_id", token.getStr("default_drive_id"))
                                    .set("to_parent_file_id", targetFileId))
                    .set("headers", Dict.create().set("Content-Type", "application/json"))
                    .set("id", IdUtil.fastSimpleUUID())
                    .set("method", "POST")
                    .set("url", "/file/" + type);
            requests.add(request);
        }
        Dict param = Dict.create().set("requests", requests).set("resource", "file");
        JSONObject res = this.postData("/v3/batch", sourceParent.getTokenId(), JSONUtil.parseObj(param), 0);
        JSONObject response = res.getJSONArray("responses").getJSONObject(0);
        boolean flag = response.getInt("status") == 200 || response.getInt("status") == 201 || response.getInt("status") == 202;
        if (flag) {
            this.clearCacheByParent(sourceParent.getTokenId(), sourceParentFileId);
            this.clearCacheByParent(path.getTokenId(), targetFileId);
        }
        return flag ? "1" : "0";
    }

    @Override
    public String move(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path) {
        return this.copyOrMove(sourceParent, sourceChildren, path, "move");
    }

    @Override
    public boolean rename(FileSystemInface.PlainPath source, String name, Integer type) {
        JSONObject token = getToken(source);
        String[] sz = source.getRealPath().split("/");
        JSONObject res = this.postData("/v3/file/update", source.getTokenId(), JSONUtil.createObj().set("check_name_mode", "refuse").set("drive_id", token.getStr("default_drive_id")).set("file_id", sz[sz.length - 1]).set("name", name), 0);
        boolean flag = StrUtil.isNotBlank(res.getStr("file_id"));
        if (flag) {
            this.clearCacheByParent(source.getTokenId(), sz[sz.length - 2]);
        }
        return flag;
    }

    private synchronized String getDirFileIdByPath(String tokenId, String parentFileId, String[] dirPaths) {
        JSONObject token = getToken(tokenId);
        for (String dirPath : dirPaths) {
            JSONObject param = JSONUtil.createObj();
            param.set("drive_id", token.getStr("default_drive_id"));
            param.set("parent_file_id", parentFileId);
            param.set("name", dirPath);
            param.set("check_name_mode", "refuse");
            param.set("type", "folder");
            JSONObject res = this.postData("/adrive/v2/file/createWithFolders", tokenId, param, 60);
            parentFileId = res.getStr("file_id");
        }
        return parentFileId;
    }

    @Override
    public String uploadPre(FileSystemInface.PlainPath path, String name, String expand) {
        JSONObject token = getToken(path);
        String[] sz = path.getRealPath().split("/");
        JSONObject data = JSONUtil.parseObj(expand);
        int type = data.getInt("type");
        if (type == 1) {
            //获取big integer
            return SecureUtil.md5(token.getStr("access_token")).substring(0, 16);
        } else if (type == 2 || type == 3) {
            //判断创建文件并判断文件是否存在
            JSONObject postData = data.getJSONObject("postData");
            postData.set("drive_id", token.getStr("default_drive_id"));
            postData.set("parent_file_id", sz[sz.length - 1]);
            postData.set("name", name);
            String[] pathSz = name.split("/");
            if (pathSz.length > 1) {
                String fileName = pathSz[pathSz.length - 1];
                String fileId = this.getDirFileIdByPath(path.getTokenId(), postData.getStr("parent_file_id"), ArrayUtil.remove(pathSz, pathSz.length - 1));
                postData.set("parent_file_id", fileId);
                postData.set("name", fileName);
            }
            JSONObject res = this.postData("/adrive/v2/file/createWithFolders", path.getTokenId(), postData, 0);
            JSONObject resData = JSONUtil.createObj();
            if (res.getBool("rapid_upload", false)) {
                resData.set("rapid_upload", true);
                resData.set("file_id", res.getStr("file_id"));
                this.clearCacheByParent(path.getTokenId(), postData.getStr("parent_file_id"));
                return resData.toString();
            }
            if (StrUtil.equals(res.getStr("code"), "PreHashMatched")) {
                //preHash发生碰撞
                resData.set("rapid_upload", false);
                resData.set("pre_hash", true);
                return resData.toString();
            }
            resData.set("pre_hash", false);
            resData.set("rapid_upload", false);
            resData.set("part_info_list", res.get("part_info_list"));
            resData.set("upload_id", res.get("upload_id"));
            resData.set("file_id", res.get("file_id"));
            return resData.toString();
        } else {
            return "";
        }
    }

    private void clearCacheByParent(String config, String parent_file_id) {
        String key = "aliyun:filelist:" + config + ":" + parent_file_id + ":*";
        CacheUtil.delCacheData(key);
    }

    @Override
    public String uploadUrl(FileSystemInface.PlainPath path, String name, String expand) {
        JSONObject token = getToken(path);
        JSONObject param = JSONUtil.parseObj(expand);
        JSONObject res = this.postData("/v2/file/get_upload_url", path.getTokenId(), param.set("drive_id", token.getStr("default_drive_id")), 0);
        return res.getJSONArray("part_info_list").toString();
    }

    @Override
    public String uploadAfter(FileSystemInface.PlainPath path, String name, String expand) {
        JSONObject token = getToken(path);
        JSONObject param = JSONUtil.parseObj(expand);
        JSONObject res = this.postData("/v2/file/complete", path.getTokenId(), param.set("drive_id", token.getStr("default_drive_id")), 0);
        String file_id = res.getStr("file_id");
        String[] pathSz = name.split("/");
        String flag = StrUtil.isNotBlank(file_id) ? "1" : "0";
        if (StrUtil.equals(flag, "1")) {
            String[] sz = path.getRealPath().split("/");
            String parent_file_id = sz[sz.length - 1];
            if (pathSz.length > 1) {
                parent_file_id = this.getDirFileIdByPath(path.getTokenId(), parent_file_id, ArrayUtil.remove(pathSz, pathSz.length - 1));
            }
            this.clearCacheByParent(path.getTokenId(), parent_file_id);
        }
        return flag;
    }

    private interface SubBytesInterface {
        byte[] getSubBytes(long start, long length);

        long fileLength();
    }

    private String proof_code(String token, SubBytesInterface sub) {
        String n = token;
        BigInteger r = HexUtil.toBigInteger(SecureUtil.md5(n).substring(0, 16));
        BigInteger i = BigInteger.valueOf(sub.fileLength());
        BigInteger o = i.longValue() > 0 ? r.mod(i) : BigInteger.ZERO;
        long start = o.longValue();
        long end = NumberUtil.min(o.add(BigInteger.valueOf(8L)).longValue(), i.longValue());
        int len = Convert.toInt(end - start);
        byte[] b = sub.getSubBytes(start, len);
        return Base64.encode(b);
    }

    private void putFile(String url, InputStream in, long start, Consumer<Long> consumer) {
        OutputStream out = null;
        try {
            HttpConnection connection = HttpConnection.create(url, null).setConnectTimeout(HttpGlobalConfig.getTimeout()).setReadTimeout(HttpGlobalConfig.getTimeout()).setInstanceFollowRedirects(false).setChunkedStreamingMode(8192).setMethod(Method.PUT).header("Referer", "https://www.aliyundrive.com/", true).header("Accept", "*/*", true).header("Connection", "keep-alive", true).header("Content-Type", "", true);
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

    private JSONObject uploadPreCommon(int type, File file, JSONObject token, PlainPath path, String name, long urls) {
        if (type == 2) {
            return uploadPreCommonType(2, file, null, null, token, path, name, urls);
        } else {
            JSONObject data = uploadPreCommonType(3, null, new SubBytesInterface() {
                @Override
                public byte[] getSubBytes(long start, long length) {
                    InputStream fp = new ShardingInputStream(FileUtil.getInputStream(file), start, length);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        IoUtil.copy(fp, baos);
                    } finally {
                        IoUtil.close(fp);
                    }
                    return baos.toByteArray();
                }

                @Override
                public long fileLength() {
                    return file.length();
                }
            }, null, token, path, name, urls);
            if (data.getBool("rapid_upload")) {
                return data;
            }
            if (data.getBool("pre_hash")) {
                return uploadPreCommonType(2, file, null, null, token, path, name, urls);
            }
            return data;
        }
    }

    private JSONObject uploadPreCommonType(int type, File file, SubBytesInterface subData, String hash, JSONObject token, PlainPath path, String name, long urls) {
        //type  为2则使用file  为3则使用subData  4为subData,hash
        long fileSize;
        if (type == 2) {
            fileSize = file.length();
        } else {
            fileSize = subData.fileLength();
        }
        Dict postData = Dict.create().set("check_name_mode", "overwrite").set("drive_id", "").set("parent_file_id", "").set("size", fileSize).set("type", "file");
        if (type == 2 || type == 4) {
            //完整模式
            if (type == 2) {
                InputStream fis = FileUtil.getInputStream(file);
                hash = SecureUtil.sha1(fis).toUpperCase();
                IoUtil.close(fis);
                subData = new SubBytesInterface() {
                    @Override
                    public byte[] getSubBytes(long start, long length) {
                        InputStream fp = new ShardingInputStream(FileUtil.getInputStream(file), start, length);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            IoUtil.copy(fp, baos);
                        } finally {
                            IoUtil.close(fp);
                        }

                        return baos.toByteArray();
                    }

                    @Override
                    public long fileLength() {
                        return file.length();
                    }
                };
            }
            String proof_code = this.proof_code(token.getStr("access_token"), subData);
            postData.set("content_hash", hash).set("content_hash_name", "sha1").set("proof_version", "v1").set("proof_code", proof_code);
            type = 2;
        } else {
            //前缀模式
            postData.set("pre_hash", SecureUtil.sha1(new ByteArrayInputStream(subData.getSubBytes(0, 1024))));
        }
        List<Dict> part_info_list = new ArrayList<>();
        for (int i = 1; i <= urls; i++) {
            part_info_list.add(Dict.create().set("part_number", i));
        }
        postData.set("part_info_list", part_info_list);
        String dataStr = uploadPre(path, name, JSONUtil.createObj().set("type", type).set("postData", postData).toString());
        return JSONUtil.parseObj(dataStr);
    }


    @Override
    public String uploadByServer(PlainPath path, String name, InputStream in, File file, Consumer<Long> consumer) {
        //分片上传
        JSONObject token = getToken(path);
        boolean needDel = true;
        if (file != null) {
            needDel = false;
        } else {
            file = FileUtil.writeFromStream(in, ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".cache");
        }
        try {
            long fileSize = file.length();
            long fpSize = 1024 * 1024 * 10;
            long fps = fileSize / fpSize;
            if (fileSize % fpSize != 0) {
                fps++;
            }
            int type = 2;//全量计算
            if (fps > 1) {
                type = 3;//前缀计算
            }
            long urls = 20;
            if (fps < urls) {
                urls = fps;
            }
            JSONObject uploadPreRes = uploadPreCommon(type, file, token, path, name, urls);
            if (uploadPreRes.getBool("rapid_upload")) {
                return uploadPreRes.getStr("file_id");
            }
            JSONArray list = uploadPreRes.getJSONArray("part_info_list");
            Map<Integer, String> urlMap = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject tmp = list.getJSONObject(i);
                urlMap.put(tmp.getInt("part_number"), tmp.getStr("upload_url"));
            }
            for (int i = 0; i < fps; i++) {
                if (consumer != null) {
                    consumer.accept((long) i * fpSize);
                }
                long start = i * fpSize;
                long length = fpSize;
                if (fileSize - start < length) {
                    length = fileSize - start;
                }
                InputStream part = new ShardingInputStream(FileUtil.getInputStream(file), start, length);
                String url = urlMap.get(i + 1);
                if (StrUtil.isBlank(url)) {
                    Dict data = Dict.create().set("file_id", uploadPreRes.getStr("file_id")).set("upload_id", uploadPreRes.getStr("upload_id"));
                    long count = 20;
                    if (fps - i < count) {
                        count = fps - i;
                    }
                    List<Dict> part_info_list = new ArrayList<>();
                    for (int m = 1; m <= count; m++) {
                        part_info_list.add(Dict.create().set("part_number", m + i));
                    }
                    data.set("part_info_list", part_info_list);
                    String urlDataStr = uploadUrl(path, name, JSONUtil.toJsonStr(data));
                    JSONArray urlData = JSONUtil.parseArray(urlDataStr);
                    for (int j = 0; j < urlData.size(); j++) {
                        JSONObject tmp = urlData.getJSONObject(j);
                        urlMap.put(tmp.getInt("part_number"), tmp.getStr("upload_url"));
                    }
                    url = urlMap.get(i + 1);
                }
                putFile(url, part, (long) i * fpSize, consumer);
            }
            String flag = uploadAfter(path, name, JSONUtil.createObj().set("file_id", uploadPreRes.getStr("file_id")).set("upload_id", uploadPreRes.getStr("upload_id")).toString());
            if (StrUtil.equals(flag, "1")) {
                if (consumer != null) {
                    consumer.accept(fileSize);
                }
                return uploadPreRes.getStr("file_id");
            }
            return "";
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
        } finally {
            if (needDel) {
                FileUtil.del(file);
            }

        }
        return "";
    }

    @Override
    public String downloadUrl(FileSystemInface.PlainPath path) {
        AliyunDriveFileSystem that = this;
        JSONObject token = getToken(path);
        String[] sz = path.getRealPath().split("/");
        String fileId = sz[sz.length - 1];
        return CacheUtil.getCacheData("aliyun:downurl:" + fileId, () -> {
            JSONObject res = that.postData("/v2/file/get_download_url", path.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", sz[sz.length - 1]).set("url_expire_sec", 14500), 0);
            if (StrUtil.isNotBlank(res.getStr("url"))) {
                return res.getStr("url");
            }
            return null;
        }, 120);
    }

    private JSONObject getToken(FileSystemInface.PlainPath parentPath) {
        return getToken(parentPath.getTokenId());
    }

    public JSONObject getToken(String tokenId) {
        IoTokenData data = TokenDataUtil.getTokenDataByIdOrToken(FileSystemUtil.ALI_PAN_DRIVE_TYPE, tokenId);
        return JSONUtil.parseObj(data.getTokenData());
    }


    @Override
    public String createDir(FileSystemInface.PlainPath parentPath, String pathName) {
        JSONObject token = getToken(parentPath);
        String[] sz = parentPath.getRealPath().split("/");
        JSONObject res = this.postData("/adrive/v2/file/createWithFolders", parentPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("parent_file_id", sz[sz.length - 1]).set("name", pathName).set("check_name_mode", "refuse").set("type", "folder"), 0);
        String file_id = res.getStr("file_id");
        if (StrUtil.isNotBlank(file_id)) {
            this.clearCacheByParent(parentPath.getTokenId(), sz[sz.length - 1]);
            return file_id;
        }
        return null;
    }

    private JSONObject postData(String uri, String tokenId, JSONObject param, int secondOut) {
        JSONObject token = getToken(tokenId);
        Supplier<JSONObject> supplier = () -> {
            String resstr = HttpUtil.createPost(apiHost + uri).header("referer", "https://www.aliyundrive.com/").header("authorization", token.getStr("access_token")).header("origin", "https://www.aliyundrive.com").body(param.toString()).execute().body();
            if (StrUtil.isBlank(resstr)) {
                return null;
            }
            JSONObject json = JSONUtil.parseObj(resstr);
            return json;
        };
        if (secondOut > 0) {
            return CacheUtil.getCacheData("aliyun:postdata:" + SecureUtil.md5(uri + param.toString() + tokenId), supplier, secondOut);
        } else {
            return supplier.get();
        }
    }

    @Override
    public CommonBean.Page<CommonBean.PathInfo> listFiles(FileSystemInface.PlainPath parentPath, String tmpNext) {
        AliyunDriveFileSystem that = this;
        if (tmpNext == null) {
            tmpNext = "";
        }
        final String next = tmpNext;
        String[] sz = parentPath.getRealPath().split("/");
        String parent_file_id = sz[sz.length - 1];
        String marker = SecureUtil.md5(next + "1");
        String redisKey = "aliyun:filelist:" + parentPath.getTokenId() + ":" + parent_file_id + ":" + marker;
        CommonBean.Page<CommonBean.PathInfo> page = CacheUtil.getCacheData(redisKey, () -> {
            JSONObject token = getToken(parentPath);
            JSONObject res = that.postData("/adrive/v3/file/list?jsonmask=next_marker%2Citems(name%2Cfile_id%2Cdrive_id%2Ctype%2Csize%2Ccreated_at%2Cupdated_at%2Ccategory%2Cfile_extension%2Cparent_file_id%2Cmime_type%2Cstarred%2Cthumbnail%2Curl%2Cstreams_info%2Ccontent_hash%2Cuser_tags%2Cuser_meta%2Ctrashed%2Cvideo_media_metadata%2Cvideo_preview_metadata%2Csync_meta%2Csync_device_flag%2Csync_flag%2Cpunish_flag)", parentPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("all", false).set("fields", "*").set("image_thumbnail_process", "image/resize,w_400/format,jpeg").set("image_url_process", "image/resize,w_1920/format,jpeg").set("video_thumbnail_process", "video/snapshot,t_1000,f_jpg,ar_auto,w_300").set("limit", 50).set("order_by", "updated_at").set("order_direction", "DESC").set("url_expire_sec", 14400).set("marker", next).set("parent_file_id", parent_file_id), 0);
            CommonBean.Page<CommonBean.PathInfo> page1 = new CommonBean.Page<>();
            if (res.getJSONArray("items") != null) {
                List<CommonBean.PathInfo> list = items2infos(res.getJSONArray("items"));
                page1.setList(list);
            }
            page1.setType(1);
            page1.setNext(res.getStr("next_marker"));
            return page1;
        }, 120);
        return page;
    }

    @Override
    public void refreshToken(String driveType, IoTokenData itd) {
        boolean isJson = JSONUtil.isTypeJSON(itd.getTokenData());
        String refresh_token;
        if (isJson) {
            JSONObject res = JSONUtil.parseObj(itd.getTokenData());
            refresh_token = res.getStr("refresh_token");
        } else {
            refresh_token = itd.getTokenData();
        }
        String resStr = HttpUtil.post("https://auth.aliyundrive.com/v2/account/token", JSONUtil.createObj()
                .set("refresh_token", refresh_token)
                .set("grant_type", "refresh_token").toString());
        JSONObject res = JSONUtil.parseObj(resStr);
        Assert.notBlank(res.getStr("user_id"), "当前token有误");
        itd.setTokenData(resStr);
        itd.setDriveType(driveType);
        itd.setId(SecureUtil.md5(driveType + res.getStr("user_id")));
        itd.setExpireTime(LocalDateTimeUtil.of(DateUtil.parse(res.getStr("expire_time")).toJdkDate()));
    }

    @Override
    public String remove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes) {
        JSONObject token = getToken(sourceParent);
        String realPath = sourceParent.getRealPath();
        String[] sz = realPath.split("/");
        String sourceParentFileId = sz[sz.length - 1];
        List<Dict> requests = new ArrayList<>();
        for (int i = 0; i < sourceChildren.size(); i++) {
            String fileId = sourceChildren.get(i);
            Dict request = Dict.create().set("body",
                            Dict.create()
                                    .set("drive_id", token.getStr("default_drive_id"))
                                    .set("file_id", fileId)
                    ).set("headers", Dict.create().set("Content-Type", "application/json"))
                    .set("id", IdUtil.fastSimpleUUID())
                    .set("method", "POST")
                    .set("url", "/recyclebin/trash");
            requests.add(request);
        }
        Dict param = Dict.create().set("requests", requests).set("resource", "file");
        JSONObject res = this.postData("/v2/batch", sourceParent.getTokenId(), JSONUtil.parseObj(param), 0);
        JSONObject response = res.getJSONArray("responses").getJSONObject(0);
        boolean flag = response.getInt("status") == 200 || response.getInt("status") == 201 || response.getInt("status") == 202 || response.getInt("status") == 204;
        if (flag) {
            this.clearCacheByParent(sourceParent.getTokenId(), sourceParentFileId);
        }
        return flag ? "1" : "0";
    }

    @Override
    public String pathName(PlainPath plainPath) {
        JSONObject token = getToken(plainPath);
        String[] sz = plainPath.getRealPath().split("/");
        String fileId = sz[sz.length - 1];
        JSONObject res = this.postData("/adrive/v1/file/get_path", plainPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", fileId), 30);
        JSONArray items = res.getJSONArray("items");
        String[] paths = plainPath.getCipherPath().split("/");
        int length = paths.length;
        for (int i = length - 1; i > 0; i--) {
            JSONObject item = items.getJSONObject(length - i - 1);
            Assert.isTrue(item != null && StrUtil.equals(item.getStr("file_id"), paths[i]), "路径非法");
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

    private List<CommonBean.PathInfo> items2infos(JSONArray items) {
        List<CommonBean.PathInfo> list = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            CommonBean.PathInfo info = file2info(item);
            list.add(info);
        }
        return list;
    }

    @Override
    public List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name) {
        JSONObject token = getToken(parentPath);
        String[] sz = parentPath.getRealPath().split("/");
        String parentFileId = sz[sz.length - 1];
        JSONObject res = this.postData("/adrive/v3/file/search", parentPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("limit", 100).set("order_by", "name ASC").set("query", "parent_file_id = \"" + parentFileId + "\" and (name = \"" + name + "\")"), 0);
        JSONArray items = res.getJSONArray("items");
        return items2infos(items);
    }

    @Override
    public InputStream getInputStream(PlainPath path, long start, long length) {
        String url = downloadUrl(path);
        HttpRequest req = HttpUtil.createGet(url);
        req.header("Referer", "https://www.aliyundrive.com/", true);
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
        JSONObject token = getToken(path);
        FileSystemInface.PlainPath source = FileSystemUtil.cipherPath2PlainPathByLogin(sourceCipherPath, "", "");
        JSONObject data = uploadPreCommonType(3, null, new SubBytesInterface() {
            @Override
            public byte[] getSubBytes(long start, long length) {
                InputStream in = FileSystemUtil.ACTION.getInputStream(source, start, length);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    IoUtil.copy(in, baos);
                } finally {
                    IoUtil.close(in);
                }

                return baos.toByteArray();
            }

            @Override
            public long fileLength() {
                return size;
            }
        }, null, token, path, name, 1);
        if (data.getBool("rapid_upload")) {
            //系统存在就秒传成功
            String[] sz = path.getRealPath().split("/");
            this.clearCacheByParent(path.getTokenId(), sz[sz.length - 1]);
            return "1";
        }
        if (data.getBool("pre_hash")) {
            //出现hash碰撞就走完整hash模式
            String hash = FileSystemUtil.ACTION.sha1(source);
            if (StrUtil.isBlank(hash)) {
                return "0";
            }
            data = uploadPreCommonType(4, null, new SubBytesInterface() {
                @Override
                public byte[] getSubBytes(long start, long length) {
                    InputStream in = FileSystemUtil.ACTION.getInputStream(source, start, length);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        IoUtil.copy(in, baos);
                    } finally {
                        IoUtil.close(in);
                    }

                    return baos.toByteArray();
                }

                @Override
                public long fileLength() {
                    return size;
                }
            }, hash, token, path, name, 1);
            if (data.getBool("rapid_upload")) {
                String[] sz = path.getRealPath().split("/");
                this.clearCacheByParent(path.getTokenId(), sz[sz.length - 1]);
                return "1";
            }
        }
        return "0";
    }

    @Override
    public String sameDriveCopy(String driveType, CommonBean.TransmissionFile file) {
        //同盘不同账号,注意token串用问题
        FileSystemInface.PlainPath source = FileSystemUtil.cipherPath2PlainPathByLogin(file.getSource(), "", "");
        CommonBean.PathInfo fileInfo = fileInfo(source);
        String hash = fileInfo.getHash();
        FileSystemInface.PlainPath path = FileSystemUtil.cipherPath2PlainPathByLogin(file.getPath(), "", "");
        JSONObject token = getToken(path);
        JSONObject data = uploadPreCommonType(4, null, new SubBytesInterface() {
            @Override
            public byte[] getSubBytes(long start, long length) {
                InputStream in = FileSystemUtil.ACTION.getInputStream(source, start, length);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    IoUtil.copy(in, baos);
                } finally {
                    IoUtil.close(in);
                }
                return baos.toByteArray();
            }

            @Override
            public long fileLength() {
                return fileInfo.getSize();
            }
        }, hash, token, path, file.getName(), 1);
        if (data.getBool("rapid_upload")) {
            String[] sz = path.getRealPath().split("/");
            this.clearCacheByParent(path.getTokenId(), sz[sz.length - 1]);
            return data.getStr("file_id");
        }
        return null;
    }

    @Override
    public String sha1(PlainPath path) {
        CommonBean.PathInfo info = fileInfo(path);
        if (info != null) {
            return info.getHash();
        }
        return null;
    }

    @Override
    public String md5(PlainPath path) {
        return null;
    }

    @Override
    public boolean restore(PlainPath path, IoUserRecycle ioUserRecycle) {
        JSONObject token = getToken(path);
        String[] sz = path.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        postData("/v2/recyclebin/restore",
                path.getTokenId(),
                JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", file_id)
                , 0
        );
        this.clearCacheByParent(path.getTokenId(), sz[sz.length - 2]);
        return true;
    }

    @Override
    public String getRootId(String driveType) {
        return "root";
    }

    @Override
    public R commonReq(PlainPath path, Context ctx) {
        return null;
    }

    @Override
    public String unzip(PlainPath file) {
        JSONObject token = getToken(file);
        String[] sz = file.getRealPath().split("/");
        String resStatus = "0";
        JSONObject res = this.postData("/v2/archive/uncompress", file.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", sz[sz.length - 1]).set("target_drive_id", token.getStr("default_drive_id")).set("target_file_id", sz[sz.length - 2]).set("domain_id", "").set("archive_type", "zip"), 0);
        if (StrUtil.equals(res.getStr("state"), "Succeed")) {
            resStatus = "1";
        }
        if (StrUtil.equals(resStatus, "0") && StrUtil.equals(res.getStr("state"), "Running")) {
            String task_id = res.getStr("task_id");
            while (true) {
                ThreadUtil.sleep(2000);
                JSONObject statusRes = this.postData("/v2/archive/status", file.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", sz[sz.length - 1]).set("domain_id", "").set("archive_type", "zip").set("task_id", task_id), 0);
                if (StrUtil.equals(statusRes.getStr("state"), "Succeed")) {
                    resStatus = "1";
                    break;
                }
                if (!StrUtil.equals(statusRes.getStr("state"), "Running")) {
                    break;
                }
            }
        }
        if (StrUtil.equals(resStatus, "1")) {
            //刷新目录
            this.clearCacheByParent(file.getTokenId(), sz[sz.length - 2]);
        }
        return resStatus;
    }

    private CommonBean.PathInfo file2info(JSONObject item) {
        CommonBean.PathInfo pi = new CommonBean.PathInfo();
        pi.setName(item.getStr("name"));
        pi.setPath(item.getStr("file_id"));
        pi.setCreatedAt(DateUtil.parse(item.getStr("created_at")).toString());
        pi.setUpdatedAt(DateUtil.parse(item.getStr("updated_at")).toString());
        if (StrUtil.equals(item.getStr("type"), "file")) {
            pi.setSize(item.getLong("size"));
            pi.setType(1);
            pi.setThumbnail(item.getStr("thumbnail"));
            pi.setExt(item.getStr("file_extension"));
            pi.setHash(item.getStr("content_hash"));
        } else {
            pi.setType(2);
        }
        return pi;
    }

    @Override
    public String zip(List<PlainPath> files, PlainPath dirPath) {
        return "2";
    }

    @Override
    public CommonBean.PathInfo fileInfo(PlainPath plainPath) {
        JSONObject token = getToken(plainPath);
        String[] sz = plainPath.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        JSONObject res = this.postData("/v2/file/get", plainPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", file_id), 0);
        return file2info(res);
    }

    @Override
    public CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit) {
        CommonBean.WpsUrlData data = new CommonBean.WpsUrlData();
        JSONObject token = getToken(plainPath);
        String[] sz = plainPath.getRealPath().split("/");
        String file_id = sz[sz.length - 1];
        if (isEdit) {
            JSONObject res = this.postData("/v2/file/get_office_edit_url", plainPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", file_id).set("option", Dict.create()
                    .set("readonly", false)
            ), 0);
            String edit_url = res.getStr("edit_url");
            String office_access_token = res.getStr("office_access_token");
            if (StrUtil.isNotBlank(edit_url) && StrUtil.isNotBlank(office_access_token)) {
                data.setUrl(edit_url);
                data.setToken(office_access_token);
                data.setEdit(true);
                data.setType(1);
                return data;
            }
        }
        JSONObject res = this.postData("/v2/file/get_office_preview_url", plainPath.getTokenId(), JSONUtil.createObj().set("drive_id", token.getStr("default_drive_id")).set("file_id", file_id), 0);
        String preview_url = res.getStr("preview_url");
        String access_token = res.getStr("access_token");
        if (StrUtil.isNotBlank(preview_url) && StrUtil.isNotBlank(access_token)) {
            data.setUrl(preview_url);
            data.setToken(access_token);
            data.setEdit(false);
            data.setType(1);
            return data;
        }
        data.setType(0);
        return data;
    }
    /**
     * 阿里云解压能力
     * https://api.aliyundrive.com/v2/archive/list
     * {"drive_id":"328680","domain_id":"","file_id":"631f42baadd101e24731456f948224578077a655","archive_type":"zip"}
     * {state: "Running", file_list: {}, task_id: "28adbda51de557529b92373380b67fa2"}
     *
     * https://api.aliyundrive.com/v2/archive/status
     * {"drive_id":"328680","domain_id":"","file_id":"631f42baadd101e24731456f948224578077a655","task_id":"28adbda51de557529b92373380b67fa2"}
     * {"state":"Running","file_list":{},"task_id":"28adbda51de557529b92373380b67fa2","progress":20}
     * {"state":"Succeed","file_list":{"font-awesome-4.7.0":{"is_folder":true,"items":[{"is_folder":true,"items":[{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/fonts/FontAwesome.otf","size":134808,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/fonts/fontawesome-webfont.svg","size":444379,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/fonts/fontawesome-webfont.woff","size":98024,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/fonts/fontawesome-webfont.woff2","size":77160,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/fonts/fontawesome-webfont.eot","size":165742,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/fonts/fontawesome-webfont.ttf","size":165548,"updated_at":"2022-09-12T14:31:41.000Z"}],"name":"font-awesome-4.7.0/fonts","size":0,"updated_at":"1970-01-01T00:00:00.000Z"},{"is_folder":true,"items":[{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/list.less","size":377,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/rotated-flipped.less","size":622,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/mixins.less","size":1603,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/variables.less","size":22563,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/core.less","size":452,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/animated.less","size":713,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/bordered-pulled.less","size":585,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/fixed-width.less","size":119,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/icons.less","size":49712,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/screen-reader.less","size":118,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/font-awesome.less","size":495,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/larger.less","size":370,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/path.less","size":771,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/less/stacked.less","size":476,"updated_at":"2022-09-12T14:31:41.000Z"}],"name":"font-awesome-4.7.0/less","size":0,"updated_at":"1970-01-01T00:00:00.000Z"},{"is_folder":true,"items":[{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_path.scss","size":783,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_stacked.scss","size":482,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_core.scss","size":459,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_variables.scss","size":22644,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_animated.scss","size":715,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_bordered-pulled.scss","size":592,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_list.scss","size":378,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_larger.scss","size":375,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_rotated-flipped.scss","size":672,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_fixed-width.scss","size":120,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_icons.scss","size":50498,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_mixins.scss","size":1637,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/_screen-reader.scss","size":134,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/scss/font-awesome.scss","size":430,"updated_at":"2022-09-12T14:31:41.000Z"}],"name":"font-awesome-4.7.0/scss","size":0,"updated_at":"1970-01-01T00:00:00.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/HELP-US-OUT.txt","size":323,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":true,"items":[{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/css/font-awesome.min.css","size":31000,"updated_at":"2022-09-12T14:31:41.000Z"},{"is_folder":false,"items":[],"name":"font-awesome-4.7.0/css/font-awesome.css","size":37414,"updated_at":"2022-09-12T14:31:41.000Z"}],"name":"font-awesome-4.7.0/css","size":0,"updated_at":"1970-01-01T00:00:00.000Z"}],"name":"font-awesome-4.7.0","size":0,"updated_at":"1970-01-01T00:00:00.000Z"}},"task_id":"28adbda51de557529b92373380b67fa2","progress":100}
     *
     * https://api.aliyundrive.com/v2/archive/uncompress
     * {"drive_id":"328680","file_id":"631f42baadd101e24731456f948224578077a655","domain_id":"","target_drive_id":"328680","target_file_id":"6272351d127342e15b084eaaa2bbf17694003eca","archive_type":"zip"}
     * {"state":"Running","task_id":"7b1cdd3a37d9e93fd3163402ae748366"}
     *
     * https://api.aliyundrive.com/v2/archive/status
     * {"drive_id":"328680","domain_id":"","file_id":"631f42baadd101e24731456f948224578077a655","task_id":"7b1cdd3a37d9e93fd3163402ae748366"}
     * {"state":"Running","file_list":{},"task_id":"7b1cdd3a37d9e93fd3163402ae748366","progress":0}
     * {"state":"Succeed","file_list":{},"task_id":"7b1cdd3a37d9e93fd3163402ae748366","progress":100}
     *
     */
}

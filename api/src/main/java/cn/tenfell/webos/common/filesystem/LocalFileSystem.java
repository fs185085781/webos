package cn.tenfell.webos.common.filesystem;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.CacheUtil;
import cn.tenfell.webos.common.util.CommonUtil;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.common.util.ShardingInputStream;
import cn.tenfell.webos.modules.action.UserRecycleAction;
import cn.tenfell.webos.modules.entity.IoTokenData;
import cn.tenfell.webos.modules.entity.IoUserRecycle;
import org.noear.solon.core.handle.Context;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LocalFileSystem implements FileSystemInface {
    private static long nextClearTime = 0;

    @Override
    public String copy(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path) {
        try {
            for (int i = 0; i < sourceChildren.size(); i++) {
                FileUtil.copy(new File(sourceParent.getRealPath() + "/" + sourceChildren.get(i)), new File(path.getRealPath()), true);
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    public String move(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path) {
        try {
            for (int i = 0; i < sourceChildren.size(); i++) {
                FileUtil.move(new File(sourceParent.getRealPath() + "/" + sourceChildren.get(i)), new File(path.getRealPath()), true);
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    public boolean rename(FileSystemInface.PlainPath source, String name, Integer type) {
        try {
            FileUtil.rename(new File(source.getRealPath()), name, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static CommonBean.PathInfo localPathInfo(String fileRealPath, String name, String fileMd5) {
        if (!FileUtil.exist(fileRealPath)) {
            return null;
        }
        CommonBean.PathInfo pi = new CommonBean.PathInfo();
        pi.setName(name);
        pi.setPath(name);
        pi.setType(1);
        pi.setSize(new File(fileRealPath).length());
        String ext = FileUtil.extName(name).toLowerCase();
        pi.setExt(ext);
        pi.setHash(SecureUtil.sha1(new File(fileRealPath)));
        pi.setMd5(fileMd5);
        try {
            BasicFileAttributes basicAttr = Files.readAttributes(Paths.get(fileRealPath), BasicFileAttributes.class);
            FileTime createTime = basicAttr.creationTime();
            pi.setCreatedAt(DateTime.of(createTime.toMillis()).toString());
            FileTime updateTime = basicAttr.lastModifiedTime();
            pi.setUpdatedAt(DateTime.of(updateTime.toMillis()).toString());
        } catch (IOException e) {
        }
        return pi;
    }

    @Override
    public String uploadPre(FileSystemInface.PlainPath path, String name, String expand) {
        JSONObject data = JSONUtil.parseObj(expand);
        JSONObject res = JSONUtil.createObj();
        String fileMd5 = data.getStr("file_hash");
        String pre_hash = data.getStr("pre_hash");
        res.set("currentFp", 0);
        if (StrUtil.isBlank(pre_hash)) {
            pre_hash = IdUtil.fastSimpleUUID();
        } else {
            String filePath = ProjectUtil.rootPath + "/tmpUpload/" + pre_hash;
            if (FileUtil.exist(filePath)) {
                List<String> files = FileUtil.listFileNames(filePath);
                if (files != null) {
                    int max = 0;
                    for (int i = 0; i < files.size(); i++) {
                        int current = Convert.toInt(files.get(i).replace(".data", ""), 0);
                        if (current > max) {
                            max = current;
                        }
                    }
                    //减2是因为文件序号是从1开始,而前端currentFp是从0开始需要减少1
                    //然后最后一个分片无法保证是完整的,不参与计算,需要继续减少1
                    max = max - 2;
                    if (max < 0) {
                        max = 0;
                    }
                    res.set("currentFp", max);
                }
            }
        }
        if (StrUtil.isNotBlank(fileMd5)) {
            String hashFilePath = getRealFileByHash(path.getRealFilePath(), fileMd5);
            if (FileUtil.exist(hashFilePath)) {
                res.set("has", true);
                createWebosFile(fileMd5, path, name);
                return res.toString();
            }
        }
        res.set("has", false);
        res.set("upload_id", pre_hash);
        return res.toString();
    }

    @Override
    public String uploadUrl(FileSystemInface.PlainPath path, String name, String expand) {
        return null;
    }

    @Override
    public String uploadAfter(FileSystemInface.PlainPath path, String name, String expand) {
        JSONObject data = JSONUtil.parseObj(expand);
        String upload_id = data.getStr("upload_id");
        String fileHash = data.getStr("file_hash");
        String tmpPath = ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".data";
        OutputStream os = FileUtil.getOutputStream(tmpPath);
        int fps = data.getInt("fps");
        for (int i = 1; i <= fps; i++) {
            String fpPath = ProjectUtil.rootPath + "/tmpUpload/" + upload_id + "/" + i + ".data";
            FileUtil.writeToStream(fpPath, os);
        }
        IoUtil.close(os);
        String nowHash = SecureUtil.md5(new File(tmpPath));
        String flag = "0";
        if (StrUtil.equals(nowHash, fileHash)) {
            flag = "1";
            removeTmpPathAndCreateWebosFile(tmpPath, path, name);
        } else {
            FileUtil.del(tmpPath);
        }
        FileUtil.del(ProjectUtil.rootPath + "/tmpUpload/" + upload_id);
        return flag;
    }

    /**
     * 将临时文件移动到webos文件并生成预览文件
     * 1.
     *
     * @param tmpPath
     * @param path
     * @param name
     */
    public void removeTmpPathAndCreateWebosFile(String tmpPath, PlainPath path, String name) {
        String fileMd5 = SecureUtil.md5(new File(tmpPath));
        String hashFilePath = getRealFileByHash(path.getRealFilePath(), fileMd5);
        if (FileUtil.exist(hashFilePath)) {
            String fileMd52 = SecureUtil.md5(new File(hashFilePath));
            if (StrUtil.equals(fileMd5, fileMd52)) {
                FileUtil.del(tmpPath);
            } else {
                FileUtil.del(hashFilePath);
                FileUtil.move(new File(tmpPath), new File(hashFilePath), true);
            }
        } else {
            FileUtil.move(new File(tmpPath), new File(hashFilePath), true);
        }
        createWebosFile(fileMd5, path, name);
    }

    private static void createWebosFile(String fileMd5, PlainPath path, String name) {
        String hashFilePath = getRealFileByHash(path.getRealFilePath(), fileMd5);
        if (!FileUtil.exist(hashFilePath)) {
            return;
        }
        String webFilePath = path.getRealPath() + "/" + name;
        name = FileUtil.getName(webFilePath);
        CommonBean.PathInfo info = localPathInfo(hashFilePath, name, fileMd5);
        JSONObject jsonObject = JSONUtil.parseObj(info);
        jsonObject.remove("name");
        jsonObject.remove("path");
        jsonObject.remove("ext");
        FileUtil.writeUtf8String(jsonObject.toString(), webFilePath);
        FileUtil.writeUtf8String(info.getHash(), path.getRealFilePath() + "/webos_files_sha1" + File.separator + info.getHash() + ".webosfile");
    }

    private static String getHashBySha1(PlainPath path, String sha1) {
        String sha1Path = path.getRealFilePath() + "/webos_files_sha1" + File.separator + sha1 + ".webosfile";
        if (!FileUtil.exist(sha1Path)) {
            return null;
        }
        return FileUtil.readUtf8String(sha1Path);
    }

    private static String getRealFileByHash(String realFilePath, String fileMd5) {
        return realFilePath + "/webos_files" + File.separator + fileMd5 + ".webosfile";
    }

    public String realFilePath(String realFilePath, String webosFilePath) {
        return LocalFileSystem.getRealFile(realFilePath, webosFilePath);
    }


    public static String getRealFile(String realFilePath, String webosFilePath) {
        JSONObject res = JSONUtil.parseObj(FileUtil.readUtf8String(webosFilePath));
        String fileMd5 = res.getStr("md5");
        if (StrUtil.isBlank(fileMd5)) {
            return null;
        }
        return getRealFileByHash(realFilePath, fileMd5);
    }

    @Override
    public String uploadByServer(PlainPath path, String name, InputStream in, File file, Consumer<Long> consumer) {
        OutputStream os = null;
        try {
            if (in == null) {
                in = FileUtil.getInputStream(file);
            }
            String tmpPath = ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + ".webosfile";
            os = FileUtil.getOutputStream(tmpPath);
            IoUtil.copy(in, os, NioUtil.DEFAULT_BUFFER_SIZE, new StreamProgress() {
                @Override
                public void start() {
                }

                @Override
                public void progress(long total, long progressSize) {
                    if (consumer != null) {
                        consumer.accept(progressSize);
                    }
                }

                @Override
                public void finish() {
                    if (consumer != null) {
                        consumer.accept(new File(tmpPath).length());
                    }
                }
            });
            IoUtil.close(os);
            removeTmpPathAndCreateWebosFile(tmpPath, path, name);
            return name;
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
        } finally {
            IoUtil.close(in);
            if (os != null) {
                IoUtil.close(os);
            }
        }
        return "";
    }

    @Override
    public String downloadUrl(FileSystemInface.PlainPath path) {
        String realPath = path.getRealPath();
        String[] sz = path.getRealPath().split("/");
        String name = sz[sz.length - 1];
        String tempId = IdUtil.fastSimpleUUID();
        CacheUtil.setValue("file_down:" + tempId, Dict.create().set("name", name).set("realPath", realPath).set("realFilePath", path.getRealFilePath()).set("type", path.getDriveType()), 14400);
        String fileDownUrl = "api?module=fileSystem&action=localFileDown&tempId=" + URLUtil.encodeAll(tempId);
        return fileDownUrl;
    }

    @Override
    public String createDir(FileSystemInface.PlainPath parentPath, String pathName) {
        File file = FileUtil.mkdir(parentPath.getRealPath() + "/" + pathName);
        if (file == null) {
            return null;
        }
        return pathName;
    }

    public CommonBean.PathInfo fileToInfo(File file, String cipherPath) {
        if (!file.exists()) {
            return null;
        }
        CommonBean.PathInfo pi;
        if (file.isFile()) {
            try {
                JSONObject res = JSONUtil.parseObj(FileUtil.readUtf8String(file));
                pi = JSONUtil.toBean(res, CommonBean.PathInfo.class);
                pi.setName(file.getName());
                pi.setPath(file.getName());
                pi.setExt(FileUtil.extName(file.getName()));
                if (CommonUtil.hasThumbnailByExt(pi.getExt())) {
                    String fileViewerUrl = "api?module=fileSystem&action=localFileViewer";
                    String thumbnail = fileViewerUrl + "&ext=" + pi.getExt() + "&path=" + URLUtil.encodeAll(cipherPath);
                    pi.setThumbnail(thumbnail);
                }
            } catch (Exception e) {
                return null;
            }
        } else {
            pi = new CommonBean.PathInfo();
            pi.setName(file.getName());
            pi.setPath(file.getName());
            pi.setType(2);
        }
        try {
            BasicFileAttributes basicAttr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
            FileTime createTime = basicAttr.creationTime();
            pi.setCreatedAt(DateTime.of(createTime.toMillis()).toString());
            FileTime updateTime = basicAttr.lastModifiedTime();
            pi.setUpdatedAt(DateTime.of(updateTime.toMillis()).toString());
        } catch (IOException e) {
        }
        return pi;
    }

    @Override
    public CommonBean.Page<CommonBean.PathInfo> listFiles(FileSystemInface.PlainPath parentPath, String next) {
        File[] files = new File(parentPath.getRealPath()).listFiles();
        CommonBean.Page<CommonBean.PathInfo> page = new CommonBean.Page<>();
        List<CommonBean.PathInfo> list = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                CommonBean.PathInfo info = fileToInfo(file, parentPath.getCipherPath() + "/" + file.getName());
                if (info != null) {
                    list.add(info);
                }
            }
        }
        page.setList(list);
        page.setType(0);
        return page;
    }

    @Override
    public void refreshToken(String driveType, IoTokenData itd) {

    }

    @Override
    public String remove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes) {
        try {
            for (int i = 0; i < sourceChildren.size(); i++) {
                FileUtil.del(new File(sourceParent.getRealPath() + "/" + sourceChildren.get(i)));
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    public String pathName(PlainPath plainPath) {
        String pathName = plainPath.getCipherPath();
        if (pathName.startsWith("/")) {
            pathName = "服务器" + pathName;
        }
        return pathName;
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
            if (!FileUtil.exist(parentPath.getRealPath() + "/" + name)) {
                return tmpMainName;
            }
            index++;
        }
    }

    @Override
    public List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name) {
        if (FileUtil.exist(parentPath.getRealPath() + "/" + name)) {
            CommonBean.PathInfo info = fileToInfo(new File(parentPath.getRealPath() + "/" + name), parentPath.getCipherPath() + "/" + name);
            if (info == null) {
                return null;
            }
            return CollUtil.newArrayList(info);
        }
        return null;
    }

    @Override
    public InputStream getInputStream(PlainPath path, long start, long length) {
        String filePath = realFilePath(path.getRealFilePath(), path.getRealPath());
        InputStream in = FileUtil.getInputStream(filePath);
        if (length == 0) {
            return in;
        }
        return new ShardingInputStream(in, start, length);
    }

    @Override
    public String secondTransmission(PlainPath path, String name, String sourceCipherPath, long size) {
        FileSystemInface.PlainPath source = FileSystemUtil.cipherPath2PlainPathByLogin(sourceCipherPath, "", "");
        String hash = FileSystemUtil.ACTION.md5(source);
        if (StrUtil.isBlank(hash)) {
            String sha1 = FileSystemUtil.ACTION.sha1(source);
            if (StrUtil.isNotBlank(sha1)) {
                hash = getHashBySha1(path, sha1);
            }
        }
        if (StrUtil.isBlank(hash)) {
            return "0";
        }
        String resStr = uploadPre(path, name, JSONUtil.createObj().set("file_hash", hash).toString());
        JSONObject res = JSONUtil.parseObj(resStr);
        return res.getBool("has") ? "1" : "0";
    }

    @Override
    public String sameDriveCopy(String driveType, CommonBean.TransmissionFile file) {
        //不需要实现,本地已经有FileSystemUtil处理过
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
        CommonBean.PathInfo info = fileInfo(path);
        if (info != null) {
            return info.getMd5();
        }
        return null;
    }

    @Override
    public boolean restore(PlainPath path, IoUserRecycle ioUserRecycle) {
        String realPath = UserRecycleAction.id2realpath(ioUserRecycle.getId());
        String dir = FileUtil.getParent(path.getRealPath(), 1);
        boolean flag = false;
        try {
            ZipUtil.unzip(realPath, dir, CharsetUtil.CHARSET_UTF_8);
            flag = true;
        } catch (Exception e) {
            if (StrUtil.equals(e.getMessage(), "MALFORMED")) {
                try {
                    ZipUtil.unzip(realPath, dir, CharsetUtil.CHARSET_GBK);
                    flag = true;
                } catch (Exception e2) {

                }
            }
        }
        if (flag) {
            FileUtil.del(realPath);
        }
        return flag;
    }

    @Override
    public String getRootId(String driveType) {
        if (System.getProperty("os.name").toLowerCase().indexOf("window") != -1) {
            return "C:";
        } else {
            return "/";
        }
    }

    @Override
    public R commonReq(PlainPath path, Context ctx) {
        return null;
    }

    @Override
    public String unzip(PlainPath file) {
        try {
            String[] sz = file.getRealPath().split("/");
            String parent = ArrayUtil.join(ArrayUtil.remove(sz, sz.length - 1), "/");
            String zipPath = realFilePath(file.getRealFilePath(), file.getRealPath());
            String tmpDir = ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID();
            tmpDir = StrUtil.replace(tmpDir, "\\", "/");
            try {
                ZipUtil.unzip(zipPath, tmpDir, CharsetUtil.CHARSET_UTF_8);
            } catch (Exception e) {
                if (StrUtil.equals(e.getMessage(), "MALFORMED")) {
                    ZipUtil.unzip(zipPath, tmpDir, CharsetUtil.CHARSET_GBK);
                } else {
                    return "0";
                }
            }
            List<File> list = FileUtil.loopFiles(tmpDir);
            for (File tmp : list) {
                String webosFilePath = parent + StrUtil.replace(StrUtil.replace(tmp.getAbsolutePath(), "\\", "/"), tmpDir, "");
                file.setRealPath(webosFilePath);
                removeTmpPathAndCreateWebosFile(tmp.getAbsolutePath(), file, "");
            }
            FileUtil.del(tmpDir);
            return "1";
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
            return "0";
        }
    }

    @Override
    public String zip(List<PlainPath> files, PlainPath dirPath) {
        try {
            String ext = "zip";
            String zipMainName = this.availableMainName(dirPath, "压缩文件", ext);
            String tmpPath = ProjectUtil.rootPath + "/tmpUpload/" + IdUtil.fastSimpleUUID() + "." + ext;
            String path = dirPath.getRealPath();
            OutputStream fos = new FileOutputStream(tmpPath);
            List<File> list = new ArrayList<>();
            for (PlainPath tmp : files) {
                String realPath = tmp.getRealPath();
                File obj = new File(realPath);
                if (!obj.exists()) {
                    return "0";
                }
                if (obj.isDirectory()) {
                    List<File> all = FileUtil.loopFiles(obj);
                    if (all != null) {
                        list.addAll(all);
                    }
                } else if (obj.isFile()) {
                    list.add(obj);
                }
            }
            String[] paths = new String[list.size()];
            InputStream[] ins = new InputStream[list.size()];
            for (int i = 0; i < list.size(); i++) {
                String filePath = list.get(i).getAbsolutePath();
                String realPath = realFilePath(dirPath.getRealFilePath(), filePath);
                ins[i] = FileUtil.getInputStream(realPath);
                paths[i] = StrUtil.replace(StrUtil.replace(filePath, "\\", "/"), path + "/", "");
            }
            ZipUtil.zip(fos, paths, ins);
            removeTmpPathAndCreateWebosFile(tmpPath, dirPath, zipMainName + "." + ext);
            return "1";
        } catch (Exception e) {
            ProjectUtil.showConsoleErr(e);
            return "0";
        }
    }

    @Override
    public CommonBean.PathInfo fileInfo(PlainPath plainPath) {
        File file = new File(plainPath.getRealPath());
        return fileToInfo(file, plainPath.getCipherPath());
    }

    @Override
    public CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit) {
        CommonBean.WpsUrlData data = new CommonBean.WpsUrlData();
        data.setType(2);
        return data;
    }

    /**
     * 每隔2小时检查一下过期的上传文件缓存
     */
    public static void clearExpireTmpUploadFile() {
        if (nextClearTime > System.currentTimeMillis()) {
            return;
        }
        String parent = ProjectUtil.rootPath + "/tmpUpload";
        File[] files = new File(parent).listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    BasicFileAttributes basicAttr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
                    FileTime createTime = basicAttr.creationTime();
                    if (System.currentTimeMillis() - createTime.toMillis() > 24 * 60 * 60 * 1000L) {
                        FileUtil.del(file);
                    }
                } catch (Exception e) {
                    ProjectUtil.showConsoleErr(e);
                }
            }
        }
        nextClearTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000;
    }
}

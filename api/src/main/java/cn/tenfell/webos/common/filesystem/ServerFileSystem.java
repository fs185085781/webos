package cn.tenfell.webos.common.filesystem;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.util.CommonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * 服务器文件
 */
public class ServerFileSystem extends LocalFileSystem {
    @Override
    public void removeTmpPathAndCreateWebosFile(String tmpPath, PlainPath path, String name) {
        FileUtil.copy(tmpPath, path.getRealPath() + "/" + name, true);
        FileUtil.del(tmpPath);
    }

    @Override
    public String realFilePath(String realFilePath, String webosRealFilePath) {
        return webosRealFilePath;
    }

    @Override

    public CommonBean.PathInfo fileToInfo(File file, String cipherPath) {
        if (!file.exists()) {
            return null;
        }
        CommonBean.PathInfo pi = new CommonBean.PathInfo();
        if (file.isFile()) {
            pi.setName(file.getName());
            pi.setPath(file.getName());
            pi.setExt(FileUtil.extName(file.getName()));
            pi.setSize(file.length());
            pi.setType(1);
            if (CommonUtil.hasThumbnailByExt(pi.getExt())) {
                String fileViewerUrl = "api?module=fileSystem&action=localFileViewer";
                String thumbnail = fileViewerUrl + "&ext=" + pi.getExt() + "&path=" + URLUtil.encodeAll(cipherPath);
                pi.setThumbnail(thumbnail);
            }
        } else {
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
    public String secondTransmission(PlainPath path, String name, String sourceCipherPath, long size) {
        return "0";
    }

    @Override
    public String sha1(PlainPath path) {
        return SecureUtil.sha1(FileUtil.file(path.getRealPath()));
    }

    @Override
    public String md5(PlainPath path) {
        return SecureUtil.md5(FileUtil.file(path.getRealPath()));
    }
}

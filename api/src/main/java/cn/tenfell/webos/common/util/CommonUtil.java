package cn.tenfell.webos.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.filesystem.LocalFileSystem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {
    private static long nextClearTime = 0;
    private static List<String> imageExt = CollUtil.newArrayList("jpeg", "png", "gif", "bmp", "jpg", "tiff", "svg", "ico");

    public static boolean isImage(String ext) {
        return imageExt.contains(ext.toLowerCase());
    }

    public static String getTbPath(String ext, String path) {
        if (isImage(ext)) {
            String md5 = SecureUtil.md5(path);
            String thumbnail = ProjectUtil.rootPath + "/fileThumbnail" + File.separator + md5 + ".jpg";
            if (FileUtil.exist(thumbnail)) {
                return thumbnail;
            }
            String realPath;
            FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPath(path);
            if (StrUtil.equals(plainPath.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
                realPath = LocalFileSystem.getRealFile(plainPath.getRealFilePath(), plainPath.getRealPath());
            } else if (StrUtil.equals(plainPath.getDriveType(), FileSystemUtil.SERVER_DRIVE_TYPE)) {
                realPath = plainPath.getRealPath();
            } else {
                return "";
            }
            try {
                BufferedImage img = ImgUtil.read(realPath);
                int width = img.getWidth();
                if (width <= 128) {
                    return realPath;
                }
                int height = 128 * img.getHeight() / width;
                Image nimg = ImgUtil.scale(img, 128, height, Color.white);
                ImgUtil.write(nimg, new File(thumbnail));
                return thumbnail;
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static boolean hasThumbnailByExt(String ext) {
        if (isImage(ext)) {
            return true;
        }
        return false;
    }


    public static void clearExpireTmpFile() {
        if (nextClearTime > System.currentTimeMillis()) {
            return;
        }
        String parent = ProjectUtil.rootPath + "/fileThumbnail";
        File[] files = new File(parent).listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    BasicFileAttributes basicAttr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
                    FileTime createTime = basicAttr.creationTime();
                    if (System.currentTimeMillis() - createTime.toMillis() > 2 * 60 * 60 * 1000L) {
                        FileUtil.del(file);
                    }
                } catch (Exception e) {
                    ProjectUtil.showConsoleErr(e);
                }
            }
        }
        nextClearTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000;
    }

    public static String trimPath(String path) {
        path = path.trim();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public static String getParentPath(String path) {
        String[] sz = trimPath(path).split("/");
        sz = ArrayUtil.remove(sz, sz.length - 1);
        String targetPath = ArrayUtil.join(sz, "/");
        if(path.startsWith("/")){
            targetPath = "/"+targetPath;
        }
        return targetPath;
    }

    public static String getLastUrl(String url) {
        url = url.trim();
        HttpResponse resp = HttpUtil.createRequest(Method.HEAD, url).execute();
        if (resp.getStatus() == 302) {
            String newUrl = resp.header("Location");
            if (StrUtil.isBlank(newUrl)) {
                return url;
            }
            newUrl = newUrl.trim();
            if (newUrl.startsWith("//")) {
                String p = url.substring(0, 4);
                String last = url.substring(4, 5);
                if (last.equals(":")) {
                    last = "";
                }
                newUrl = p + last + ":" + newUrl;
            }
            return getLastUrl(newUrl);
        }
        return url;
    }

    public static String replaceFirst(String str, String old, String newString) {
        return Pattern.compile(old, Pattern.LITERAL).matcher(
                str).replaceFirst(Matcher.quoteReplacement(newString));
    }
}

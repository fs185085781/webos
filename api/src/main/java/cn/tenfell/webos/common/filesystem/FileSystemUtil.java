package cn.tenfell.webos.common.filesystem;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.*;
import cn.tenfell.webos.modules.entity.*;
import org.noear.solon.core.handle.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FileSystemUtil implements FileSystemInface {
    final private static Map<String, FileSystemInface> fileSystemMap = new ConcurrentHashMap<>();
    final public static FileSystemUtil ACTION = new FileSystemUtil();
    final public static String LOCAL_DRIVE_TYPE = "local";
    final public static String SERVER_DRIVE_TYPE = "server";
    final public static String ALI_PAN_DRIVE_TYPE = "aliyundrive";
    final public static String PAN123_DRIVE_TYPE = "pan123";
    final public static String PAN189_DRIVE_TYPE = "pan189";
    final public static String KODBOX_DRIVE_TYPE = "kodbox";

    static {
        fileSystemMap.put(ALI_PAN_DRIVE_TYPE, new AliyunDriveFileSystem());
        fileSystemMap.put(PAN123_DRIVE_TYPE, new Pan123FileSystem());
        fileSystemMap.put(LOCAL_DRIVE_TYPE, new LocalFileSystem());
        fileSystemMap.put(SERVER_DRIVE_TYPE, new ServerFileSystem());
        fileSystemMap.put(PAN189_DRIVE_TYPE, new Pan189FileSystem());
        fileSystemMap.put(KODBOX_DRIVE_TYPE, new KodBoxFileSystem());
    }

    public static FileSystemInface.PlainPath cipherPath2PlainPathByUser(String cipherPath, String shareCode, String sharePwd, SysUser user) {
        FileSystemInface.PlainPath plainPath = cipherPath2PlainPath(cipherPath);//权限验证
        if (user != null && user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            return plainPath;
        }
        boolean hasAuth = false;
        if (StrUtil.isNotBlank(plainPath.getSioNo())) {
            //当前是sio 校验分享编码和密码,失败校验当前登录用户
            ShareFile sf = getShareFileByNo(plainPath.getSioNo());
            String errMsg = "";
            int authCount = 0;
            if (!StrUtil.equals(sf.getCode(), shareCode)) {
                if (StrUtil.isBlank(errMsg)) {
                    errMsg = "此分享不存在";
                }
            } else {
                authCount++;
            }
            if (StrUtil.isNotBlank(sf.getPassword()) && !StrUtil.equals(sharePwd, sf.getPassword())) {
                if (StrUtil.isBlank(errMsg)) {
                    errMsg = "权限不足,请刷新后重试";
                }
            } else {
                authCount++;
            }
            if (sf.getExpireTime() != null && LocalDate.now().isAfter(sf.getExpireTime())) {
                if (StrUtil.isBlank(errMsg)) {
                    errMsg = "你来太晚了,当前分享已过期";
                }
            } else {
                authCount++;
            }
            hasAuth = authCount == 3;
            if (hasAuth) {
                String[] fsz = cipherPath.split("/");
                if (fsz.length > 1) {
                    String[] files = sf.getFiles().split(";");
                    Assert.isTrue(ArrayUtil.contains(files, fsz[1]), "权限不足");
                }
            }
            if (!hasAuth) {
                if (user != null) {
                    hasAuth = StrUtil.equals(sf.getUserId(), user.getId());
                }
            }
            Assert.isTrue(hasAuth, StrUtil.isNotBlank(errMsg) ? errMsg : "权限不足,请刷新后重试");
        } else if (StrUtil.isNotBlank(plainPath.getUioNo())) {
            //当前是uio 校验当前登录用户
            IoUserDrive iud = getIoUserDriveByNo(plainPath.getUioNo());
            if (user != null) {
                if (user.getUserType() == 1) {
                    hasAuth = StrUtil.equals(iud.getParentUserId(), user.getId());
                } else {
                    hasAuth = StrUtil.equals(iud.getUserId(), user.getId());
                }
            }
            Assert.isTrue(hasAuth, "权限不足,请刷新后重试");
        } else {
            //当前是io 校验当前登录人
            if (StrUtil.isBlank(plainPath.getIoNo()) && user.getIsAdmin() == 1) {
                return plainPath;
            }
            IoDrive id = getIoDriveByNo(plainPath.getIoNo());
            if (user != null) {
                if (user.getUserType() == 1) {
                    hasAuth = StrUtil.equals(id.getParentUserId(), user.getId());
                }
            }
            Assert.isTrue(hasAuth, "权限不足,请刷新后重试");
        }
        return plainPath;
    }

    /**
     * 密文地址转明文地址
     *
     * @param cipherPath 密文地址 {io:1} {uio:1}这种
     * @param shareCode  分享的code
     * @param sharePwd   分享的密码
     * @return
     */
    public static FileSystemInface.PlainPath cipherPath2PlainPathByLogin(String cipherPath, String shareCode, String sharePwd) {
        return cipherPath2PlainPathByUser(cipherPath, shareCode, sharePwd, LoginAuthUtil.getUser());
    }

    public static ShareFile getShareFileByNo(String sio) {
        ShareFile sf = DbUtil.queryObject("select * from share_file where no = ?", ShareFile.class, sio);
        Assert.notNull(sf, "此分享不存在");
        return sf;
    }

    public static IoUserDrive getIoUserDriveByNo(String uio) {
        IoUserDrive iud = DbUtil.queryObject("select * from io_user_drive where no = ?", IoUserDrive.class, uio);
        Assert.notNull(iud, "此网盘不存在");
        Assert.isTrue(iud.getValid() == 1, "此网盘已被停止使用");
        return iud;
    }

    public static IoDrive getIoDriveByNo(String io) {
        IoDrive id = DbUtil.queryObject("select * from io_drive where no = ?", IoDrive.class, io);
        Assert.notNull(id, "此硬盘不存在");
        return id;
    }

    /**
     * 密文地址转明文地址
     * 将以{io:1},{uio:1},{sio:1}开头的地址
     *
     * @param cipherPath
     * @return
     */
    public static FileSystemInface.PlainPath cipherPath2PlainPath(String cipherPath) {
        //cipherPath 例如 {io:12}/abc {uio:13}/abc {sio:14}/abc
        Assert.notBlank(cipherPath, "地址不可为空");
        cipherPath = StrUtil.replace(cipherPath, "\\", "/");
        if (cipherPath.endsWith(":")) {
            cipherPath = cipherPath + "/";
        }
        String tmpPath = cipherPath;
        FileSystemInface.PlainPath pp = new FileSystemInface.PlainPath();
        if (StrUtil.startWith(tmpPath, "{sio:")) {
            String[] sz = tmpPath.split("/");
            String sioNo = sz[0].split("}")[0].split(":")[1];
            ShareFile sf = getShareFileByNo(sioNo);
            tmpPath = CommonUtil.replaceFirst(tmpPath, sz[0], sf.getPath());
            pp.setSioNo(sioNo);
        }
        if (StrUtil.startWith(tmpPath, "{uio:")) {
            String[] sz = tmpPath.split("/");
            String uioNo = sz[0].split("}")[0].split(":")[1];
            IoUserDrive iud = getIoUserDriveByNo(uioNo);
            Assert.isTrue(iud.getValid() == 1, "此网盘已被禁用");
            tmpPath = CommonUtil.replaceFirst(tmpPath, sz[0], iud.getPath());
            pp.setUioNo(uioNo);
        }
        if (StrUtil.startWith(tmpPath, "{io:")) {
            String[] sz = tmpPath.split("/");
            String ioNo = sz[0].split("}")[0].split(":")[1];
            pp.setIoNo(ioNo);
            IoDrive id = getIoDriveByNo(ioNo);
            String ioPath = StrUtil.replace(id.getPath(), "\\", "/");
            String realPath = CommonUtil.replaceFirst(tmpPath, sz[0], ioPath);
            pp.setTokenId(id.getTokenId());
            pp.setCipherPath(cipherPath);
            pp.setRealPath(StrUtil.replace(new File(realPath).getPath(), "\\", "/"));
            pp.setDriveType(id.getDriveType());
            if (StrUtil.equals(id.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
                if (id.getSecondTransmission() == 1) {
                    //支持秒传,需要真实文件位置
                    pp.setRealFilePath(id.getRealFilePath());
                } else {
                    //不支持秒传,当服务器文件处理
                    pp.setDriveType(FileSystemUtil.SERVER_DRIVE_TYPE);
                }
            }
            return pp;
        } else {
            //系统文件
            if (!FileUtil.exist(tmpPath)) {
                Assert.isTrue(false, "此路径不存在");
            }
            pp.setCipherPath(cipherPath);
            pp.setRealPath(StrUtil.replace(new File(tmpPath).getPath(), "\\", "/"));
            pp.setDriveType(FileSystemUtil.SERVER_DRIVE_TYPE);
            return pp;
        }
    }

    private static FileSystemInface getInfaceByType(String type) {
        FileSystemInface inface = fileSystemMap.get(type);
        Assert.notNull(inface, "当前地址暂时还不支持");
        return inface;
    }

    public static void serverConfirm(String taskId) {
        final CommonBean.CopyMoveFile cmf = CacheUtil.getValue("copymove:task:" + taskId);
        final SysUser user = CacheUtil.getValue("copymove:task-user:" + taskId);
        Assert.notNull(cmf, "当前任务数据不存在");
        Assert.notNull(user, "当前任务未指定用户数据");
        JSONObject exp = JSONUtil.parseObj(cmf.getExp());
        cmf.setExp(null);
        final List<CommonBean.TransmissionFile> files = exp.getBeanList("files", CommonBean.TransmissionFile.class);
        Assert.notEmpty(files, "需要传输的文件不可为空");
        CommonBean.TransmissionFile firstFile = files.get(0);
        cmf.setCurrentFileName("(剩余" + files.size() + "个)" + firstFile.getFileName());
        cmf.setSd(0);
        cmf.setJd(0);
        cmf.setLoaded(0);
        cmf.setSize(firstFile.getSize());
        cmf.setStatus(1);
        CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
        FiberUtil.run(() -> {
            final Supplier<Boolean> isStop = () -> {
                Integer flag = CacheUtil.getValue("copymove:task-stop:" + taskId);
                if (flag != null && flag == 1) {
                    return true;
                }
                return false;
            };
            LoginAuthUtil.USER_LOCAL.set(user);
            final AtomicBoolean hasError = new AtomicBoolean(false);
            int fileIndex = 0;
            int count = files.size();
            int success = 0;
            for (CommonBean.TransmissionFile file : files) {
                if (isStop.get()) {
                    cmf.setCurrentFileName(success + "个文件");
                    cmf.setStatus(3);
                    CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                    LoginAuthUtil.USER_LOCAL.set(null);
                    return null;
                }
                fileIndex++;
                cmf.setCurrentFileName("(剩余" + (files.size() - fileIndex) + "个)" + file.getFileName());
                cmf.setSd(0);
                cmf.setJd(0);
                cmf.setLoaded(0);
                cmf.setSize(file.getSize());
                cmf.setStatus(1);
                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                long currentTime = System.currentTimeMillis();
                PlainPath source1 = FileSystemUtil.cipherPath2PlainPathByUser(file.getSource(), "", "", user);
                PlainPath path1 = FileSystemUtil.cipherPath2PlainPathByUser(file.getPath(), "", "", user);
                InputStream in = FileSystemUtil.ACTION.getInputStream(source1, 0, 0);
                String fileId = FileSystemUtil.ACTION.uploadByServer(path1, file.getName(), in, null,
                        loaded -> {
                            cmf.setSd(loaded / 1024.0 / ((System.currentTimeMillis() - currentTime) / 1000.0));
                            cmf.setJd(loaded * 1.0 / cmf.getSize());
                            cmf.setLoaded(loaded);
                            CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                        }
                );
                if (StrUtil.isBlank(fileId)) {
                    hasError.set(true);
                } else {
                    success++;
                }
            }
            cmf.setCurrentFileName(count + "个文件");
            cmf.setStatus(hasError.get() ? 3 : 2);
            CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
            LoginAuthUtil.USER_LOCAL.set(null);
            return null;
        });
    }

    public static void serverStop(String taskId) {
        CacheUtil.setValue("copymove:task-stop:" + taskId, 1, 3600);
    }

    private String copyOrMove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path, String method) {
        if (!isSameDrive(sourceParent, path)) {
            //通盘不同号 或者 不同盘
            return serverCopyOrMove(sourceParent, sourceChildren, path, method);
        }
        //通盘同号
        FileSystemInface inface = getInfaceByType(sourceParent.getDriveType());
        Assert.notNull(inface, "当前地址暂时还不支持");
        if (StrUtil.equals(method, "copy")) {
            return inface.copy(sourceParent, sourceChildren, sourceTypes, path);
        } else if (StrUtil.equals(method, "move")) {
            return inface.move(sourceParent, sourceChildren, sourceTypes, path);
        } else {
            return "0";
        }
    }

    private String serverCopyOrMove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, PlainPath path, String method) {
        String sourceDriveType = sourceParent.getDriveType();
        String pathDriveType = path.getDriveType();
        String taskId = "task" + IdUtil.fastSimpleUUID();
        final SysUser user = LoginAuthUtil.getUser();
        final CommonBean.CopyMoveFile cmf = new CommonBean.CopyMoveFile();
        String[] pathName1sz = FileSystemUtil.ACTION.pathName(sourceParent).split("/");
        String[] pathName2sz = FileSystemUtil.ACTION.pathName(path).split("/");
        cmf.setSourceName(pathName1sz[pathName1sz.length - 1]);
        cmf.setTargetName(pathName2sz[pathName2sz.length - 1]);
        cmf.setStatus(1);
        cmf.setSd(0d);
        cmf.setLoaded(0);
        cmf.setSize(0);
        cmf.setCurrentFileName("即将开始扫描文件");
        CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
        FiberUtil.run(() -> {
            LoginAuthUtil.USER_LOCAL.set(user);
            final Supplier<Boolean> isStop = () -> {
                Integer flag = CacheUtil.getValue("copymove:task-stop:" + taskId);
                if (flag != null && flag == 1) {
                    return true;
                }
                return false;
            };
            cmf.setCurrentFileName("正在扫描文件,请耐心等待");
            CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
            List<CommonBean.CopyMoveInfo> list = new ArrayList<>();
            String sourceParentRealPath = sourceParent.getRealPath();
            String sourceParentCipherPath = sourceParent.getCipherPath();
            for (int i = 0; i < sourceChildren.size(); i++) {
                String sourceChild = sourceChildren.get(i);
                sourceParent.setRealPath(sourceParentRealPath + "/" + sourceChild);
                sourceParent.setCipherPath(sourceParentCipherPath + "/" + sourceChild);
                CommonBean.PathInfo info = FileSystemUtil.ACTION.fileInfo(sourceParent);
                if (info.getType() == 1) {
                    //文件
                    cmf.setCurrentFileName("(扫描中..)" + info.getName());
                    CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                    info.setPath(sourceParent.getRealPath());
                    list.add(CommonBean.CopyMoveInfo.init(info, sourceParent.getCipherPath()));
                } else {
                    //目录
                    loopList(sourceParent, list, cmf, taskId);
                }
            }

            List<CommonBean.TransmissionFile> files = new ArrayList<>();
            int index = 0;
            int count = list.size();
            int type = 3;//1同盘不同号,利用服务器实现秒传 2不同盘,与服务器文件相关的采用服务器上传 3不同盘,与服务器文件无关的采用前端处理
            if (StrUtil.equals(sourceDriveType, pathDriveType)) {
                type = 1;
            } else {
                if (StrUtil.equals(sourceDriveType, LOCAL_DRIVE_TYPE)
                        || StrUtil.equals(sourceDriveType, SERVER_DRIVE_TYPE)
                        || StrUtil.equals(pathDriveType, LOCAL_DRIVE_TYPE)
                        || StrUtil.equals(pathDriveType, SERVER_DRIVE_TYPE)) {
                    type = 2;
                }
            }
            int success = 0;
            for (CommonBean.CopyMoveInfo cmInfo : list) {
                if (isStop.get()) {
                    cmf.setCurrentFileName(success + "个文件");
                    cmf.setStatus(3);
                    CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                    LoginAuthUtil.USER_LOCAL.set(null);
                    return null;
                }
                index++;
                cmf.setCurrentFileName("(扫描中)" + cmInfo.getInfo().getName());
                cmf.setJd(index * 1.0 / count);
                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                CommonBean.PathInfo fileInfo = cmInfo.getInfo();
                sourceParent.setRealPath(fileInfo.getPath());
                sourceParent.setCipherPath(cmInfo.getCipherPath());
                String pathName = FileSystemUtil.ACTION.pathName(sourceParent);
                String tmpPath2 = StrUtil.removeAll(fileInfo.getPath(), sourceParentRealPath);
                int length = tmpPath2.split("/").length;
                String[] sz = pathName.split("/");
                int cz = sz.length - length;
                List<String> nameList = new ArrayList<>();
                for (int i = 1; i < length; i++) {
                    nameList.add(sz[cz + i]);
                }
                String name = CollUtil.join(nameList, "/");
                if (type == 1) {
                    //同盘不同号,不验证秒传能力
                } else {
                    //不同盘,验证秒传能力
                    String st = FileSystemUtil.ACTION.secondTransmission(path, name, sourceParent.getCipherPath(), fileInfo.getSize());
                    if (StrUtil.equals(st, "1")) {
                        success++;
                        continue;
                    }
                }
                CommonBean.TransmissionFile tf = new CommonBean.TransmissionFile();
                tf.setName(name);
                tf.setPath(path.getCipherPath());
                tf.setSource(sourceParent.getCipherPath());
                tf.setSize(fileInfo.getSize());
                tf.setFileName(fileInfo.getName());
                tf.setThumbnail(fileInfo.getThumbnail());
                files.add(tf);
            }
            if (files.size() == 0) {
                cmf.setCurrentFileName(count + "个文件");
                cmf.setJd(1);
                cmf.setStatus(2);
                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                LoginAuthUtil.USER_LOCAL.set(null);
                return null;
            }
            if (type == 3) {
                //前端处理
                cmf.setStatus(4);
                cmf.setExp(JSONUtil.createObj().set("type", "2").set("files", files).toString());
                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                CacheUtil.setValue("copymove:task-user:" + taskId, user, 3600);
                LoginAuthUtil.USER_LOCAL.set(null);
                return null;
            }
            final AtomicBoolean hasError = new AtomicBoolean(false);
            int fileIndex = 0;
            List<CommonBean.TransmissionFile> sameUploads = new ArrayList<>();
            for (CommonBean.TransmissionFile file : files) {
                if (isStop.get()) {
                    cmf.setCurrentFileName(success + "个文件");
                    cmf.setStatus(3);
                    CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                    LoginAuthUtil.USER_LOCAL.set(null);
                    return null;
                }
                fileIndex++;
                cmf.setCurrentFileName("(剩余" + (files.size() - fileIndex) + "个)" + file.getFileName());
                cmf.setSd(0);
                cmf.setJd(0);
                cmf.setLoaded(0);
                cmf.setSize(file.getSize());
                cmf.setStatus(1);
                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                String fileId;
                long currentTime = System.currentTimeMillis();
                if (type == 1) {
                    //同盘不同号,利用服务器实现秒传
                    fileId = FileSystemUtil.ACTION.sameDriveCopy(sourceDriveType, file);
                    if (StrUtil.isBlank(fileId)) {
                        sameUploads.add(file);
                        break;
                    }
                } else {
                    //不同盘,与服务器文件相关的采用服务器上传
                    PlainPath source1 = FileSystemUtil.cipherPath2PlainPathByUser(file.getSource(), "", "", user);
                    PlainPath path1 = FileSystemUtil.cipherPath2PlainPathByUser(file.getPath(), "", "", user);
                    InputStream in = FileSystemUtil.ACTION.getInputStream(source1, 0, 0);
                    fileId = FileSystemUtil.ACTION.uploadByServer(path1, file.getName(), in, null,
                            loaded -> {
                                cmf.setSd(loaded / 1024.0 / ((System.currentTimeMillis() - currentTime) / 1000.0));
                                cmf.setJd(loaded * 1.0 / cmf.getSize());
                                cmf.setLoaded(loaded);
                                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                            }
                    );
                }
                if (StrUtil.isBlank(fileId)) {
                    hasError.set(true);
                } else {
                    success++;
                }
            }
            if (sameUploads.size() > 0) {
                cmf.setStatus(4);
                cmf.setExp(JSONUtil.createObj().set("type", "2").set("files", files).toString());
                CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                CacheUtil.setValue("copymove:task-user:" + taskId, user, 3600);
                LoginAuthUtil.USER_LOCAL.set(null);
                return null;
            }
            cmf.setCurrentFileName(count + "个文件");
            cmf.setStatus(hasError.get() ? 3 : 2);
            CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
            LoginAuthUtil.USER_LOCAL.set(null);
            return null;
        });
        return taskId;
    }

    private void loopList(PlainPath parent, List<CommonBean.CopyMoveInfo> files, CommonBean.CopyMoveFile cmf, String taskId) {
        String next = "";
        String parentRealPath = parent.getRealPath();
        String parentCipherPath = parent.getCipherPath();
        while (true) {
            CommonBean.Page<CommonBean.PathInfo> page = FileSystemUtil.ACTION.listFiles(parent, next);
            if (CollUtil.isEmpty(page.getList())) {
                break;
            }
            page.getList().forEach(fileInfo -> {
                if (fileInfo.getType() == 1) {
                    String path = fileInfo.getPath();
                    fileInfo.setPath(parentRealPath + "/" + path);
                    files.add(CommonBean.CopyMoveInfo.init(fileInfo, parentCipherPath + "/" + path));
                    cmf.setCurrentFileName("(扫描中..)" + fileInfo.getName());
                    CacheUtil.setValue("copymove:task:" + taskId, cmf, 3600);
                } else {
                    parent.setRealPath(parentRealPath + "/" + fileInfo.getPath());
                    parent.setCipherPath(parentCipherPath + "/" + fileInfo.getPath());
                    loopList(parent, files, cmf, taskId);
                }
            });
            if (StrUtil.isBlank(page.getNext())) {
                break;
            }
            next = page.getNext();
        }
    }

    private boolean isSameDrive(FileSystemInface.PlainPath first, FileSystemInface.PlainPath... list) {
        if (first == null) {
            return false;
        }
        if (list == null) {
            return false;
        }
        for (FileSystemInface.PlainPath tmp : list) {
            if (!StrUtil.equals(tmp.getDriveType(), first.getDriveType())) {
                return false;
            }
            if (!StrUtil.equals(tmp.getDriveType(), LOCAL_DRIVE_TYPE) && !StrUtil.equals(tmp.getDriveType(), SERVER_DRIVE_TYPE)) {
                if (!StrUtil.equals(tmp.getTokenId(), first.getTokenId())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String copy(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path) {
        return copyOrMove(sourceParent, sourceChildren, sourceTypes, path, "copy");
    }

    @Override
    public String move(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path) {
        return copyOrMove(sourceParent, sourceChildren, sourceTypes, path, "move");
    }

    @Override
    public boolean rename(FileSystemInface.PlainPath source, String name, Integer type) {
        return getInfaceByType(source.getDriveType()).rename(source, name, type);
    }

    @Override
    public String uploadPre(FileSystemInface.PlainPath path, String name, String expand) {
        return getInfaceByType(path.getDriveType()).uploadPre(path, name, expand);
    }

    @Override
    public String uploadUrl(FileSystemInface.PlainPath path, String name, String expand) {
        return getInfaceByType(path.getDriveType()).uploadUrl(path, name, expand);
    }

    @Override
    public String uploadAfter(FileSystemInface.PlainPath path, String name, String expand) {
        return getInfaceByType(path.getDriveType()).uploadAfter(path, name, expand);
    }

    @Override
    public String uploadByServer(PlainPath path, String name, InputStream in, File file, Consumer<Long> consumer) {
        return getInfaceByType(path.getDriveType()).uploadByServer(path, name, in, file, consumer);
    }

    @Override
    public String downloadUrl(FileSystemInface.PlainPath path) {
        return getInfaceByType(path.getDriveType()).downloadUrl(path);
    }

    @Override
    public String createDir(FileSystemInface.PlainPath path, String pathName) {
        return getInfaceByType(path.getDriveType()).createDir(path, pathName);
    }

    @Override
    public CommonBean.Page listFiles(FileSystemInface.PlainPath parentPath, String next) {
        return getInfaceByType(parentPath.getDriveType()).listFiles(parentPath, next);
    }

    @Override
    public void refreshToken(String driveType, IoTokenData itd) {
        getInfaceByType(driveType).refreshToken(driveType, itd);
    }


    @Override
    public String remove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes) {
        return getInfaceByType(sourceParent.getDriveType()).remove(sourceParent, sourceChildren, sourceTypes);
    }

    @Override
    public String pathName(PlainPath plainPath) {
        String path = getInfaceByType(plainPath.getDriveType()).pathName(plainPath);
        String[] paths = path.split("/");
        if (StrUtil.isNotBlank(plainPath.getSioNo())) {
            //当前是sio
            ShareFile sf = getShareFileByNo(plainPath.getSioNo());
            paths[0] = sf.getName();
        } else if (StrUtil.isNotBlank(plainPath.getUioNo())) {
            //当前是uio
            IoUserDrive iud = getIoUserDriveByNo(plainPath.getUioNo());
            paths[0] = iud.getName();
        } else {
            //当前是io
            if (StrUtil.isBlank(plainPath.getIoNo()) && LoginAuthUtil.isSystem()) {
                return ArrayUtil.join(paths, "/");
            }
            IoDrive id = getIoDriveByNo(plainPath.getIoNo());
            paths[0] = id.getName();
        }
        return ArrayUtil.join(paths, "/");
    }

    @Override
    public String availableMainName(PlainPath parentPath, String mainName, String ext) {
        return getInfaceByType(parentPath.getDriveType()).availableMainName(parentPath, mainName, ext);
    }

    @Override
    public String unzip(PlainPath file) {
        return getInfaceByType(file.getDriveType()).unzip(file);
    }

    @Override
    public String zip(List<PlainPath> files, PlainPath dirPath) {
        if (!isSameDrive(dirPath, ArrayUtil.toArray(files, PlainPath.class))) {
            return "2";
        }
        return getInfaceByType(dirPath.getDriveType()).zip(files, dirPath);
    }

    @Override
    public CommonBean.PathInfo fileInfo(PlainPath plainPath) {
        return getInfaceByType(plainPath.getDriveType()).fileInfo(plainPath);
    }

    @Override
    public CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit) {
        return getInfaceByType(plainPath.getDriveType()).getWpsUrl(plainPath, isEdit);
    }

    @Override
    public List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name) {
        return getInfaceByType(parentPath.getDriveType()).searchFile(parentPath, name);
    }

    @Override
    public InputStream getInputStream(PlainPath path, long start, long length) {
        return getInfaceByType(path.getDriveType()).getInputStream(path, start, length);
    }

    @Override
    public String secondTransmission(PlainPath path, String name, String sourceCipherPath, long size) {
        return getInfaceByType(path.getDriveType()).secondTransmission(path, name, sourceCipherPath, size);
    }

    @Override
    public String sameDriveCopy(String driveType, CommonBean.TransmissionFile file) {
        return getInfaceByType(driveType).sameDriveCopy(driveType, file);
    }

    @Override
    public String sha1(PlainPath path) {
        String hash = getInfaceByType(path.getDriveType()).sha1(path);
        if (StrUtil.isNotBlank(hash)) {
            return hash.toLowerCase();
        }
        return null;
    }

    public interface HashSimpleInputStream {
        InputStream get(long start, long length);
    }


    public static String fileHashSimple(HashSimpleInputStream hsis, long size) {
        InputStream in;
        if (size > 1e4) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long n = size / 50;
            for (int s = 0; s < 50; s++) {
                InputStream part = hsis.get(n * s, 200);
                IoUtil.copy(part, baos);
                IoUtil.close(part);
            }
            InputStream part = hsis.get(size - 200, 200);
            IoUtil.copy(part, baos);
            IoUtil.close(part);
            in = new ByteArrayInputStream(baos.toByteArray());
        } else {
            in = hsis.get(0, 0);
        }
        String hashSimple = SecureUtil.md5(in) + size;
        IoUtil.close(in);
        return hashSimple;
    }

    @Override
    public String md5(PlainPath path) {
        String md5 = getInfaceByType(path.getDriveType()).md5(path);
        if (StrUtil.isNotBlank(md5)) {
            return md5.toLowerCase();
        }
        return null;
    }

    @Override
    public boolean restore(PlainPath path, IoUserRecycle ioUserRecycle) {
        return getInfaceByType(path.getDriveType()).restore(path, ioUserRecycle);
    }

    @Override
    public String getRootId(String driveType) {
        return getInfaceByType(driveType).getRootId(driveType);
    }

    @Override
    public R commonReq(PlainPath path, Context ctx) {
        return getInfaceByType(path.getDriveType()).commonReq(path, ctx);
    }
}

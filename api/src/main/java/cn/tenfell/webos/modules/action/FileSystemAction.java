package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.net.multipart.MultipartFormData;
import cn.hutool.core.net.multipart.UploadFile;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.filesystem.LocalFileSystem;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.*;
import cn.tenfell.webos.modules.entity.*;
import org.noear.solon.core.handle.Context;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BeanAction(val = "fileSystem")
public class FileSystemAction {
    //查询文件列表
    public static CommonBean.Page<CommonBean.PathInfo> fileListPage(Dict param) {
        String parentPath = param.getStr("parentPath");
        Integer type = param.getInt("type");
        if (type == null) {
            type = 0;
        }
        String next = "";
        if (type != 0) {
            if (type == 1) {
                next = param.getStr("next");
            } else {
                type = 0;
            }
        }
        String shareCode = param.getStr("shareCode");
        String sharePwd = param.getStr("sharePwd");
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(parentPath, shareCode, sharePwd);
        if (StrUtil.isNotBlank(plainPath.getSioNo()) && plainPath.getCipherPath().split("/").length == 1) {
            //分享的数据的列表
            ShareFile sf;
            if (StrUtil.isNotBlank(shareCode)) {
                sf = DbUtil.queryObject("select * from share_file where code = ?", ShareFile.class, shareCode);
            } else {
                sf = DbUtil.queryObject("select * from share_file where no = ?", ShareFile.class, plainPath.getSioNo());
            }
            String[] files = sf.getFiles().split(";");
            String realPath = plainPath.getRealPath();
            String cipherPath = plainPath.getCipherPath();
            CommonBean.Page<CommonBean.PathInfo> page = new CommonBean.Page<>();
            page.setType(0);
            List<CommonBean.PathInfo> list = new ArrayList<>();
            for (String file : files) {
                plainPath.setRealPath(realPath + "/" + file);
                plainPath.setCipherPath(cipherPath + "/" + file);
                CommonBean.PathInfo info = FileSystemUtil.ACTION.fileInfo(plainPath);
                if (info != null) {
                    info.setPath(parentPath + "/" + info.getPath());
                    list.add(info);
                }
            }
            page.setList(list);
            return page;
        } else {
            CommonBean.Page<CommonBean.PathInfo> page = FileSystemUtil.ACTION.listFiles(plainPath, next);
            if (page == null) {
                page = new CommonBean.Page<>();
                page.setList(new ArrayList<>());
            }
            List<CommonBean.PathInfo> list = page.getList();
            for (CommonBean.PathInfo info : list) {
                String pj = "/";
                if (StrUtil.equals(parentPath, "/")) {
                    pj = "";
                }
                info.setPath(parentPath + pj + info.getPath());
            }
            page.setList(list);
            return page;
        }
    }

    /**
     * 文件列表
     *
     * @param param
     * @return
     */
    @Action(val = "fileList", type = 1)
    @Login(val = false)
    public R fileList(Dict param) {
        return R.ok(fileListPage(param), "获取成功");
    }

    @Action(val = "downUrl", type = 1)
    @Login(val = false)
    public R downUrl(Dict param) {
        String path = param.getStr("path");
        String shareCode = param.getStr("shareCode");
        String sharePwd = param.getStr("sharePwd");
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, shareCode, sharePwd);
        String url = FileSystemUtil.ACTION.downloadUrl(plainPath);
        if (StrUtil.isNotBlank(url)) {
            return R.okData(url);
        }
        return R.failed("当前功能敬请期待");
    }

    @Action(val = "fileInfo", type = 1)
    @Login(val = false)
    public R fileInfo(Dict param) {
        String path = param.getStr("path");
        if (path.startsWith("{trash:")) {
            return UserRecycleAction.infoByPath(path);
        }
        String[] sz = StrUtil.replace(path, "\\", "/").split("/");
        String parentPath = ArrayUtil.join(ArrayUtil.remove(sz, sz.length - 1), "/");
        String shareCode = param.getStr("shareCode");
        String sharePwd = param.getStr("sharePwd");
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, shareCode, sharePwd);
        CommonBean.PathInfo info;
        if (sz.length == 1) {
            info = new CommonBean.PathInfo();
            info.setPath(path);
            info.setType(2);
            if (StrUtil.isNotBlank(plainPath.getSioNo())) {
                ShareFile sf = FileSystemUtil.getShareFileByNo(plainPath.getSioNo());
                info.setName(sf.getName());
                info.setCreatedAt(LocalDateTimeUtil.formatNormal(sf.getShareTime()));
            } else if (StrUtil.isNotBlank(plainPath.getUioNo())) {
                IoUserDrive iud = FileSystemUtil.getIoUserDriveByNo(plainPath.getUioNo());
                info.setName(iud.getName());
                info.setCreatedAt(LocalDateTimeUtil.formatNormal(iud.getCreatedTime()));
                info.setUpdatedAt(LocalDateTimeUtil.formatNormal(iud.getUpdatedTime()));
            } else if (StrUtil.isNotBlank(plainPath.getIoNo())) {
                IoDrive id = FileSystemUtil.getIoDriveByNo(plainPath.getIoNo());
                info.setName(id.getName());
                info.setCreatedAt(LocalDateTimeUtil.formatNormal(id.getCreatedTime()));
                info.setUpdatedAt(LocalDateTimeUtil.formatNormal(id.getUpdatedTime()));
            }
        } else {
            info = FileSystemUtil.ACTION.fileInfo(plainPath);
            info.setPath(parentPath + "/" + info.getPath());
        }
        return R.okData(info);
    }

    @Action(val = "pathName", type = 1)
    @Login(val = false)
    public R pathName(Dict param) {
        String path = param.getStr("path");
        String shareCode = param.getStr("shareCode");
        String sharePwd = param.getStr("sharePwd");
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, shareCode, sharePwd);
        String pathName = FileSystemUtil.ACTION.pathName(plainPath);
        if (StrUtil.isNotBlank(pathName)) {
            return R.okData(pathName);
        }
        return R.failed("当前功能敬请期待");
    }

    @Action(val = "remove", type = 1)
    public R remove(Dict param) {
        String flag = copyMoveDel(param, "remove");
        if (StrUtil.equals(flag, "1")) {
            return R.ok(null, "删除成功");
        }
        return R.failed("删除失败");
    }

    private String copyMoveDel(Dict param, String type) {
        String sourceParent = param.getStr("sourceParent");
        List<String> sourceChildren = param.getBean("sourceChildren");
        List<Integer> sourceTypes = param.getBean("sourceTypes");
        String target = param.getStr("target");
        FileSystemInface.PlainPath sourceParentPath = FileSystemUtil.cipherPath2PlainPathByLogin(sourceParent, "", "");
        if (StrUtil.equals(type, "copy")) {
            FileSystemInface.PlainPath targetPath = FileSystemUtil.cipherPath2PlainPathByLogin(target, "", "");
            return FileSystemUtil.ACTION.copy(sourceParentPath, sourceChildren, sourceTypes, targetPath);
        } else if (StrUtil.equals(type, "move")) {
            FileSystemInface.PlainPath targetPath = FileSystemUtil.cipherPath2PlainPathByLogin(target, "", "");
            return FileSystemUtil.ACTION.move(sourceParentPath, sourceChildren, sourceTypes, targetPath);
        } else if (StrUtil.equals(type, "remove")) {
            List<CommonBean.PathInfo> infos = null;
            Map<String, String> removeMap = null;
            String realPath = sourceParentPath.getRealPath();
            String cipherPath = sourceParentPath.getCipherPath();
            if (!StrUtil.equals(sourceParentPath.getDriveType(), FileSystemUtil.SERVER_DRIVE_TYPE)) {
                infos = new ArrayList<>();
                if (StrUtil.equals(sourceParentPath.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
                    removeMap = new HashMap<>();
                }
                for (String child : sourceChildren) {
                    sourceParentPath.setRealPath(realPath + "/" + child);
                    sourceParentPath.setCipherPath(cipherPath + "/" + child);
                    infos.add(FileSystemUtil.ACTION.fileInfo(sourceParentPath));
                    if (StrUtil.equals(sourceParentPath.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
                        String cycleId = IdUtil.fastSimpleUUID();
                        String cycle = ProjectUtil.rootPath + "/recycle/" + cycleId + ".zip";
                        OutputStream fos = FileUtil.getOutputStream(cycle);
                        List<File> list = new ArrayList<>();
                        File obj = new File(sourceParentPath.getRealPath());
                        if (obj.exists()) {
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
                            File file = list.get(i);
                            ins[i] = FileUtil.getInputStream(file);
                            paths[i] = StrUtil.replace(StrUtil.replace(file.getAbsolutePath(), "\\", "/"), realPath + "/", "");
                        }
                        ZipUtil.zip(fos, paths, ins);
                        removeMap.put(child, cycleId);
                    }
                }
                sourceParentPath.setRealPath(realPath);
                sourceParentPath.setCipherPath(cipherPath);
            }
            String removeStr = FileSystemUtil.ACTION.remove(sourceParentPath, sourceChildren, sourceTypes);
            sourceParentPath.setRealPath(realPath);
            sourceParentPath.setCipherPath(cipherPath);
            if (StrUtil.equals(removeStr, "1") && infos != null) {
                for (CommonBean.PathInfo info : infos) {
                    IoUserRecycle iur = new IoUserRecycle();
                    iur.setUserId(LoginAuthUtil.getUser().getId());
                    iur.setSize(info.getSize());
                    iur.setType(info.getType());
                    iur.setName(info.getName());
                    iur.setDeletedTime(LocalDateTime.now());
                    iur.setRemovePath(sourceParentPath.getCipherPath() + "/" + info.getPath());
                    if (StrUtil.equals(sourceParentPath.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
                        iur.setId(removeMap.get(info.getPath()));
                    } else {
                        iur.setId(IdUtil.fastSimpleUUID());
                    }
                    DbUtil.insertObject(iur);
                }
            } else if (StrUtil.equals(removeStr, "0") && removeMap != null) {
                removeMap.forEach((s, path) -> FileUtil.del(path));
            }
            return removeStr;
        } else {
            Assert.isTrue(false, "此操作不存在");
        }
        return "";
    }

    @Action(val = "copy", type = 1)
    public R copy(Dict param) {
        String resData = copyMoveDel(param, "copy");
        return R.ok(resData, "请根据结果判断");
    }

    @Action(val = "move", type = 1)
    public R move(Dict param) {
        String resData = copyMoveDel(param, "move");
        return R.ok(resData, "请根据结果判断");
    }

    @Action(val = "rename", type = 1)
    public R rename(Dict param) {
        String path = param.getStr("path");
        String name = param.getStr("name");
        Integer type = param.getInt("type");
        FileSystemInface.PlainPath file = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        boolean flag = FileSystemUtil.ACTION.rename(file, name, type);
        if (flag) {
            return R.ok();
        }
        return R.failed();
    }

    @Action(val = "serverJd", type = 1)
    public R serverJd(Dict param) {
        String taskId = param.getStr("taskId");
        CommonBean.CopyMoveFile cmf = CacheUtil.getValue("copymove:task:" + taskId);
        if (cmf == null) {
            return R.failed();
        }
        return R.okData(cmf);
    }

    @Action(val = "serverConfirm", type = 1)
    public R serverConfirm(Dict param) {
        String taskId = param.getStr("taskId");
        FileSystemUtil.serverConfirm(taskId);
        return R.ok();
    }

    @Action(val = "serverStop", type = 1)
    public R serverStop(Dict param) {
        String taskId = param.getStr("taskId");
        FileSystemUtil.serverStop(taskId);
        return R.ok(null, "将在下一个文件进行终止");
    }

    @Action(val = "createDir", type = 1)
    public R createDir(Dict param) {
        String path = param.getStr("path");
        String name = param.getStr("name");
        FileSystemInface.PlainPath file = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        String fileId = FileSystemUtil.ACTION.createDir(file, name);
        if (StrUtil.isNotBlank(fileId)) {
            return R.okData(fileId);
        }
        return R.failed();
    }

    @Action(val = "getDriveType", type = 1)
    @Login(val = false)
    public R getDriveType(Dict param) {
        String path = param.getStr("path");
        String shareCode = param.getStr("shareCode");
        String sharePwd = param.getStr("sharePwd");
        FileSystemInface.PlainPath file = FileSystemUtil.cipherPath2PlainPathByLogin(path, shareCode, sharePwd);
        return R.ok(file.getDriveType(), "获取驱动类型成功");
    }

    @Action(val = "uploadPre", type = 1)
    public R uploadPre(Dict param) {
        String path = param.getStr("path");
        Assert.isFalse(path.startsWith("{sio:"), "此目录不支持上传");
        FileSystemInface.PlainPath parentPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        String expand = param.getStr("expand");
        String name = param.getStr("name");
        String data = FileSystemUtil.ACTION.uploadPre(parentPath, name, expand);
        return R.ok(data, "获取上传数据成功");
    }

    @Action(val = "uploadUrl", type = 1)
    public R uploadUrl(Dict param) {
        String path = param.getStr("path");
        Assert.isFalse(path.startsWith("{sio:"), "此目录不支持上传");
        String name = param.getStr("name");
        FileSystemInface.PlainPath parentPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        String expand = param.getStr("expand");
        String data = FileSystemUtil.ACTION.uploadUrl(parentPath, name, expand);
        return R.ok(data, "上传验证完成");
    }

    @Action(val = "uploadAfter", type = 1)
    public R uploadAfter(Dict param) {
        String path = param.getStr("path");
        Assert.isFalse(path.startsWith("{sio:"), "此目录不支持上传");
        String name = param.getStr("name");
        FileSystemInface.PlainPath parentPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        String expand = param.getStr("expand");
        String data = FileSystemUtil.ACTION.uploadAfter(parentPath, name, expand);
        return R.ok(data, "上传验证完成");
    }

    @Action(val = "pathEncrypt", type = 1)
    @Login(val = false)
    public R pathEncrypt(Dict dict) {
        String path = dict.getStr("path");
        JSONObject map = JSONUtil.createObj().set("path", path);
        SysUser user = LoginAuthUtil.getUser();
        if (user != null) {
            map.set("hash", user.getId());
        }
        String str = SecureUtil.rsa(ProjectUtil.startConfig.getStr("zlPrivate"), ProjectUtil.startConfig.getStr("zlPublic")).encryptBase64(map.toString(), CharsetUtil.CHARSET_UTF_8, KeyType.PublicKey);
        return R.okData(str);
    }

    private InputStream getContentByType(Dict dict, int type) {
        //type 1下载内容(使用重定向)  2.ajax获取内容(支持的使用重定向,不支持的使用中转)
        String pathCipher = dict.getStr("path");
        String jsonStr = SecureUtil.rsa(ProjectUtil.startConfig.getStr("zlPrivate"), ProjectUtil.startConfig.getStr("zlPublic")).decryptStr(pathCipher, KeyType.PrivateKey);
        JSONObject map = JSONUtil.parseObj(jsonStr);
        String path = map.getStr("path");
        String userId = map.getStr("hash");
        String share = dict.getStr("share");
        String pwd = dict.getStr("pwd");
        String lastname = dict.getStr("lastname");
        if (StrUtil.isBlank(path)) {
            return null;
        }
        SysUser user = null;
        if (StrUtil.isNotBlank(userId)) {
            user = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, userId);
        }
        if (user == null) {
            user = LoginAuthUtil.getUser();
        }
        FileSystemInface.PlainPath plainPath;
        if (StrUtil.equals(lastname, "1")) {
            String[] sz = StrUtil.replace(path, "\\", "/").split("/");
            String name = sz[sz.length - 1];
            String parentPath = ArrayUtil.join(ArrayUtil.remove(sz, sz.length - 1), "/");
            FileSystemInface.PlainPath parent = FileSystemUtil.cipherPath2PlainPathByUser(parentPath, share, pwd, user);
            List<CommonBean.PathInfo> list = FileSystemUtil.ACTION.searchFile(parent, name);
            if (CollUtil.isEmpty(list)) {
                return null;
            }
            plainPath = FileSystemUtil.cipherPath2PlainPathByUser(parentPath + "/" + list.get(0).getPath(), share, pwd, user);
        } else {
            plainPath = FileSystemUtil.cipherPath2PlainPathByUser(path, share, pwd, user);
        }
        if (type == 2) {
            //需要中转才能获取内容的在这里处理
        }
        //重定向获取
        String url = FileSystemUtil.ACTION.downloadUrl(plainPath);
        Context ctx = ProjectUtil.getContext().getCtx();
        ctx.headerSet("Location", url);
        ctx.headerSet("Access-Control-Allow-Origin", "*");
        ctx.headerSet("Access-Control-Allow-Credentials", "true");
        ctx.headerSet("Access-Control-Allow-Methods", "*");
        ctx.headerSet("Access-Control-Allow-Headers", "*");
        ctx.headerSet("Access-Control-Expose-Headers", "*");
        ctx.headerSet("Access-Control-Max-Age", "86400");
        ctx.status(302);
        return null;
    }

    @Action(val = "content", type = 2)
    @Login(val = false)
    public InputStream content(Dict dict) {
        return getContentByType(dict, 2);
    }


    @Action(val = "url", type = 2)
    @Login(val = false)
    public InputStream url(Dict dict) {
        return getContentByType(dict, 1);
    }

    @Action(val = "availableMainName", type = 1)
    public R availableMainName(Dict dict) {
        String path = dict.getStr("path");
        String mainName = dict.getStr("mainName");
        String ext = dict.getStr("ext");
        FileSystemInface.PlainPath parentPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        //返回新名称,不存在直接返回mainName,存在返回可能的mainName新名称
        String newMainName = FileSystemUtil.ACTION.availableMainName(parentPath, mainName, ext);
        return R.okData(newMainName);
    }

    @Action(val = "unzip", type = 1)
    public R unzip(Dict dict) {
        String path = dict.getStr("path");
        FileSystemInface.PlainPath file = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        //返回新名称,不存在直接返回mainName,存在返回可能的mainName新名称
        String status = FileSystemUtil.ACTION.unzip(file);
        if (StrUtil.equals(status, "1")) {
            return R.ok(null, "解压成功");
        }
        return R.failed("解压失败");
    }


    @Action(val = "zip", type = 1)
    public R zip(JSONObject dict) {
        List<String> paths = dict.getBeanList("paths", String.class);
        List<FileSystemInface.PlainPath> files = new ArrayList<>();
        for (String path : paths) {
            FileSystemInface.PlainPath file = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
            files.add(file);
        }
        FileSystemInface.PlainPath parent = FileSystemUtil.cipherPath2PlainPathByLogin(dict.getStr("parentPath"), "", "");
        //返回新名称,不存在直接返回mainName,存在返回可能的mainName新名称
        String flag = FileSystemUtil.ACTION.zip(files, parent);
        if (StrUtil.equals("1", flag)) {
            return R.ok("1", "压缩成功");
        } else if (StrUtil.equals("2", flag)) {
            return R.ok("2", "此盘暂不支持压缩");
        }
        return R.failed("压缩失败");
    }


    @Action(val = "uploadSmallFile", type = 0)
    public R uploadSmallFile(Context ctx) throws Exception {
        MultipartFormData multiForm = ProjectContext.getMultipart(ctx);
        Map<String, String[]> param = multiForm.getParamMap();
        String parentPath = param.get("parentPath")[0];
        Assert.isFalse(parentPath.startsWith("{sio:"), "此目录不支持上传");
        UploadFile file = multiForm.getFile("file");
        InputStream in;
        if (file.size() == 0) {
            in = new ByteArrayInputStream(new byte[0]);
        } else {
            in = file.getFileInputStream();
        }
        String name = param.get("name")[0];
        FileSystemInface.PlainPath parent = FileSystemUtil.cipherPath2PlainPathByLogin(parentPath, "", "");
        String fileId = FileSystemUtil.ACTION.uploadByServer(parent, name, in, null, null);
        Assert.notBlank(fileId, "保存失败");
        return R.ok(fileId, "保存成功");
    }

    //以下方法主要针对本地文件
    @Action(val = "localFileUpload", type = 0)
    @Login(val = false)
    public R localFileUpload(Context ctx) {
        String upload_id = ctx.header("upload-id");
        String index = ctx.header("upload-index");
        String fp_hash = ctx.header("fp-hash");
        try {
            String filePath = ProjectUtil.rootPath + "/tmpUpload/" + upload_id + "/" + index + ".data";
            FileUtil.writeFromStream(ctx.bodyAsStream(), filePath);
            String nowHash = SecureUtil.md5(new File(filePath));
            if (fp_hash.equals(nowHash)) {
                return R.ok("1", "上传成功");
            }
        } catch (Exception e) {

        }
        return R.ok("2", "分片上传失败");
    }

    @Action(val = "localFileViewer", type = 0)
    @Login(val = false)
    public InputStream localFileViewer(Context ctx) {
        String ext = ctx.param("ext");
        String path = ctx.param("path");
        String tbPath = CommonUtil.getTbPath(ext, path);
        if (StrUtil.isBlank(tbPath)) {
            return null;
        }
        if (CommonUtil.isImage(ext)) {
            ctx.headerSet("content-type", "image/jpeg");
        } else {
            ctx.headerSet("content-type", "application/octet-stream");
        }
        return FileUtil.getInputStream(tbPath);
    }

    @Action(val = "localFileDown", type = 0)
    @Login(val = false)
    public InputStream localFileDown(Context ctx) {
        String tempId = ctx.param("tempId");
        Dict data = CacheUtil.getValue("file_down:" + tempId);
        if (data == null) {
            return null;
        }
        String name = data.getStr("name");
        String realPath;
        if (StrUtil.equals(data.getStr("type"), FileSystemUtil.SERVER_DRIVE_TYPE)) {
            realPath = data.getStr("realPath");
        } else if (StrUtil.equals(data.getStr("type"), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
            realPath = LocalFileSystem.getRealFile(data.getStr("realFilePath"), data.getStr("realPath"));
        } else {
            return null;
        }
        File file = new File(realPath);
        long size = file.length();
        if (size == 0) {
            return null;
        }
        //type 1试探请求 2分片下载 3完整下载
        int type = 3;
        if (StrUtil.equals(ctx.method(), "HEAD")) {
            type = 1;
        }
        long start = 0;
        long end = 0;
        if (type == 3) {
            String range = ctx.header("Range");
            if (StrUtil.isNotBlank(range)) {
                type = 1;
                String[] sz = range.split("=")[1].split(",")[0].split("-");
                if (sz.length > 0) {
                    start = Convert.toLong(sz[0].trim());
                    if (sz.length > 1) {
                        end = Convert.toLong(sz[1].trim());
                    } else {
                        end = size - 1;
                    }
                    type = 2;
                }
            }
        }
        if (CommonUtil.isImage(FileUtil.extName(name))) {
            ctx.headerSet("Content-Type", "image/jpeg");
        } else {
            ctx.headerSet("Content-Type", "application/octet-stream");
        }
        ctx.headerSet("Accept-Ranges", "bytes");
        ctx.headerSet("Content-Disposition", "attachment; filename=\"" + URLUtil.encodeAll(name) + "\"");
        if (type == 1 || type == 3) {
            start = 0;
            end = size - 1;
        }
        ctx.headerSet("Content-Length", Convert.toStr(end - start + 1));
        if (type == 1) {
            return null;
        }
        InputStream in = FileUtil.getInputStream(file);
        long length = end - start + 1;
        InputStream resIn;
        if (ProjectUtil.startConfig.getBool("rangeFilter")) {
            ctx.headerSet("Content-Range", "bytes " + start + "-" + end + "/" + size);
            resIn = new ShardingInputStream(in, start, length);
        } else {
            resIn = in;
        }
        if (type == 2) {
            ctx.status(206);
        } else if (type == 3) {
            ctx.status(200);
        }
        return resIn;
    }

    @Action(val = "commonDriveReq", type = 0)
    public R commonDriveReq(Context req) {
        String path = URLUtil.decode(req.header("drive-path"));
        Assert.notBlank(path, "路径不可为空");
        FileSystemInface.PlainPath drivePath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
        return FileSystemUtil.ACTION.commonReq(drivePath, req);
    }
}

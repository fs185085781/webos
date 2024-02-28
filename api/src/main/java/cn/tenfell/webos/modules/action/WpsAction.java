package cn.tenfell.webos.modules.action;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.AliyunDriveFileSystem;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.CacheUtil;
import cn.tenfell.webos.common.util.ValidaUtil;

import java.io.InputStream;

@BeanAction(val = "wps")
public class WpsAction {
    private final static WpsDriveSystem wpsDriveSystem;

    static {
        wpsDriveSystem = new WpsDriveSystem();
    }

    private static JSONObject getCommonToken() {
        return CacheUtil.getCacheData("wps:common_token", () -> JSONUtil.parseObj(HttpUtil.get("https://plugins.webos.tenfell.cn/aliyundrive_third_token/index.php?type=5")), 300);
    }

    private static class WpsDriveSystem extends AliyunDriveFileSystem {
        @Override
        public JSONObject getToken(String tokenId) {
            return getCommonToken();
        }
    }

    @Action(val = "save", type = 1)
    public R save(Dict param) throws Exception {
        String path = param.getStr("path");
        String fileId = param.getStr("fileId");
        String name = param.getStr("name");
        ValidaUtil.init(param)
                .notBlank("name", "名称")
                .notBlank("path", "路径")
                .notBlank("fileId", "文件id");
        FileSystemInface.PlainPath tmp = new FileSystemInface.PlainPath();
        tmp.setDriveType(FileSystemUtil.ALI_PAN_DRIVE_TYPE);
        tmp.setRealPath(fileId);
        String[] sz = StrUtil.replace(path, "\\", "/").split("/");
        String parentPath = ArrayUtil.join(ArrayUtil.remove(sz, sz.length - 1), "/");
        FileSystemInface.PlainPath plainParentPath = FileSystemUtil.cipherPath2PlainPathByLogin(parentPath, "", "");
        String newFile = FileSystemUtil.ACTION.uploadByServer(plainParentPath, name, wpsDriveSystem.getInputStream(tmp, 0, 0), null, null);
        Assert.notBlank(newFile, "文件上传失败");
        return R.ok(null, "保存成功");
    }

    @Action(val = "url", type = 1)
    @Login(val = false)
    public R url(Dict param) {
        String path = param.getStr("path");
        boolean edit = StrUtil.equals(param.getStr("edit"), "edit");
        boolean hasFileAuth = false;
        FileSystemInface.PlainPath plainPath = null;
        try {
            plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, "", "");
            hasFileAuth = true;
        } catch (Exception e) {

        }
        if (edit && plainPath != null) {
            try {
                CommonBean.WpsUrlData data = FileSystemUtil.ACTION.getWpsUrl(plainPath, true);
                Assert.notNull(data, "当前网盘还未支持");
                data.setHasFileAuth(true);
                if (data.getType() == 1) {
                    return R.okData(data);
                }
                if (data.getType() == 2) {
                    pushFile2AliAndGetUrl(plainPath, data, true);
                }
                if (data.getType() == 0) {
                    return R.failed("文件获取失败,建议刷新页面重试");
                }
                return R.okData(data);
            } catch (Exception e) {
                //无权限
            }
        }
        String shareCode = param.getStr("shareCode");
        String sharePwd = param.getStr("sharePwd");
        plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, shareCode, sharePwd);
        CommonBean.WpsUrlData data = FileSystemUtil.ACTION.getWpsUrl(plainPath, false);
        data.setHasFileAuth(hasFileAuth);
        Assert.notNull(data, "当前网盘还未支持");
        if (data.getType() == 1) {
            return R.okData(data);
        }
        if (data.getType() == 2) {
            pushFile2AliAndGetUrl(plainPath, data, false);
        }
        if (data.getType() == 0) {
            return R.failed("文件获取失败,建议刷新页面重试");
        }
        return R.okData(data);
    }

    private void pushFile2AliAndGetUrl(FileSystemInface.PlainPath plainPath, CommonBean.WpsUrlData data, boolean edit) {
        CommonBean.PathInfo info = FileSystemUtil.ACTION.fileInfo(plainPath);
        InputStream in = FileSystemUtil.ACTION.getInputStream(plainPath, 0, 0);
        FileSystemInface.PlainPath tmp = new FileSystemInface.PlainPath();
        tmp.setDriveType(FileSystemUtil.ALI_PAN_DRIVE_TYPE);
        String ali_path = getCommonToken().getStr("list_id");
        tmp.setRealPath(ali_path);
        String name = IdUtil.fastSimpleUUID() + "." + info.getExt();
        String fileId = wpsDriveSystem.uploadByServer(tmp, name, in, null, null);
        if (StrUtil.isBlank(fileId)) {
            data.setType(0);
            return;
        }
        tmp.setRealPath(ali_path + "/" + fileId);
        CommonBean.WpsUrlData res = wpsDriveSystem.getWpsUrl(tmp, edit);
        if (res.getType() == 0) {
            data.setType(0);
            return;
        }
        data.setUrl(res.getUrl());
        data.setToken(res.getToken());
        data.setEdit(res.isEdit());
        data.setFileId(fileId);
    }
}

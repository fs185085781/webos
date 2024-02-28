package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.Auth;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Transactional;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.util.TokenDataUtil;
import cn.tenfell.webos.common.util.ValidaUtil;
import cn.tenfell.webos.modules.entity.IoDrive;
import cn.tenfell.webos.modules.entity.IoTokenData;
import cn.tenfell.webos.modules.entity.IoUserDrive;
import cn.tenfell.webos.modules.entity.SysUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BeanAction(val = "ioDrive")
public class IoDriveAction {
    public static CommonBean.PageRes<IoDrive> mainUserDrive(Dict param){
        SysUser user = LoginAuthUtil.getUser();
        Assert.isTrue(user.getUserType()==1,"权限不足");
        int currentPage = param.getInt("current");
        Integer pageSize = param.getInt("pageSize");
        String where = " where 1=1";
        List<Object> params = new ArrayList<>();
        if (!LoginAuthUtil.isSystem()) {
            where += " and parent_user_id = ?";
            params.add(user.getId());
        }
        CommonBean.PageRes<IoDrive> data = DbUtil.pageObject("select *", "from io_drive" + where, IoDrive.class, currentPage, pageSize, params.toArray());
        for (IoDrive id : data.getData()) {
            id.setName("(" + UserAction.getUserName(id.getParentUserId()) + ")" + id.getName());
        }
        return data;
    }
    /**
     * 获取硬盘列表
     * 主用户专享
     */
    @Action(val = "list", type = 1)
    @Auth(val = "main_auth")
    public R list(Dict param) {
        return R.ok(mainUserDrive(param), "获取成功");
    }

    @Action(val = "info", type = 1)
    @Auth(val = "main_auth")
    public R info(IoDrive data) {
        SysUser user = LoginAuthUtil.getUser();
        String where = " where id = ?";
        List<Object> params = new ArrayList<>();
        params.add(data.getId());
        if (!LoginAuthUtil.isSystem()) {
            where += " and parent_user_id = ?";
            params.add(user.getId());
        }
        IoDrive db = DbUtil.queryObject("select * from io_drive" + where, IoDrive.class, params.toArray());
        Assert.notNull(db, "当前记录不存在");
        return R.ok(db, "获取成功");
    }

    public static IoDrive saveIoDrive(IoDrive param, String userId) {
        IoDrive data = new IoDrive();
        data.setParentUserId(userId);
        data.setCreatedTime(LocalDateTime.now());
        data.setName(param.getName());
        data.setDriveType(param.getDriveType());
        data.setMaxSize(param.getMaxSize());
        data.setUserDriveName(param.getUserDriveName());
        data.setPath(StrUtil.replace(StrUtil.replace(param.getPath(), "\\", "/"), "//", "/"));
        data.setUseSize(0L);
        data.setSecondTransmission(param.getSecondTransmission());
        if (StrUtil.equals(data.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE) && data.getSecondTransmission() == 1) {
            Assert.notBlank(param.getRealFilePath(), "开启秒传但是缺少真实文件目录");
        }
        if (StrUtil.isNotBlank(param.getRealFilePath())) {
            String tmp = StrUtil.replace(StrUtil.replace(param.getRealFilePath(), "\\", "/"), "//", "/");
            if (tmp.endsWith("/")) {
                tmp = tmp.substring(0, tmp.length() - 1);
            }
            data.setRealFilePath(tmp);
        }
        data.setAvailSize(data.getMaxSize());
        data.setUpdatedTime(data.getCreatedTime());
        IoTokenData itd = TokenDataUtil.getTokenDataByIdOrToken(data.getDriveType(), param.getTokenId());
        data.setTokenId(itd.getId());
        Long no = DbUtil.queryLong("select max(`no`) as `max_no` from `io_drive`", IoDrive.class);
        no++;
        data.setNo(no.intValue());
        data.setId(IdUtil.fastSimpleUUID());
        Assert.isTrue(DbUtil.insertObject(data), "硬盘创建失败");
        return data;
    }

    /**
     * 添加硬盘
     * 主用户专享
     */
    @Action(val = "save", type = 1)
    @Auth(val = "main_auth")
    public R save(IoDrive param) {
        SysUser user = LoginAuthUtil.getUser();
        ValidaUtil.init(param)
                .notBlank("name", "名称")
                .notBlank("driveType", "类型")
                .notNull("maxSize", "容量")
                .notBlank("tokenId", "配置", StrUtil.equals(param.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE))
                .notBlank("path", "路径");
        if (StrUtil.equals(param.getDriveType(), FileSystemUtil.LOCAL_DRIVE_TYPE)) {
            Assert.isTrue(LoginAuthUtil.isSystem(), "非管理员无法添加本地磁盘");
        }
        saveIoDrive(param, user.getId());
        return R.ok();
    }

    /**
     * 修改硬盘
     * 主用户专享
     */
    @Action(val = "update", type = 1)
    @Auth(val = "main_auth")
    public R update(IoDrive param) {
        SysUser user = LoginAuthUtil.getUser();
        String where = " where id = ?";
        List<Object> params = new ArrayList<>();
        params.add(param.getId());
        if (!LoginAuthUtil.isSystem()) {
            where += " and parent_user_id = ?";
            params.add(user.getId());
        }
        IoDrive db = DbUtil.queryObject("select * from io_drive" + where, IoDrive.class, params.toArray());
        Assert.notNull(db, "当前记录不存在");
        if (StrUtil.isNotBlank(param.getName())) {
            db.setName(param.getName());
        }
        if (param.getMaxSize() > 0) {
            db.setMaxSize(param.getMaxSize());
            db.setAvailSize(db.getMaxSize() - db.getUseSize());
            if (db.getAvailSize() < 0) {
                db.setAvailSize(0);
            }
        }
        if (StrUtil.isNotBlank(param.getTokenId())) {
            IoTokenData itd = TokenDataUtil.getTokenDataByIdOrToken(db.getDriveType(), param.getTokenId());
            db.setTokenId(itd.getId());
        }
        if (StrUtil.isNotBlank(param.getUserDriveName())) {
            db.setUserDriveName(param.getUserDriveName());
        }
        db.setUpdatedTime(LocalDateTime.now());
        return DbUtil.commonEdit(db);
    }

    public static void delIoDriveByIds(List<IoDrive> list){
        if(CollUtil.isEmpty(list)){
            return;
        }
        List<String> ids = CollUtil.getFieldValues(list, "id", String.class);
        String idsStr = StrUtil.format("'{}'", CollUtil.join(ids, "','"));
        DbUtil.delete("delete from io_drive where id in ( " + idsStr + " )", IoDrive.class);
        List<IoUserDrive> iuds = DbUtil.queryList("select * from io_user_drive where drive_id in ( " + idsStr + " )", IoUserDrive.class);
        IoUserDriveAction.delIoUserDriveByIds(iuds);
    }

    @Action(val = "dels", type = 1)
    @Auth(val = "main_auth")
    @Transactional
    public R dels(List<String> ids) {
        Assert.notEmpty(ids, "当前数据不存在");
        SysUser user = LoginAuthUtil.getUser();
        String idsStr = StrUtil.format("'{}'", CollUtil.join(ids, "','"));
        String where = "";
        List<Object> params = new ArrayList<>();
        if (!LoginAuthUtil.isSystem()) {
            where += " and parent_user_id = ?";
            params.add(user.getId());
        }
        List<IoDrive> list = DbUtil.queryList("select * from io_drive where id in ( " + idsStr + " )" + where, IoDrive.class, params.toArray());
        delIoDriveByIds(list);
        return R.ok();
    }


    /**
     * 获取下拉框
     */
    @Action(val = "select")
    @Auth(val = "main_auth")
    public R select() {
        List<IoDrive> list = selectData();
        return R.ok(list, "获取成功");
    }

    private List<IoDrive> selectData() {
        SysUser loginUser = LoginAuthUtil.getUser();
        if (LoginAuthUtil.isSystem()) {
            return DbUtil.queryList("select * from io_drive", IoDrive.class);
        } else {
            return DbUtil.queryList("select * from io_drive where parent_user_id = ?", IoDrive.class, loginUser.getId());
        }
    }

    /**
     * 获取下拉框Map
     */
    @Action(val = "selectMap")
    @Auth(val = "main_auth")
    public R selectMap() {
        List<IoDrive> list = selectData();
        Map<String, String> map = new HashMap<>();
        for (IoDrive id : list) {
            map.put(id.getId(), id.getName() + "(" + id.getPath() + ")");
        }
        return R.ok(map, "获取成功");
    }

    @Action(val = "getTokenId", type = 1)
    @Auth(val = "main_auth")
    public R getTokenId(Dict param) {
        String driveType = param.getStr("driveType");
        String tokenId = param.getStr("tokenId");
        IoTokenData itd = TokenDataUtil.getTokenDataByIdOrToken(driveType, tokenId);
        Assert.notNull(itd, "当前token配置有误,请重新输入");
        Assert.notBlank(itd.getId(), "当前token配置有误,请重新输入");
        return R.ok(itd.getId(), "获取成功");
    }

    @Action(val = "getFolderByParentPath", type = 1)
    @Auth(val = "main_auth")
    public R getFolderByParentPath(Dict param) {
        String driveType = param.getStr("driveType");
        if (StrUtil.equals(driveType, FileSystemUtil.LOCAL_DRIVE_TYPE)) {
            driveType = FileSystemUtil.SERVER_DRIVE_TYPE;
            Assert.isTrue(LoginAuthUtil.isSystem(), "非管理员无法访问本地磁盘");
        }
        String tokenId = param.getStr("tokenId");
        String parentPath = param.getStr("parentPath");
        if (StrUtil.isBlank(parentPath)) {
            parentPath = FileSystemUtil.ACTION.getRootId(driveType);
        }
        FileSystemInface.PlainPath parent = new FileSystemInface.PlainPath();
        parent.setRealPath(parentPath);
        parent.setTokenId(tokenId);
        parent.setDriveType(driveType);
        List<Dict> data = new ArrayList<>();
        String next = "";
        while (true) {
            CommonBean.Page<CommonBean.PathInfo> page = FileSystemUtil.ACTION.listFiles(parent, next);
            List<CommonBean.PathInfo> list = page.getList();
            if (CollUtil.isNotEmpty(list)) {
                for (CommonBean.PathInfo pathInfo : list) {
                    if (pathInfo.getType() != 2) {
                        continue;
                    }
                    data.add(Dict.create()
                            .set("name", pathInfo.getName())
                            .set("path", parentPath + "/" + pathInfo.getPath())
                    );
                }
            }
            if (StrUtil.isBlank(page.getNext())) {
                break;
            }
            next = page.getNext();
        }
        return R.okData(Dict.create().set("data", data).set("parent", parentPath));
    }
}

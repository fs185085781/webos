package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.annt.Transactional;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.modules.entity.ShareFile;
import cn.tenfell.webos.modules.entity.SysUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@BeanAction(val = "shareFile")
public class ShareFileAction {

    /**
     * 获取随机code
     *
     * @return
     */
    @Action(val = "getCode", type = 1)
    public R getCode(Dict param) {
        while (true) {
            String code = RandomUtil.randomString(RandomUtil.BASE_CHAR + RandomUtil.BASE_CHAR.toUpperCase() + RandomUtil.BASE_NUMBER, 8);
            long count = DbUtil.queryLong("select count(0) from share_file where code = ?", ShareFile.class, code);
            if (count == 0) {
                return R.okData(code);
            }
        }
    }

    @Action(val = "save", type = 1)
    @Transactional
    public R save(ShareFile sf) {
        Assert.notBlank(sf.getName(), "分享标题不可为空");
        if (StrUtil.isNotBlank(sf.getId())) {
            ShareFile db = DbUtil.queryObject("select * from share_file where id = ? and user_id = ?", ShareFile.class, sf.getId(), LoginAuthUtil.getUser().getId());
            Assert.notNull(db, "此分享数据不存在");
            db.setPassword(sf.getPassword());
            db.setExpireTime(sf.getExpireTime());
            db.setName(sf.getName());
            DbUtil.updateObject(db, Entity.create().set("id", sf.getId()));
        } else {
            Assert.notBlank(sf.getCode(), "编码不可为空");
            Assert.notBlank(sf.getFiles(), "文件不可为空");
            Assert.notBlank(sf.getPath(), "当前文件不允许分享");
            //验证用户是否有此路径的权限
            FileSystemUtil.cipherPath2PlainPathByLogin(sf.getPath(), "", "");
            sf.setId(IdUtil.fastSimpleUUID());
            long count = DbUtil.queryLong("select count(0) from share_file where code = ?", ShareFile.class, sf.getCode());
            Assert.isTrue(count == 0, "当前编号已存在");
            sf.setUserId(LoginAuthUtil.getUser().getId());
            sf.setViewNum(0);
            sf.setDownNum(0);
            sf.setShareTime(LocalDateTime.now());
            Long no = DbUtil.queryLong("select max(`no`) as `max_no` from `share_file`", ShareFile.class);
            no++;
            sf.setNo(no.intValue());
            Assert.isTrue(DbUtil.insertObject(sf), "分享失败");
        }
        return R.ok(null, "分享成功");
    }

    @Action(val = "hasShare", type = 1)
    @Login(val = false)
    public R hasShare(Dict dict) {
        String code = dict.getStr("code");
        Assert.notBlank(code, "编码不存在");
        long count = DbUtil.queryLong("select count(0) from share_file where code = ?", ShareFile.class, code);
        if (count > 0) {
            return R.ok();
        }
        return R.failed();
    }

    @Action(val = "shareData", type = 1)
    @Login(val = false)
    public R shareData(Dict dict) {
        String code = dict.getStr("code");
        Assert.notBlank(code, "编码不存在");
        String password = dict.getStr("password");
        ShareFile shareData = DbUtil.queryObject("select * from share_file where code = ?", ShareFile.class, code);
        if (shareData == null) {
            return R.failed("当前分享不存在");
        }
        if (shareData.getExpireTime() != null && shareData.getExpireTime().isAfter(LocalDate.of(2000, 1, 1))) {
            if (LocalDate.now().isAfter(shareData.getExpireTime())) {
                return R.failed("当前分享已过期");
            }
        }
        if (StrUtil.isNotBlank(shareData.getPassword())) {
            if (!StrUtil.equals(password, shareData.getPassword())) {
                return R.okData(Dict.create().set("type", -1));
            }
        }
        return R.okData(Dict.create().set("type", 1).set("data", shareData));
    }

    @Action(val = "list", type = 1)
    public R list(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        int currentPage = param.getInt("current");
        Integer pageSize = param.getInt("pageSize");
        String where = " where 1=1";
        List<Object> params = new ArrayList<>();
        where += " and user_id = ?";
        params.add(user.getId());
        CommonBean.PageRes<ShareFile> data = DbUtil.pageObject("select *", "from share_file" + where, ShareFile.class, currentPage, pageSize, params.toArray());
        data.getData().forEach(shareFile -> {
            String[] sz = shareFile.getFiles().split(";");
            CommonBean.PathInfo info = null;
            String parent = "{sio:" + shareFile.getNo() + "}";
            try{
                boolean dirMode = true;
                if (sz.length == 1) {
                    FileSystemInface.PlainPath path = FileSystemUtil.cipherPath2PlainPathByLogin(parent + "/" + shareFile.getFiles(), "", "");
                    info = FileSystemUtil.ACTION.fileInfo(path);
                    if (info != null) {
                        dirMode = false;
                        info.setPath(parent + "/" + info.getPath());
                    }
                }
                if (dirMode) {
                    info = new CommonBean.PathInfo();
                    info.setName(shareFile.getName());
                    info.setPath(parent);
                    info.setCreatedAt(shareFile.getShareTime().toString());
                    info.setType(2);
                }
            }catch (Exception e){
                info = new CommonBean.PathInfo();
                info.setName("此文件可能丢失,建议删除");
                info.setPath(parent);
                info.setCreatedAt(shareFile.getShareTime().toString());
                info.setType(2);
            }
            shareFile.setFiles(JSONUtil.parse(info).toString());
        });
        return R.ok(data, "获取成功");
    }

    @Action(val = "dels", type = 1)
    public R dels(List<String> ids) {
        Assert.notEmpty(ids, "当前数据不存在");
        SysUser user = LoginAuthUtil.getUser();
        String idsStr = StrUtil.format("'{}'", CollUtil.join(ids, "','"));
        String where = "";
        List<Object> params = new ArrayList<>();
        where += " and user_id = ?";
        params.add(user.getId());
        DbUtil.delete("delete from share_file where id in ( " + idsStr + " )" + where, ShareFile.class, params.toArray());
        return R.ok();
    }

    @Action(val = "info", type = 1)
    public R info(Dict param) {
        String id = param.getStr("id");
        return R.okData(DbUtil.queryObject("select * from share_file where id = ? and user_id = ?", ShareFile.class, id, LoginAuthUtil.getUser().getId()));
    }

    @Action(val = "findOne", type = 1)
    public R findOne(Dict param) {
        String path = param.getStr("path");
        String files = param.getStr("files");
        ShareFile shareFile = DbUtil.queryObject("select * from share_file where user_id = ? and path = ? and files = ?",
                ShareFile.class, LoginAuthUtil.getUser().getId(),
                path, files);
        if (shareFile != null) {
            return R.okData(shareFile.getId());
        }
        return R.failed();
    }
}

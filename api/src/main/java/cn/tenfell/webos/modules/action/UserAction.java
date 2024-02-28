package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.tenfell.webos.common.annt.*;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.modules.entity.IoDrive;
import cn.tenfell.webos.modules.entity.IoUserDrive;
import cn.tenfell.webos.modules.entity.SysConfig;
import cn.tenfell.webos.modules.entity.SysUser;
import org.noear.solon.core.handle.Context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BeanAction(val = "user")
public class UserAction {
    public static String getUserName(String userId) {
        return DbUtil.queryString("select username from sys_user where id = ?", SysUser.class, userId);
    }

    /**
     * 创建子用户
     *
     * @param param
     * @return
     */
    @Action(val = "createChild", type = 1)
    @Auth(val = "main_auth")
    public R createChild(SysUser param) {
        Assert.notBlank(param.getUsername(), "用户名不可为空");
        if (LoginAuthUtil.isSystem()) {
            Assert.notBlank(param.getParentUserNo(), "主用户编号不可为空");
        } else {
            SysUser loginUser = LoginAuthUtil.getUser();
            param.setParentUserNo(loginUser.getParentUserNo());
        }
        long count = DbUtil.queryLong("select count(0) from sys_user where username = ? and parent_user_no = ?", SysUser.class, param.getUsername(), param.getParentUserNo());
        Assert.isTrue(count < 1, "当前用户已存在");
        param.setPassword(ProjectUtil.startConfig.getStr("defaultPwd"));
        createChildUser(param);
        return R.ok(null, "创建子用户成功");
    }

    private CommonBean.AccessToken createChildUser(SysUser user) {
        user.setId(IdUtil.fastSimpleUUID());
        user.setPassword(LoginAuthUtil.encodePassword(user.getPassword()));
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        user.setUserType(2);
        user.setIsAdmin(2);
        Assert.isTrue(DbUtil.insertObject(user), "子用户创建失败");
        CommonBean.AccessToken token = LoginAuthUtil.createToken(user.getId(), user.getPassword(),null, user.getUserType());
        return token;
    }

    public static synchronized SysUser createMain(String username, String password, Integer isAdmin) {
        Assert.notBlank(username, "用户名不可为空");
        long count = DbUtil.queryLong("select count(0) from sys_user where username = ? and user_type = 1", SysUser.class, username);
        Assert.isTrue(count < 1, "当前用户已存在");
        Long maxNo = DbUtil.queryLong("select max(parent_user_no) from sys_user where user_type = 1", SysUser.class);
        if (maxNo == null || maxNo == 0) {
            maxNo = 10000L;
        }
        Long parentUserNo = maxNo + 1L;
        SysUser user = new SysUser();
        user.setId(IdUtil.fastSimpleUUID());
        user.setUsername(username);
        user.setPassword(LoginAuthUtil.encodePassword(password));
        user.setParentUserNo(Convert.toStr(parentUserNo));
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        user.setUserType(1);
        user.setValid(1);
        user.setIsAdmin(isAdmin);
        Assert.isTrue(DbUtil.insertObject(user), "创建主用户失败");
        SysConfig config = new SysConfig();
        config.setId(IdUtil.fastSimpleUUID());
        config.setParentUserId(user.getId());
        config.setOpenReg(0);
        Assert.isTrue(DbUtil.insertObject(config), "主用户配置写入失败");
        return user;
    }

    /**
     * 创建主用户
     *
     * @param param
     * @return
     */
    @Action(val = "createMain", type = 1)
    @Transactional
    @Auth(val = "system")
    public R createMain(SysUser param) {
        createMain(param.getUsername(), ProjectUtil.startConfig.getStr("defaultPwd"), 2);
        return R.ok(null, "主用户创建成功");
    }

    /**
     * 子用户注册
     *
     * @param param
     * @return
     */
    @Action(val = "reg", type = 1)
    @Login(val = false)
    public R reg(SysUser param) {
        Assert.notBlank(param.getUsername(), "用户名不可为空");
        Assert.notBlank(param.getPassword(), "密码不可为空");
        Assert.notBlank(param.getParentUserNo(), "主用户编号不可为空");
        SysUser parentUser = DbUtil.queryObject("select * from sys_user where user_type = 1 and parent_user_no = ? and valid = 1", SysUser.class, param.getParentUserNo());
        Assert.notNull(parentUser, "当前主用户不存在");
        SysConfig config = DbUtil.queryObject("select * from sys_config where parent_user_id = ?", SysConfig.class, parentUser.getId());
        Assert.notNull(config, "当前主用户配置异常");
        Assert.isTrue(config.getOpenReg() != null && config.getOpenReg() == 1, "当前主用户未开放注册");
        long count = DbUtil.queryLong("select count(0) from sys_user where username = ? and parent_user_no = ?", SysUser.class, param.getUsername(), param.getParentUserNo());
        Assert.isTrue(count < 1, "当前用户已被注册");
        param.setValid(1);
        CommonBean.AccessToken token = createChildUser(param);
        return R.ok(token, "注册成功");
    }

    public static CommonBean.AccessToken userLogin(SysUser user){
        Assert.notBlank(user.getUsername(), "用户名不可为空");
        Assert.notBlank(user.getPassword(), "密码不可为空");
        Assert.isTrue(CollUtil.newArrayList(1, 2).contains(user.getUserType()), "用户类型不正确");
        SysUser dbUser;
        if (user.getUserType() == 2) {
            //子用户登录
            Assert.notBlank(user.getParentUserNo(), "主用户编号不可为空");
            dbUser = DbUtil.queryObject("select * from sys_user where user_type = 2 and username = ? and parent_user_no = ?", SysUser.class, user.getUsername(), user.getParentUserNo());
        } else {
            //主用户登录
            dbUser = DbUtil.queryObject("select * from sys_user where user_type = 1 and username = ?", SysUser.class, user.getUsername());
        }
        Assert.notNull(dbUser, "用户名或密码错误");
        Assert.isTrue(LoginAuthUtil.checkPassword(user.getPassword(), dbUser.getPassword()), "用户名或密码错误");
        Assert.isTrue(dbUser.getValid() == 1, "当前用户已禁用");
        CommonBean.AccessToken token = LoginAuthUtil.createToken(dbUser.getId(), dbUser.getPassword(),null, dbUser.getUserType());
        return token;
    }

    /**
     * 用户登录
     *
     * @param user
     * @return
     */
    @Action(val = "login", type = 1)
    @Login(val = false)
    public R login(SysUser user) {
        return R.ok(userLogin(user), "登录成功");
    }

    /**
     * 解锁
     *
     * @param param
     * @return
     */
    @Action(val = "loginByLock", type = 1)
    @Login(val = false)
    public R loginByLock(Dict param) {
        String userId = param.getStr("userId");
        String password = param.getStr("password");
        Assert.notBlank(userId, "用户Id不可为空");
        Assert.notBlank(password, "密码不可为空");
        SysUser dbUser = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, userId);
        Assert.notNull(dbUser, "此用户不存在");
        if (StrUtil.isNotBlank(dbUser.getSpPassword())) {
            Assert.isTrue(LoginAuthUtil.checkPassword(password, dbUser.getPassword()) || LoginAuthUtil.checkPassword(password, dbUser.getSpPassword()), "密码错误");
        } else {
            Assert.isTrue(LoginAuthUtil.checkPassword(password, dbUser.getPassword()), "密码错误");
        }
        Assert.isTrue(dbUser.getValid() == 1, "当前用户已禁用");
        CommonBean.AccessToken token = LoginAuthUtil.createToken(dbUser.getId(), dbUser.getPassword(),null, dbUser.getUserType());
        return R.ok(token, "登录成功");
    }

    @Action(val = "lock", type = 1)
    public R lock(Dict param) {
        LoginAuthUtil.toLock();
        return R.ok();
    }

    @Action(val = "checkLock", type = 1)
    public R checkLock(Dict param) {
        return LoginAuthUtil.checkLock();
    }

    /**
     * 获取登录信息
     *
     * @param ctx
     * @return
     */
    @Action(val = "info")
    public R info(Context ctx) {
        SysUser user = LoginAuthUtil.getUser();
        user.setPassword("");
        user.setSpPassword("");
        return R.ok(user, "获取成功");
    }

    /**
     * 刷新token
     *
     * @param param
     * @return
     */
    @Action(val = "refreshToken", type = 1)
    @Login(val = false)
    public R refreshToken(Dict param) {
        CommonBean.AccessToken token = LoginAuthUtil.refreshToken(param.getStr("refreshToken"));
        Assert.notNull(token, "刷新token失败");
        return R.ok(token, "刷新token成功");
    }


    /**
     * 获取用户信息
     *
     * @param data
     * @return
     */
    @Action(val = "infoById", type = 1)
    @Auth(val = "main_auth")
    public R infoById(SysUser data) {
        SysUser db = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, data.getId());
        Assert.notNull(db, "当前记录不存在");
        if (!LoginAuthUtil.isSystem()) {
            SysUser user = LoginAuthUtil.getUser();
            Assert.isTrue(StrUtil.equals(db.getParentUserNo(), user.getParentUserNo()), "当前记录不存在");
        }
        db.setPassword("");
        db.setSpPassword("");
        return R.ok(db, "获取成功");
    }

    /**
     * 获取用户列表
     * 主用户专享
     */
    @Action(val = "list", type = 1)
    @Auth(val = "main_auth")
    public R list(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        String keyword = param.getStr("keyword");
        int currentPage = param.getInt("current");
        Integer pageSize = param.getInt("pageSize");
        String where = " where 1=1";
        List<Object> sqlParams = new ArrayList<>();
        if (!LoginAuthUtil.isSystem()) {
            where += " and parent_user_no = ?";
            sqlParams.add(user.getParentUserNo());
        }
        if (StrUtil.isNotBlank(keyword)) {
            where += " and username like ?";
            sqlParams.add("%" + keyword + "%");
        }
        CommonBean.PageRes<SysUser> data = DbUtil.pageObject("select *", "from sys_user" + where, SysUser.class, currentPage, pageSize, sqlParams.toArray());
        data.getData().forEach(item -> item.setPassword(""));
        return R.ok(data, "获取成功");
    }

    /**
     * 修改用户信息
     * 主用户专享
     */
    @Action(val = "update", type = 1)
    @Auth(val = "main_auth")
    public R update(SysUser param) {
        //用户名,图像,昵称,有效
        Assert.notBlank(param.getId(), "参数不足");
        SysUser user = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, param.getId());
        Assert.notNull(user, "此用户不存在");
        boolean isSystem = LoginAuthUtil.isSystem();
        SysUser loginUser = LoginAuthUtil.getUser();
        if (!isSystem) {
            Assert.isTrue(StrUtil.equals(loginUser.getParentUserNo(), user.getParentUserNo()), "权限不足");
        }
        if (StrUtil.isNotBlank(param.getUsername()) && !StrUtil.equals(user.getUsername(), param.getUsername())) {
            if (!isSystem) {
                Assert.isTrue(!StrUtil.equals(loginUser.getId(), user.getId()), "请联系管理员修改用户名");
            }
            if (user.getUserType() == 1) {
                //主用户
                long count = DbUtil.queryLong("select count(0) from sys_user where user_type = 1 and username = ? and id != ?", SysUser.class, param.getUsername(), user.getId());
                Assert.isTrue(count < 1, "当前用户名被占用");
            } else {
                //子用户
                long count = DbUtil.queryLong("select count(0) from sys_user where user_type = 2 and username = ? and parent_user_no = ? and id != ?", SysUser.class, param.getUsername(), user.getParentUserNo(), user.getId());
                Assert.isTrue(count < 1, "当前用户名被占用");
            }
            user.setUsername(param.getUsername());
        }
        user.setImgPath(param.getImgPath());
        user.setNickName(param.getNickName());
        user.setValid(param.getValid());
        boolean flag = DbUtil.updateObject(user, Entity.create().set("id", param.getId())) > 0;
        Assert.isTrue(flag, "操作失败");
        return R.ok();
    }

    /**
     * 修改当前登录用户密码
     */
    @Action(val = "updatePassword", type = 1)
    public R updatePassword(Dict param) {
        String oldPwd = param.getStr("oldPassword");
        Assert.notBlank(oldPwd, "旧密码不可为空");
        String password = param.getStr("password");
        Assert.notBlank(oldPwd, "新密码不可为空");
        SysUser user = LoginAuthUtil.getUser();
        SysUser db = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, user.getId());
        Assert.isTrue(LoginAuthUtil.checkPassword(oldPwd, db.getPassword()), "旧密码不正确");
        db.setPassword(LoginAuthUtil.encodePassword(password));
        db.setUpdatedTime(LocalDateTime.now());
        boolean flag = DbUtil.updateObject(db, Entity.create().set("id", db.getId())) > 0;
        Assert.isTrue(flag, "修改失败");
        return R.ok();
    }

    /**
     * 重置用户密码
     */
    @Action(val = "resetPassword", type = 1)
    @Auth(val = "main_auth")
    public R resetPassword(SysUser param) {
        Assert.notBlank(param.getId(), "参数不足");
        SysUser db = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, param.getId());
        Assert.notNull(db, "此用户不存在");
        if (!LoginAuthUtil.isSystem()) {
            SysUser user = LoginAuthUtil.getUser();
            Assert.isTrue(StrUtil.equals(user.getParentUserNo(), db.getParentUserNo()), "权限不足");
        }
        db.setPassword(LoginAuthUtil.encodePassword(ProjectUtil.startConfig.getStr("defaultPwd")));
        db.setUpdatedTime(LocalDateTime.now());
        boolean flag = DbUtil.updateObject(db, Entity.create().set("id", db.getId())) > 0;
        Assert.isTrue(flag, "修改失败");
        return R.ok();
    }

    /**
     * 更新用户信息
     *
     * @param param
     * @return
     */
    @Action(val = "updateInfo", type = 1)
    public R updateInfo(SysUser param) {
        SysUser user = LoginAuthUtil.getUser();
        if (StrUtil.isNotBlank(param.getUsername())) {
            if (user.getUserType() == 1) {
                //主用户
                long count = DbUtil.queryLong("select count(0) from sys_user where user_type = 1 and username = ? and id != ?", SysUser.class, param.getUsername(), user.getId());
                Assert.isTrue(count < 1, "当前用户名被占用");
            } else {
                //子用户
                long count = DbUtil.queryLong("select count(0) from sys_user where user_type = 2 and username = ? and parent_user_no = ? and id != ?", SysUser.class, param.getUsername(), user.getParentUserNo(), user.getId());
                Assert.isTrue(count < 1, "当前用户名被占用");
            }
            user.setUsername(param.getUsername());
        }
        user.setNickName(param.getNickName());
        user.setImgPath(param.getImgPath());
        if (StrUtil.isNotBlank(param.getSpPassword())) {
            user.setSpPassword(LoginAuthUtil.encodePassword(param.getSpPassword()));
        }
        boolean flag = DbUtil.updateObject(user, Entity.create().set("id", user.getId())) > 0;
        Assert.isTrue(flag, "修改失败");
        return R.ok();
    }


    /**
     * 获取下拉框
     */
    @Action(val = "select")
    @Auth(val = "main_auth")
    public R select() {
        List<SysUser> list = selectData();
        return R.ok(list, "获取成功");
    }

    private List<SysUser> selectData() {
        SysUser loginUser = LoginAuthUtil.getUser();
        return DbUtil.queryList("select * from sys_user where parent_user_no = ?", SysUser.class, loginUser.getParentUserNo());
    }

    /**
     * 获取下拉框Map
     */
    @Action(val = "selectMap")
    @Auth(val = "main_auth")
    public R selectMap() {
        List<SysUser> list = selectData();
        Map<String, String> map = new HashMap<>();
        for (SysUser user : list) {
            map.put(user.getId(), user.getUsername() + "(" + user.getNickName() + ")");
        }
        return R.ok(map, "获取成功");
    }

    @Action(val = "del",type = 1)
    @Auth(val = "main_auth")
    public R del(SysUser data) {
        SysUser db = DbUtil.queryObject("select * from sys_user where id = ?",SysUser.class,data.getId());
        if(db == null){
            return R.ok();
        }
        SysUser loginUser = LoginAuthUtil.getUser();
        if(!LoginAuthUtil.isSystem()){
            Assert.isTrue(StrUtil.equals(loginUser.getParentUserNo(),db.getParentUserNo()),"权限不足");
        }
        Assert.isTrue(!StrUtil.equals(loginUser.getId(),db.getId()),"自己不能删除自己");
        if(db.getUserType() == 1){
            //主用户,删除IoDrive
            List<IoDrive> list = DbUtil.queryList("select * from io_drive where parent_user_id = ?", IoDrive.class, db.getId());
            IoDriveAction.delIoDriveByIds(list);
        }else if(db.getUserType() == 2){
            //子用户,删除IoUserDrive
            List<IoUserDrive> list = DbUtil.queryList("select * from io_user_drive where user_id = ?", IoUserDrive.class, db.getId());
            IoUserDriveAction.delIoUserDriveByIds(list);
        }else{
            Assert.isTrue(false,"此用户类型不存在");
        }
        DbUtil.delete("delete from sys_user where id = ?", SysUser.class,db.getId());
        return R.ok(null,"删除成功");
    }

    @Action(val = "defaultPwd",type = 1)
    @Auth(val = "main_auth")
    public R defaultPwd(Dict dict) {
        return R.okData(ProjectUtil.startConfig.getStr("defaultPwd"));
    }
}

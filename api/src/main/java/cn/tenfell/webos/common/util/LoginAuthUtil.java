package cn.tenfell.webos.common.util;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.modules.entity.SysMenu;
import cn.tenfell.webos.modules.entity.SysRoleMenu;
import cn.tenfell.webos.modules.entity.SysUser;
import cn.tenfell.webos.modules.entity.SysUserRole;
import lombok.Data;

import java.util.List;

public class LoginAuthUtil {
    //主要用于异步线程中获得用户信息
    public static ThreadLocal<SysUser> USER_LOCAL = new ThreadLocal<>();
    public static String userCacheSql = "select * from sys_user where id = ? and valid = 1";
    private static JwtKeyCache keyCache;

    private static List<String> getJwtKeys() {
        if (keyCache == null || keyCache.getCreateTime() == null || System.currentTimeMillis() - keyCache.getCreateTime() > 10 * 60 * 1000) {
            if (keyCache == null) {
                keyCache = new JwtKeyCache();
            }
            long eachTwo = System.currentTimeMillis() / 1000 / 60 / 60 / 2;
            int expireTime = 7200;
            String key1 = CacheUtil.getCacheData("jwt:key-" + eachTwo, IdUtil::simpleUUID, expireTime);
            String key2 = CacheUtil.getCacheData("jwt:key-" + (eachTwo + 1L), IdUtil::simpleUUID, expireTime * 2);
            keyCache.setKeys(CollUtil.newArrayList(key1, key2));
            keyCache.setCreateTime(System.currentTimeMillis());
        }
        return keyCache.getKeys();
    }

    public static CommonBean.AccessToken createToken(String userId,String pwd ,String haw, Integer userType) {
        if(StrUtil.isBlank(haw)){
            haw = createPageCheck(pwd);
            Assert.notBlank(haw,"请刷新页面重试");
        }
        JSONObject mapObj = new JSONObject();
        mapObj.set("userId", userId);
        mapObj.set("userType", userType);
        mapObj.set("haw", haw);
        Long expireTime = System.currentTimeMillis() + 7200 * 1000L;
        mapObj.set("expireTime", expireTime);
        List<String> keys = getJwtKeys();
        String token = createTokenStr(mapObj, keys.get(1));
        String uuid = IdUtil.fastSimpleUUID();
        CacheUtil.setValue("jwt:user-" + uuid, mapObj, 7200);
        CacheUtil.delValue("jwt:user_lock:" + userId);
        CacheUtil.setValue("jwt:user-page-id-"+userId,haw,14400);
        return CommonBean.AccessToken.builder().webosToken(token).expireTime(expireTime).refreshToken(uuid).build();
    }

    private static String createTokenStr(JSONObject mapObj, String key) {
        String str = Base64Encoder.encode(mapObj.toString());
        String str2 = SecureUtil.sha1(str + key);
        return str + "." + str2;
    }

    private static boolean verifyToken(String token, String key) {
        String[] tokenSz = token.split("\\.");
        if (tokenSz.length < 2) {
            return false;
        }
        String str = tokenSz[0];
        String str2 = tokenSz[1];
        String str2p = SecureUtil.sha1(str + key);
        return str2p.equals(str2);
    }

    public static SysUser getUserByToken(String token) {
        List<String> keys = getJwtKeys();
        if (verifyToken(token, keys.get(1)) || verifyToken(token, keys.get(0))) {
            //说明是合法的
        } else {
            return null;
        }
        JSONObject mapObj = JSONUtil.parseObj(Base64Decoder.decodeStr(token.split("\\.")[0]));
        Long expireTime = mapObj.getLong("expireTime");
        if (System.currentTimeMillis() > expireTime) {
            return null;
        }
        String userId = mapObj.getStr("userId");
        String haw = mapObj.getStr("haw");
        SysUser cacheUser = DbUtil.queryObject(userCacheSql, SysUser.class, userId);
        if (cacheUser == null) {
            return null;
        }
        String cacheHaw = CacheUtil.getValue("jwt:user-page-id-"+userId);
        if(StrUtil.isBlank(cacheHaw)){
            return null;
        }
        if(!StrUtil.equals(cacheHaw,haw)){
            return null;
        }
        String pageId = createPageCheck(cacheUser.getPassword());
        if(StrUtil.isNotBlank(pageId)){
            if(!StrUtil.equals(cacheHaw,pageId)){
                return null;
            }
        }
        return cacheUser;
    }

    public static R checkLock() {
        SysUser user = getUser();
        if (user != null) {
            String lock = CacheUtil.getValue("jwt:user_lock:" + user.getId());
            if (StrUtil.equals(lock, "1")) {
                //此用户已锁定
                return R.lock();
            }
        }
        return R.ok();
    }

    public static SysUser getUser() {
        SysUser user = USER_LOCAL.get();
        if (user == null) {
            try {
                String token = ProjectUtil.getContext().getCtx().header("webos-token");
                if (StrUtil.isBlank(token)) {
                    return null;
                }
                user = getUserByToken(token);
            } catch (Exception e) {
            }
        }
        if (user == null) {
            return null;
        }
        return user;
    }

    public static boolean isSystem() {
        List<String> auths = getUserAuths();
        if (CollUtil.isEmpty(auths)) {
            return false;
        }
        return auths.contains("system");
    }

    public static boolean isMain() {
        List<String> auths = getUserAuths();
        if (CollUtil.isEmpty(auths)) {
            return false;
        }
        return auths.contains("main_auth");
    }

    public static List<String> getUserAuths() {
        SysUser user = getUser();
        if (user == null) {
            return null;
        }
        if (true) {
            //因不考虑角色关系,所以用户只分主用户和子用户
            List<String> list = CollUtil.newArrayList(user.getUserType() == 1 ? "main_auth" : "child_auth");
            if (user.getUserType() == 1 && user.getIsAdmin() == 1) {
                list.add("system");
            }
            return list;
        }
        List<SysUserRole> sysUserRoles = DbUtil.queryList("select * from sys_user_role where user_id = ?", SysUserRole.class, user.getId());
        if (CollUtil.isEmpty(sysUserRoles)) {
            return null;
        }
        List<String> roleIds = CollUtil.getFieldValues(sysUserRoles, "roleId", String.class);
        String roleIdsStr = "'" + CollUtil.join(roleIds, "','") + "'";
        List<SysRoleMenu> sysRoleMenus = DbUtil.queryList("select * from sys_role_menu where role_id in ( " + roleIdsStr + " )", SysRoleMenu.class);
        if (CollUtil.isEmpty(sysRoleMenus)) {
            return null;
        }
        List<String> menuIds = CollUtil.getFieldValues(sysRoleMenus, "menuId", String.class);
        String menuIdsStr = "'" + CollUtil.join(menuIds, "','") + "'";
        List<SysMenu> menus = DbUtil.queryList("select * from sys_menu where id in ( " + menuIdsStr + " )", SysMenu.class);
        if (CollUtil.isEmpty(menus)) {
            return null;
        }
        return CollUtil.getFieldValues(menus, "authKey", String.class);
    }

    private static String createPageCheck(String userPwd){
        String pageId = ProjectUtil.getContext().getCtx().header("page-id");
        if(StrUtil.isBlank(pageId)){
            return ProjectUtil.getContext().getCtx().attr("page-id");
        }
        return SecureUtil.md5(pageId+userPwd);
    }
    public static CommonBean.AccessToken refreshToken(String refreshToken) {
        JSONObject mapObj = CacheUtil.getValue("jwt:user-" + refreshToken);
        if (mapObj == null) {
            return null;
        }
        String userId = mapObj.getStr("userId");
        String haw = mapObj.getStr("haw");
        return createToken(userId, null,haw, mapObj.getInt("userType"));
    }

    final private static String PASSWORD_KEY = "kCK!EKRhjli0d0ce";

    public static String encodePassword(String password) {
        String sjs = RandomUtil.randomString(6);
        String ep = SecureUtil.hmacSha256(PASSWORD_KEY + sjs).digestBase64(password, false);
        return ep + "." + sjs;
    }

    public static boolean checkPassword(String password, String encodePassword) {
        String[] mmsz = encodePassword.split("\\.");
        String sjs = mmsz[1];
        String ep = SecureUtil.hmacSha256(PASSWORD_KEY + sjs).digestBase64(password, false);
        return encodePassword.equals(ep + "." + sjs);
    }

    public static void toLock() {
        SysUser user = LoginAuthUtil.getUser();
        if (user != null) {
            CacheUtil.setValue("jwt:user_lock:" + user.getId(), "1", 4 * 60 * 60);
        }
    }

    @Data
    private static class JwtKeyCache {
        private Long createTime;
        private List<String> keys;
    }
}

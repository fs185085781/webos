package cn.tenfell.webos.common.webdav;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.util.CacheUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.webdav.sf.*;
import cn.tenfell.webos.common.webdav.sf.exceptions.UnauthenticatedException;
import cn.tenfell.webos.modules.action.UserAction;
import cn.tenfell.webos.modules.entity.SysUser;

import java.io.InputStream;
import java.security.Principal;
import java.util.List;

public class WebosStore implements IWebdavStore {
    @Override
    public void destroy() {
        System.out.println("destroy");
    }

    @Override
    public ITransaction begin(Principal principal, HttpServletRequest req, HttpServletResponse resp) {
        return new Transaction(principal,req,resp);
    }

    @Override
    public void checkAuthentication(ITransaction transaction) {
        if(transaction.getPrincipal() == null){
            throw new UnauthenticatedException(401);
        }
        String userPwd = transaction.getPrincipal().getName();
        if(StrUtil.isBlank(userPwd)){
            throw new UnauthenticatedException(401);
        }
        SysUser user = CacheUtil.getValue("webdav:login:"+userPwd);
        boolean login = false;
        if(user == null){
            String[] userPwds = Base64.decodeStr(userPwd).split(":",3);
            SysUser param = new SysUser();
            if(userPwds.length == 2){
                //主账户
                param.setUsername(userPwds[0]);
                param.setPassword(userPwds[1]);
                param.setUserType(1);
            }else if(userPwds.length == 3){
                //子账户
                param.setParentUserNo(userPwds[0]);
                param.setUsername(userPwds[1]);
                param.setPassword(userPwds[2]);
                param.setUserType(2);
            }else{
                throw new UnauthenticatedException(401);
            }
            CommonBean.AccessToken token;
            try{
                token = UserAction.userLogin(param);
            }catch (Exception e){
                throw new UnauthenticatedException(401);
            }
            if(token == null){
                throw new UnauthenticatedException(401);
            }
            String accessToken = token.getWebosToken();
            user = LoginAuthUtil.getUserByToken(accessToken);
            if(user == null){
                throw new UnauthenticatedException(401);
            }
            CacheUtil.setValue("webdav:login:"+userPwd,user,60*60*24*7);
            CacheUtil.setValue("webdav:token:"+userPwd,token,60*60*4);
            login = true;
        }
        if(!login){
            CommonBean.AccessToken token = CacheUtil.getValue("webdav:token:"+userPwd);
            if(token != null && System.currentTimeMillis() >= token.getExpireTime() - 15*60*1000){
                token = LoginAuthUtil.refreshToken(token.getRefreshToken());
                if(token != null){
                    CacheUtil.setValue("webdav:login:"+userPwd,user,60*60*24*7);
                    CacheUtil.setValue("webdav:token:"+userPwd,token,60*60*4);
                }else{
                    CacheUtil.delValue("webdav:token:"+userPwd);
                }
            }
        }
        LoginAuthUtil.USER_LOCAL.set(user);
    }

    @Override
    public void commit(ITransaction transaction) {
    }

    @Override
    public void rollback(ITransaction transaction) {
    }

    @Override
    public void createFolder(ITransaction transaction, String folderUri) {
        String cnPath = WebdavUtil.cnPath(folderUri);
        WebosWebDavUtil.createFolder(cnPath);
    }

    @Override
    public void createResource(ITransaction transaction, String resourceUri) {
        System.out.println("createResource:"+resourceUri);
    }

    @Override
    public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
        String cnPath = WebdavUtil.cnPath(resourceUri);
        return WebosWebDavUtil.getIn(cnPath);
    }

    @Override
    public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
        String cnPath = WebdavUtil.cnPath(resourceUri);
        return WebosWebDavUtil.putFile(cnPath,content);
    }

    @Override
    public String[] getChildrenNames(ITransaction transaction, String folderUri) {
        String cnPath = WebdavUtil.cnPath(folderUri);
        List<CommonBean.PathInfo> list = WebosWebDavUtil.fileList(cnPath);
        if(CollUtil.isEmpty(list)){
            return new String[0];
        }
        return ArrayUtil.toArray(CollUtil.getFieldValues(list,"name",String.class),String.class);
    }

    @Override
    public long getResourceLength(ITransaction transaction, String path) {
        String cnPath = WebdavUtil.cnPath(path);
        return WebosWebDavUtil.fileLength(cnPath);
    }

    @Override
    public void removeObject(ITransaction transaction, String uri) {
        String cnPath = WebdavUtil.cnPath(uri);
        WebosWebDavUtil.del(cnPath);
    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath) {
        Console.log("moveObject:{};{}",destinationPath,sourcePath);
        return false;
    }

    @Override
    public StoredObject getStoredObject(ITransaction transaction, String uri) {
        String cnPath = WebdavUtil.cnPath(uri);
        CommonBean.PathInfo info = WebosWebDavUtil.fileInfo(cnPath);
        if(info != null){
            StoredObject stored = new StoredObject();
            stored.setFolder(info.getType() == 2);
            if(StrUtil.isNotBlank(info.getUpdatedAt())){
                stored.setLastModified(DateUtil.parseDateTime(info.getUpdatedAt()).toJdkDate());
            }
            if(StrUtil.isNotBlank(info.getCreatedAt())){
                stored.setCreationDate(DateUtil.parseDateTime(info.getCreatedAt()).toJdkDate());
            }
            stored.setResourceLength(info.getSize());
            if(stored.isResource()){
                stored.setMimeType("application/octet-stream");
                if(stored.getResourceLength() == 0){
                    stored.setNullResource(true);
                }
            }else{
                stored.setMimeType("application/octet-stream");
            }
            return stored;
        }
        return null;
    }
}

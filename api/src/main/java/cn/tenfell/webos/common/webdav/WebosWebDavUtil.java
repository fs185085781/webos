package cn.tenfell.webos.common.webdav;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.tenfell.webos.WebOsApp;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.util.CacheUtil;
import cn.tenfell.webos.common.util.CommonUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.webdav.sf.exceptions.AccessDeniedException;
import cn.tenfell.webos.modules.action.FileSystemAction;
import cn.tenfell.webos.modules.action.IoDriveAction;
import cn.tenfell.webos.modules.action.IoUserDriveAction;
import cn.tenfell.webos.modules.entity.IoDrive;
import cn.tenfell.webos.modules.entity.IoUserDrive;
import lombok.Data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WebosWebDavUtil {
    private static CommonBean.PathInfo systemInfo(String path){
        CommonBean.PathInfo info = new CommonBean.PathInfo();
        info.setType(2);
        info.setName(path);
        info.setCreatedAt(DateUtil.now());
        info.setUpdatedAt(info.getCreatedAt());
        return info;
    }
    public static List<CommonBean.PathInfo> fileList(String cnParentPath){
        if(StrUtil.equals(cnParentPath,"")){
            List<CommonBean.PathInfo> infos = CollUtil.newArrayList(systemInfo("此电脑"));
            if(LoginAuthUtil.isMain()){
                infos.add(systemInfo("磁盘管理"));
            }
            return infos;
        }else if(StrUtil.equals(cnParentPath,"此电脑")){
            try{
                CommonBean.PageRes<IoUserDrive> iudPage = IoUserDriveAction.userDriveList(Dict.create().set("current",1).set("pageSize",9999));
                if(iudPage == null || CollUtil.isEmpty(iudPage.getData())){
                    return null;
                }
                List<CommonBean.PathInfo> list = new ArrayList<>();
                for (IoUserDrive one:iudPage.getData()){
                    CommonBean.PathInfo info = new CommonBean.PathInfo();
                    info.setType(2);
                    info.setName(one.getName());
                    info.setCreatedAt(LocalDateTimeUtil.format(one.getCreatedTime(),"yyyy-MM-dd HH:mm:ss"));
                    info.setUpdatedAt(LocalDateTimeUtil.format(one.getUpdatedTime(),"yyyy-MM-dd HH:mm:ss"));
                    info.setPath("{uio:"+one.getNo()+"}");
                    setCnPathCache(cnParentPath+"/"+info.getName(),info.getPath());
                    list.add(info);
                }
                return list;
            }catch (Exception e){

            }
        }else if(StrUtil.equals(cnParentPath,"磁盘管理")){
            try{
                CommonBean.PageRes<IoDrive> idPage = IoDriveAction.mainUserDrive(Dict.create().set("current",1).set("pageSize",9999));
                if(idPage == null || CollUtil.isEmpty(idPage.getData())){
                    return null;
                }
                List<CommonBean.PathInfo> list = new ArrayList<>();
                for (IoDrive one:idPage.getData()){
                    CommonBean.PathInfo info = new CommonBean.PathInfo();
                    info.setType(2);
                    info.setName(one.getName());
                    info.setCreatedAt(LocalDateTimeUtil.format(one.getCreatedTime(),"yyyy-MM-dd HH:mm:ss"));
                    info.setUpdatedAt(LocalDateTimeUtil.format(one.getUpdatedTime(),"yyyy-MM-dd HH:mm:ss"));
                    info.setPath("{io:"+one.getNo()+"}");
                    setCnPathCache(cnParentPath+"/"+info.getName(),info.getPath());
                    list.add(info);
                }
                return list;
            }catch (Exception e){

            }
        }else{
            String webosPath = cnPathToWebosPath(cnParentPath);
            if(StrUtil.isBlank(webosPath)){
               return null;
            }
            try{
                Integer type = 0;
                String next = "";
                List<CommonBean.PathInfo> fileList = new ArrayList<>();
                while(true){
                    Dict param = Dict.create()
                            .set("parentPath",webosPath)
                            .set("type",type)
                            .set("next",next);
                    CommonBean.Page<CommonBean.PathInfo> data = FileSystemAction.fileListPage(param);
                    if(data == null || CollUtil.isEmpty(data.getList())){
                        break;
                    }
                    if(CollUtil.isNotEmpty(data.getList())){
                        fileList.addAll(data.getList());
                    }
                    if(data.getType() == 0){
                        break;
                    }
                    if(StrUtil.isBlank(data.getNext())){
                        break;
                    }
                    type = data.getType();
                    next = data.getNext();
                }
                for(CommonBean.PathInfo info:fileList){
                    setCnPathCache(cnParentPath+"/"+info.getName(),info.getPath());
                }
                return fileList;
            }catch (Exception e){

            }
        }
        return null;
    }
    public static CommonBean.PathInfo fileInfo(String cnPath){
        if(StrUtil.equalsAny(cnPath,"","此电脑","磁盘管理")){
            if(StrUtil.equals(cnPath,"")){
                return systemInfo("根目录");
            }
            return systemInfo(cnPath);
        }
        String webosPath = cnPathToWebosPath(cnPath);
        if(StrUtil.isNotBlank(webosPath)){
            try{
                FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(webosPath, null,null);
                CommonBean.PathInfo info = FileSystemUtil.ACTION.fileInfo(plainPath);
                if(info != null){
                    String[] sz = cnPath.split("/");
                    info.setName(sz[sz.length-1]);
                }
                return info;
            }catch (Exception e){
            }
        }
        return null;
    }
    private static String cnPathToWebosPath(String cnPath){
        return CacheUtil.getValue("webdav:path:"+ LoginAuthUtil.getUser().getId()+":"+ SecureUtil.md5(cnPath));
    }
    private static void setCnPathCache(String cnPath,String webosPath){
        CacheUtil.setValue("webdav:path:"+ LoginAuthUtil.getUser().getId()+":"+ SecureUtil.md5(cnPath),webosPath,60*60*24*2);
    }

    public static InputStream getIn(String cnPath) {
        return FileSystemUtil.ACTION.getInputStream(cnPath2PlainPath(cnPath),0,0);
    }

    public static long putFile(String cnPath, InputStream content) {
        ParentSplit p = cnPath2ParentSplit(cnPath);
        String fId = FileSystemUtil.ACTION.uploadByServer(p.getParentPath(),p.getName(),content,null,null);
        return StrUtil.isNotBlank(fId)?1:0;
    }

    public static void del(String cnPath) {
        try{
            String path = cnPathToWebosPath(cnPath);
            FileSystemInface.PlainPath filePath = FileSystemUtil.cipherPath2PlainPathByLogin(path, null,null);
            CommonBean.PathInfo info = FileSystemUtil.ACTION.fileInfo(filePath);
            if(info == null){
                return;
            }
            String[] sz = path.split("/");
            String current = sz[sz.length-1];
            sz = ArrayUtil.remove(sz, sz.length - 1);
            String parentPath = ArrayUtil.join(sz, "/");
            FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(parentPath, null,null);
            String res = FileSystemUtil.ACTION.remove(plainPath,CollUtil.newArrayList(current),CollUtil.newArrayList(info.getType()));
            Assert.isTrue(StrUtil.equals("1",res));
        }catch (Exception e){
            throw new AccessDeniedException("权限不足");
        }


    }

    public static long fileLength(String cnPath) {
        CommonBean.PathInfo info = FileSystemUtil.ACTION.fileInfo(cnPath2PlainPath(cnPath));
        return info!=null?info.getSize():0;
    }

    public static void createFolder(String cnPath) {
        ParentSplit p = cnPath2ParentSplit(cnPath);
        FileSystemUtil.ACTION.createDir(p.getParentPath(),p.getName());
    }
    private static ParentSplit cnPath2ParentSplit(String cnPath){
       try{
           String[] sz = cnPath.split("/");
           String name = sz[sz.length-1];
           sz = ArrayUtil.remove(sz, sz.length - 1);
           String cnParentPath = ArrayUtil.join(sz, "/");
           String path = cnPathToWebosPath(cnParentPath);
           FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(path, null,null);
           ParentSplit p = new ParentSplit();
           p.setParentPath(plainPath);
           p.setName(name);
           return p;
       }catch (Exception e){
           throw new AccessDeniedException("权限不足");
       }
    }
    private static FileSystemInface.PlainPath cnPath2PlainPath(String cnPath){
        try{
            String webosPath = cnPathToWebosPath(cnPath);
            FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(webosPath, null,null);
            return plainPath;
        }catch (Exception e){
            throw new AccessDeniedException("权限不足");
        }
    }
    @Data
    private static class ParentSplit{
        private String name;
        private FileSystemInface.PlainPath parentPath;
    }
}

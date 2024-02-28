package cn.tenfell.webos.common.bean;

import cn.hutool.db.Entity;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class CommonBean {
    @Data
    public static class PageRes<T> implements Serializable {
        private List<T> data;
        private long count;
        private long pages;
    }

    @Data
    public static class CopyMoveInfo implements Serializable {
        private PathInfo info;
        private String cipherPath;

        public static CopyMoveInfo init(PathInfo info, String cipherPath) {
            CopyMoveInfo cmInfo = new CopyMoveInfo();
            cmInfo.setInfo(info);
            cmInfo.setCipherPath(cipherPath);
            return cmInfo;
        }
    }


    @Data
    public static class PathInfo implements Serializable {
        private String name;//例如  图片.jpg
        private String path;//picture_id  //第三方云盘的id,本地盘的名称
        private String createdAt;
        private String updatedAt;
        private long size;
        private int type;//1文件 2目录
        private String thumbnail;//缩略图
        private String ext;//拓展名
        private String hash;//sha1数据
        private String md5;//md5数据
    }

    @Data
    public static class Page<T> implements Serializable {
        //分页类型0不分页,1分页
        private int type;//必填
        private List<T> list;//必填
        private String next;//下一页参数
    }

    @Data
    @Builder
    public static class AccessToken implements Serializable {
        private String webosToken;
        private Long expireTime;
        private String refreshToken;
    }

    @Data
    public static class SoftStore {
        private String id;
        private String firstCat;
        private String secondCat;
        private String imgPath;
        private String name;
        private String code;
        private String descr;
        private String screenShots;
        private Integer score1;
        private Integer score2;
        private Integer score3;
        private Integer score4;
        private Integer score5;
        private Integer ratings;
        private Integer type;
        private String downloadUrl;
        private String version;
        private String author;
        private String effect;
        private String average;
        private String iframeUrl;
        //1本地软件 2远程软件
        private Integer isLocal;
    }

    @Data
    public static class WpsUrlData {
        private String url;
        private String token;
        //1支持0不支持2非阿里云盘
        private Integer type;
        //是否是编辑
        private boolean edit;
        //type=2的时候才有意义,存储wps专用的fileId
        private String fileId;
        private boolean hasFileAuth;
    }

    @Data
    public static class OfficeUrlData {
        private String url;
        private LocalDateTime expireTime;//时间戳
        //返回 1.文件链接  2.分享链接 3.扫码+阿里云 4.阿里云
        private Integer type;
        private Integer coordinationVal;

        public static OfficeUrlData init(String url, LocalDateTime expireTime, Integer type, Integer coordinationVal) {
            OfficeUrlData that = new OfficeUrlData();
            that.setUrl(url);
            that.setType(type);
            that.setExpireTime(expireTime);
            that.setCoordinationVal(coordinationVal);
            return that;
        }

        public static OfficeUrlData init(Integer type) {
            OfficeUrlData that = new OfficeUrlData();
            that.setType(type);
            return that;
        }
    }

    @Data
    public static class TransmissionFile implements Serializable {
        //{source:"{uio:1}/1.txt",path:"{uio:2}/path",name:"test/1.txt"}
        //源文件
        private String source;
        //目标目录
        private String path;
        //目标文件
        private String name;
        //文件名
        private String fileName;
        //缩略图
        private String thumbnail;
        //文件大小
        private long size;
    }

    @Data
    public static class CopyMoveFile implements Serializable {
        private String sourceName;
        private String targetName;
        private String currentFileName;
        private double sd;//kb/s
        private double jd;//0.1等于10%
        private long loaded;//已上传量
        private long size;//总量
        private int status;//1执行中2执行成功3部分失败4前端实现
        private String exp;
    }

    public static class BaseEntity implements Serializable {
        public Entity toEntity() {
            return Entity.parseWithUnderlineCase(this);
        }
    }
}

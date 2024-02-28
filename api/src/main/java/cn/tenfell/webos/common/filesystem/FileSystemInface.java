package cn.tenfell.webos.common.filesystem;


import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.modules.entity.IoTokenData;
import cn.tenfell.webos.modules.entity.IoUserRecycle;
import lombok.Data;
import org.noear.solon.core.handle.Context;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

/**
 * 文件系统接口
 */
public interface FileSystemInface {
    /**
     * 复制文件或者目录
     * 批量
     *
     * @param sourceParent 源文件或者目录 例如aliyundrive/userid/root/626cf8b5abbaf1e4e9e141ea834f6a81c837ef2a/626cf8cb52d147efb3954b85b5888abd6772a0fb
     * @param path         目标目录 aliyundrive/userid/root  则结果为aliyundrive/userid/root/626cf8cb52d147efb3954b85b5888abd6772a0fb
     * @return 1成功 0失败 其他taskId
     */
    String copy(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path);

    /**
     * 移动文件或者目录
     * 批量
     *
     * @param sourceParent 源文件或者目录 例如aliyundrive/userid/root/626cf8b5abbaf1e4e9e141ea834f6a81c837ef2a/626cf8cb52d147efb3954b85b5888abd6772a0fb
     * @param path         目标目录 aliyundrive/userid/root  则结果为aliyundrive/userid/root/626cf8cb52d147efb3954b85b5888abd6772a0fb
     * @return 1成功 0失败 其他taskId
     */
    String move(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, FileSystemInface.PlainPath path);

    /**
     * 重命名文件或者目录
     *
     * @param source 源文件或者目录 例如aliyundrive/userid/root/626cf8b5abbaf1e4e9e141ea834f6a81c837ef2a/626cf8cb52d147efb3954b85b5888abd6772a0fb
     * @param name   名称
     * @return 0失败 1成功
     */
    boolean rename(FileSystemInface.PlainPath source, String name, Integer type);

    /**
     * 上传文件 因第三方数据各个网盘做法不一样,依赖js实现
     * @param path 上传的目录
     * @param name 名称
     * @param expand 拓展数据
     * @return
     * */
    /**
     * 阿里云盘
     * 1.https://api.aliyundrive.com/adrive/v3/file/search 检查是否存在
     * 2.js端进行分块,每块10MB
     * 3.https://api.aliyundrive.com/adrive/v2/file/createWithFolders创建文件并获取1-20包的上传链接,如果md5存在直接秒传
     * 4.https://api.aliyundrive.com/v2/file/get_upload_url获取21-40包的上传链接
     * 5.按照顺序上传包,每上传完获取下20包,直到上传完毕
     * 6.https://api.aliyundrive.com/v2/file/complete执行完成上传,并返回fileId
     * 123网盘
     * 1.https://www.123pan.com/b/api/file/upload_request
     * 2.js端进行分块,每块10MB
     * 3.https://www.123pan.com/b/api/file/upload_request创建文件,如果md5存在直接秒传
     * 4.https://www.123pan.com/b/api/file/s3_repare_upload_parts_batch获取上传分包,第一次4包,其他每次2包
     * 5.按照顺序上传包,每上传完获取下2包,直到上传完毕
     * 6.https://www.123pan.com/a/api/file/s3_complete_multipart_upload批量上传预校验
     * 7.https://www.123pan.com/a/api/file/upload_complete执行完成上传
     */
    /**
     * 预检方法,存在则跳过
     *
     * @param path
     * @param name
     * @param expand
     * @return 返回前端需要的参数
     */
    String uploadPre(FileSystemInface.PlainPath path, String name, String expand);

    /**
     * 获取上传地址
     *
     * @param path
     * @param name
     * @param expand
     * @return 返回前端需要的参数
     */
    String uploadUrl(FileSystemInface.PlainPath path, String name, String expand);

    /**
     * 完成方法执行
     * 分片上传后调用此方法
     *
     * @param path
     * @param name
     * @param expand
     * @return 1.成功  0.失败
     */
    String uploadAfter(FileSystemInface.PlainPath path, String name, String expand);

    /**
     * 后端上传逻辑
     *
     * @param path
     * @param name
     * @param in       二选一
     * @param file     二选一
     * @param consumer 进度条,已上传的值
     * @return 成功返回fileId 失败返回空
     */
    String uploadByServer(FileSystemInface.PlainPath path, String name, InputStream in, File file, Consumer<Long> consumer);

    /**
     * 获取下载地址
     *
     * @param path
     * @return 下载地址
     */
    String downloadUrl(FileSystemInface.PlainPath path);

    /**
     * 新建目录
     *
     * @return 返回fileId
     */
    String createDir(FileSystemInface.PlainPath parentPath, String pathName);

    /**
     * 文件分页数据
     *
     * @param parentPath
     * @param next
     * @return
     */
    CommonBean.Page<CommonBean.PathInfo> listFiles(FileSystemInface.PlainPath parentPath, String next);

    void refreshToken(String driveType, IoTokenData itd);

    /**
     * 删除文件
     *
     * @return 1成功 0失败 其他taskId
     */
    String remove(FileSystemInface.PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes);

    /**
     * 获取地址对应的中文名
     *
     * @param plainPath
     * @return
     */
    String pathName(PlainPath plainPath);

    /**
     * 判断文件是否存在,不存在返回mainName,存在返回可能的mainName名称
     *
     * @param parentPath
     * @param mainName   主名称
     * @param ext
     * @return
     */
    String availableMainName(PlainPath parentPath, String mainName, String ext);

    /**
     * zip解压
     * 1解压成功 0解压失败 2前端实现
     *
     * @param file
     * @return
     */
    String unzip(PlainPath file);

    /**
     * zip压缩
     *
     * @param files
     * @param dirPath
     * @return 1.成功 0.失败 2.前端实现
     */
    String zip(List<PlainPath> files, PlainPath dirPath);

    /**
     * 根据路径获取文件信息
     *
     * @param plainPath
     * @return
     */

    CommonBean.PathInfo fileInfo(PlainPath plainPath);

    /**
     * 获取wps编辑地址
     * 只有阿里云盘返回
     * 其他网盘返回{type:2}即可
     *
     * @param plainPath
     * @param isEdit
     * @return
     */
    CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit);

    /**
     * 根据目录,搜索目录下指定的文件
     *
     * @param parentPath
     * @param name
     * @return
     */
    List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name);

    /**
     * 读取文件部分内容
     * 如果要读取全部,设置length为0
     *
     * @param path
     * @param start
     * @param length
     * @return
     */

    InputStream getInputStream(PlainPath path, long start, long length);

    /**
     * 秒传判断
     *
     * @param path             当前要传输的目录
     * @param name             当前文件要存放的目录 aaa/dd/1.txt
     * @param sourceCipherPath 源文件密文 {sio:1}/path/file
     * @param size             文件长度
     * @return 1秒传成功 0秒传失败
     */
    String secondTransmission(PlainPath path, String name, String sourceCipherPath, long size);

    /**
     * 同盘不同号,自己实现秒传能力
     *
     * @param driveType
     * @param file      包含从哪到哪
     * @return 成功返回fileId 失败返回空
     */
    String sameDriveCopy(String driveType, CommonBean.TransmissionFile file);

    /**
     * 获取文件的sha1
     * 不支持返回空
     * 请不要实时计算
     *
     * @param path
     * @return
     */
    String sha1(PlainPath path);

    /**
     * 获取文件的md5
     * 不支持返回空
     * 请不要实时计算
     *
     * @param path
     * @return
     */
    String md5(PlainPath path);

    @Data
    class PlainPath implements Serializable {
        private String driveType;//硬盘类型
        private String realPath;//真实目录
        private String tokenId;//当前配置数据
        private String cipherPath;//当前密文路径
        private String sioNo;//分享时候的编号
        private String uioNo;//用户io编号
        private String ioNo;//主io编号
        private String realFilePath;//真实文件目录(仅限local文件)
    }

    /**
     * 从回收站恢复
     * 本地网盘,realPath必填
     * 非本地网盘,从path恢复即可
     *
     * @param path
     * @param ioUserRecycle
     * @return
     */

    boolean restore(PlainPath path, IoUserRecycle ioUserRecycle);


    String getRootId(String driveType);

    /**
     * 通用接口,此接口是万能接口
     *
     * @param path
     * @param ctx
     * @return
     */
    R commonReq(PlainPath path, Context ctx);
}


package cn.tenfell.webos.common.filesystem;

import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.modules.entity.IoTokenData;
import cn.tenfell.webos.modules.entity.IoUserRecycle;
import org.noear.solon.core.handle.Context;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * 天翼云盘合作版
 */
public class Pan189hzFileSystem implements FileSystemInface {
    private static final String USER_APP_ID = "8148776600";
    private static final String USER_APP_SECRET = "dRrp4P7QrVeGGbwsNOD4MhBXmBM3N7pR";
    private static final String PAN_APP_KEY = "1130133156402";
    private static final String PAN_APP_SECRET = "e3a44b7a3ce1f2598f57e05dfbff3f0b";

    @Override
    public String copy(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path) {
        return null;
    }

    @Override
    public String move(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes, PlainPath path) {
        return null;
    }

    @Override
    public boolean rename(PlainPath source, String name, Integer type) {
        return false;
    }

    @Override
    public String uploadPre(PlainPath path, String name, String expand) {
        return null;
    }

    @Override
    public String uploadUrl(PlainPath path, String name, String expand) {
        return null;
    }

    @Override
    public String uploadAfter(PlainPath path, String name, String expand) {
        return null;
    }

    @Override
    public String uploadByServer(PlainPath path, String name, InputStream in, File file, Consumer<Long> consumer) {
        return null;
    }

    @Override
    public String downloadUrl(PlainPath path) {
        return null;
    }

    @Override
    public String createDir(PlainPath parentPath, String pathName) {
        return null;
    }

    @Override
    public CommonBean.Page<CommonBean.PathInfo> listFiles(PlainPath parentPath, String next) {
        return null;
    }

    @Override
    public void refreshToken(String driveType, IoTokenData itd) {

    }

    @Override
    public String remove(PlainPath sourceParent, List<String> sourceChildren, List<Integer> sourceTypes) {
        return null;
    }

    @Override
    public String pathName(PlainPath plainPath) {
        return null;
    }

    @Override
    public String availableMainName(PlainPath parentPath, String mainName, String ext) {
        return null;
    }

    @Override
    public String unzip(PlainPath file) {
        return null;
    }

    @Override
    public String zip(List<PlainPath> files, PlainPath dirPath) {
        return null;
    }

    @Override
    public CommonBean.PathInfo fileInfo(PlainPath plainPath) {
        return null;
    }

    @Override
    public CommonBean.WpsUrlData getWpsUrl(PlainPath plainPath, boolean isEdit) {
        return null;
    }

    @Override
    public List<CommonBean.PathInfo> searchFile(PlainPath parentPath, String name) {
        return null;
    }

    @Override
    public InputStream getInputStream(PlainPath path, long start, long length) {
        return null;
    }

    @Override
    public String secondTransmission(PlainPath path, String name, String sourceCipherPath, long size) {
        return null;
    }

    @Override
    public String sameDriveCopy(String driveType, CommonBean.TransmissionFile file) {
        return null;
    }

    @Override
    public String sha1(PlainPath path) {
        return null;
    }

    @Override
    public String md5(PlainPath path) {
        return null;
    }

    @Override
    public boolean restore(PlainPath path, IoUserRecycle ioUserRecycle) {
        return false;
    }

    @Override
    public String getRootId(String driveType) {
        return null;
    }

    @Override
    public R commonReq(PlainPath path, Context ctx) {
        return null;
    }
}

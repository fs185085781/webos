package cn.tenfell.webos.common.util;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.modules.entity.IoTokenData;

import java.time.LocalDateTime;
import java.util.List;

public class TokenDataUtil {
    public static IoTokenData getTokenDataByIdOrToken(String driveType, String idOrToken) {
        if (StrUtil.equals(driveType, FileSystemUtil.LOCAL_DRIVE_TYPE)) {
            return new IoTokenData();
        }
        if (StrUtil.equals(driveType, FileSystemUtil.SERVER_DRIVE_TYPE)) {
            Assert.isTrue(false, "此磁盘不可作为用户盘使用");
        }
        Assert.notBlank(idOrToken, "id或token不可为空");
        IoTokenData itd = DbUtil.queryObject("select * from io_token_data where id = ?", IoTokenData.class, idOrToken);
        if (itd == null) {
            itd = new IoTokenData();
            itd.setTokenData(idOrToken);
            itd.setDriveType(driveType);
            FileSystemUtil.ACTION.refreshToken(driveType, itd);
            Assert.notBlank(itd.getId(), "当前磁盘暂未支持,请联系管理员");
            itd.setErrCount(0);
            DbUtil.upsertObject(itd, "id");
        }
        return itd;
    }

    public static void refreshToken() {
        LocalDateTime now = LocalDateTime.now().plusMinutes(20L);
        List<IoTokenData> list = DbUtil.queryList("select * from io_token_data where expire_time <= ?", IoTokenData.class, now);
        for (IoTokenData itd : list) {
            try {
                if (itd.getErrCount() != null && itd.getErrCount() > 10) {
                    DbUtil.delete("delete from io_token_data where id = ?", IoTokenData.class, itd.getId());
                    continue;
                }

                try {
                    FileSystemUtil.ACTION.refreshToken(itd.getDriveType(), itd);
                } catch (Exception e) {
                    ProjectUtil.showConsoleErr(e);
                    if (itd.getErrCount() == null) {
                        itd.setErrCount(0);
                    }
                    itd.setErrCount(itd.getErrCount() + 1);
                }
                DbUtil.upsertObject(itd, "id");
            } catch (Exception e2) {
                ProjectUtil.showConsoleErr(e2);
            }
        }
    }
}

package cn.tenfell.webos;

import java.util.TimeZone;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpGlobalConfig;
import cn.hutool.log.dialect.console.ConsoleLog;
import cn.hutool.log.level.Level;
import cn.tenfell.webos.common.util.ProjectUtil;

public class WebOsApp {
    public static void main(String[] args) {
        try {
            ConsoleLog.setLevel(Level.INFO);
            SecureUtil.disableBouncyCastle();
            // 设置HttpUtil超时时间为1小时
            HttpGlobalConfig.setTimeout(60 * 60 * 1000);
            // 设置系统时区固定为东八区,真实显示时间以语言切换为主
            final TimeZone timeZone = TimeZone.getTimeZone("GMT+8");
            TimeZone.setDefault(timeZone);
            ProjectUtil.init();
        } catch (Exception e) {
            ProjectUtil.authRestart(e);
        }
    }
}

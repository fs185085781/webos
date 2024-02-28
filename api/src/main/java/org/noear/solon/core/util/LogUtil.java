package org.noear.solon.core.util;

import org.noear.solon.Utils;

public class LogUtil {
    private static boolean toLog = false;
    private static LogUtil global = new LogUtil();
    public static LogUtil global() {
        return global;
    }

    /**
     * 全局打印工具（用于改改日志实现）
     * */
    public static void globalSet(LogUtil instance) {
        if(instance != null) {
            LogUtil.global = instance;
        }
    }

    public void infoAsync(String content) {
        Utils.async(() -> {
            info(content);
        });
    }


    public  void trace(String content) {
        if(!toLog){
            return;
        }
        System.out.print("[Solon] ");
        PrintUtil.greenln(content);
    }

    public  void debug(String content) {
        if(!toLog){
            return;
        }
        System.out.print("[Solon] ");
        PrintUtil.blueln(content);
    }

    public  void info(String content) {
        if(!toLog){
            return;
        }
        System.out.println("[Solon] " + content);
    }

    public  void warn(String content) {
        if(!toLog){
            return;
        }
        System.out.print("[Solon] ");
        PrintUtil.yellowln(content);
    }

    public  void error(String content) {
        if(!toLog){
            return;
        }
        System.out.print("[Solon] ");
        PrintUtil.redln(content);
    }
}

package cn.tenfell.webos.common.server;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.tenfell.webos.common.util.ProjectUtil;

import java.util.HashMap;

public class R extends HashMap<String, Object> {
    private static final Long serialVersionUID = 1L;

    public static String getExceptionMessage(Throwable e, boolean isAll) {
        String msg;
        Throwable temp = ExceptionUtil.getRootCause(e);
        if (isAll) {
            msg = ExceptionUtil.stacktraceToString(temp);
        } else {
            msg = temp.getMessage();
            if (StrUtil.isBlank(msg)) {
                msg = temp.getClass().getName();
            }
        }
        return msg;
    }

    //操作成功
    public static R ok() {
        return okData(null);
    }

    public static <T> R okData(T data) {
        return ok(data, "操作成功");
    }

    public static <T> R ok(T data, String msg) {
        return restResult(data, 0, msg, null);
    }

    public static R error(Throwable e) {
        String error = null;
        if (ProjectUtil.startConfig.getBool("debug")) {
            error = getExceptionMessage(e, true);
        }
        String msg = getExceptionMessage(e, false);
        return restResult(null, -1, msg, error);
    }

    //操作失败
    public static R failed() {
        return failed("操作失败");
    }

    public static R failed(String msg) {
        return failed(msg, -1);
    }

    public static R failed(String msg, int status) {
        return restResult(null, status, msg, null);
    }

    //权限不足
    public static R noAuth() {
        return failed("权限不足", 403);
    }

    public static R lock() {
        return failed("已锁定", 408);
    }


    //未找到
    public static R noPath() {
        return failed("此路径找不到", 404);
    }

    //未登录
    public static R noLogin() {
        return noLogin("请登录后重试");
    }

    public static R noLogin(String msg) {
        return failed(msg, 401);
    }

    private static <T> R restResult(T data, int osStatus, String msg, String error) {
        R r = new R();
        //0成功 -1失败 401未登录 403权限不足 404未找到 407系统未安装
        r.set("code", osStatus)
                .set(data != null, "data", data)
                .set(error != null, "error", error)
                .set("msg", msg);
        return r;
    }

    public static R noInstall() {
        return failed("当前系统未安装，请先安装", 407);
    }

    private R set(String attr, Object object) {
        return set(true, attr, object);
    }

    private R set(boolean flag, String attr, Object object) {
        if (flag) {
            this.put(attr, object);
        }
        return this;
    }
}

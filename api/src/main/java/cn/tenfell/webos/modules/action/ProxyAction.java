package cn.tenfell.webos.modules.action;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.net.multipart.MultipartFormData;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Login;
import cn.tenfell.webos.common.util.ProjectContext;
import org.noear.solon.core.NvMap;
import org.noear.solon.core.handle.Context;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@BeanAction(val = "proxy")
public class ProxyAction {
    @Action(val = "http")
    @Login(val = false)
    public InputStream http(Context ctx) {
        String url = ctx.param("url");
        if (StrUtil.isBlank(url)) {
            return null;
        }
        url = Base64Decoder.decodeStr(url);
        boolean cdx = StrUtil.isNotBlank(ctx.param("cdx"));
        //创建请求
        HttpRequest httpReq = HttpUtil.createRequest(Method.valueOf(ctx.method()), url);
        if(cdx){
            httpReq.setMaxRedirectCount(20);
        }
        //整合headers
        String header = ctx.param("header");
        if (StrUtil.isNotBlank(header)) {
            Map<String, String> headerMapping = getHeaderMapping(Base64Decoder.decodeStr(header));
            Map<String, String> headers = new HashMap<>();
            Map<String,String> tmp = ctx.headerMap();
            tmp.forEach((key, val) -> {
                String newKey = headerMapping.get(key.toLowerCase());
                if (StrUtil.isBlank(newKey)) {
                    return;
                }
                headers.put(newKey, val);
            });
            httpReq.headerMap(headers, true);
        }
        String expHeader = ctx.param("expHeader");
        if (StrUtil.isNotBlank(header)) {
            Map<String, String> headerMapping = getHeaderMapping(Base64Decoder.decodeStr(expHeader));
            headerMapping.forEach((key, val) -> httpReq.header(key, val));
        }

        if (!StrUtil.equals(ctx.method(), "GET")) {
            //整合提交的数据
            if (StrUtil.containsAny(ctx.contentType(),"json","JSON")) {
                //body提交
                try{
                    String body = ctx.body();
                    httpReq.body(body);
                }catch (Exception e){

                }
            } else {
                //表单提交
                NvMap nvMap = ctx.paramMap();
                Map<String, Object> httpParam = new HashMap<>();
                nvMap.forEach((key, val) -> {
                    if (StrUtil.equals(key, "module") ||
                            StrUtil.equals(key, "action") ||
                            StrUtil.equals(key, "url") ||
                            StrUtil.equals(key, "expHeader") ||
                            StrUtil.equals(key, "resHeader") ||
                            StrUtil.equals(key, "header") ||
                            StrUtil.equals(key, "cdx")
                    ) {
                        return;
                    }
                    httpParam.put(key, val);
                });
                httpReq.form(httpParam);
            }
        }
        //发起请求并返回
        HttpResponse resp = httpReq.executeAsync();
        //整合headers
        String resHeader = ctx.param("resHeader");
        if (StrUtil.isNotBlank(resHeader)) {
            Map<String, String> headerMapping = getHeaderMapping(Base64Decoder.decodeStr(resHeader));
            resp.headers().forEach((s, strings) -> {
                if (StrUtil.isBlank(s)) {
                    return;
                }
                String newKey = headerMapping.get(s.toLowerCase());
                if (StrUtil.isBlank(newKey)) {
                    return;
                }
                for (int i = 0; i < strings.size(); i++) {
                    ctx.headerAdd(newKey, strings.get(i));
                }
            });
        }
        return resp.bodyStream();
    }

    private Map<String, String> getHeaderMapping(String str) {
        Map<String, String> map = new HashMap<>();
        if (StrUtil.isBlank(str)) {
            return map;
        }
        String[] fhStrs = str.split(";");
        for (String fhStr : fhStrs) {
            if (StrUtil.isBlank(fhStr)) {
                continue;
            }
            String[] mhStrs = fhStr.split(":");
            String key = mhStrs[0];
            if (StrUtil.isBlank(key)) {
                continue;
            }
            String val = mhStrs.length > 1 ? mhStrs[1] : mhStrs[0];
            map.put(key.toLowerCase(), val.toUpperCase());
        }
        return map;
    }
}

package cn.tenfell.webos.common.util;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.net.multipart.MultipartFormData;
import cn.hutool.core.net.multipart.UploadSetting;
import cn.hutool.core.util.CharsetUtil;
import lombok.Data;
import org.noear.solon.core.handle.Context;

import java.io.IOException;

@Data
public class ProjectContext {
    private Context ctx;
    public static MultipartFormData getMultipart(Context ctx) throws IORuntimeException {
        final MultipartFormData formData = new MultipartFormData(new UploadSetting());
        try {
            formData.parseRequestStream(ctx.bodyAsStream(), CharsetUtil.charset("utf-8"));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return formData;
    }
    public static ProjectContext init(Context ctx) {
        ProjectContext context = new ProjectContext();
        context.setCtx(ctx);
        return context;
    }
}

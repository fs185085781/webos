package cn.tenfell.webos.common.webdav.sf;

import java.io.OutputStream;
import java.io.Writer;

public interface HttpServletResponse {
    void sendError(int status);

    void sendError(int code, String statusText);

    void setStatus(int scMultiStatus);

    Writer getWriter();

    void addHeader(String allow, String methodsAllowed);

    void setContentType(String s);

    OutputStream getOutputStream();

    void setCharacterEncoding(String utf8);

    String encodeRedirectURL(String s);

    void sendRedirect(String encodeRedirectURL);

    void setDateHeader(String s, long lastModified);

    void setContentLength(int resourceLength);

    void setHeader(String s, String s1);
}

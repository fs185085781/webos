package cn.tenfell.webos.common.webdav.sf;

import java.io.InputStream;
import java.security.Principal;
import java.util.Locale;

public interface HttpServletRequest {
    String getHeader(String key);

    String getServerName();

    String getContextPath();

    String getPathInfo();

    String getServletPath();

    String getAttribute(String s);

    String getRequestURI();

    String getMethod();

    long getContentLength();

    InputStream getInputStream();

    Principal getUserPrincipal();

    Locale getLocale();
}

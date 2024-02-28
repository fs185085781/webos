package cn.tenfell.webos.common.webdav.sf;

import java.security.Principal;

public interface ITransaction {

    Principal getPrincipal();

    HttpServletRequest getRequest();

    HttpServletResponse getResponse();
}

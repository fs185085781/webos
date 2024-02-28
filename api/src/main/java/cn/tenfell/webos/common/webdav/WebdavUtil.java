package cn.tenfell.webos.common.webdav;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.tenfell.webos.common.webdav.sf.*;
import cn.tenfell.webos.common.webdav.sf.exceptions.UnauthenticatedException;
import cn.tenfell.webos.common.webdav.sf.exceptions.WebdavException;
import cn.tenfell.webos.common.webdav.sf.locking.ResourceLocks;
import cn.tenfell.webos.common.webdav.sf.methods.*;
import org.noear.solon.core.handle.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.security.Principal;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebdavUtil {
    private static Map<String, IMethodExecutor> webdavMap = new ConcurrentHashMap<>();
    private static IWebdavStore store;
    private static final boolean READ_ONLY = false;
    public static void init(String dftIndexFile,
                     String insteadOf404, int nocontentLenghHeaders,
                     boolean lazyFolderCreationOnPut){
        ResourceLocks _resLocks = new ResourceLocks();
        IMimeTyper mimeTyper = new IMimeTyper() {
            public String getMimeType(ITransaction transaction, String path) {
                String retVal= store.getStoredObject(transaction, path).getMimeType();
                return retVal;
            }
        };
        register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
                mimeTyper, nocontentLenghHeaders));
        register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
                _resLocks, mimeTyper, nocontentLenghHeaders));
        DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
                _resLocks, READ_ONLY));
        DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
                doDelete, READ_ONLY));
        register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
        register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
        register("MOVE", new DoMove(store, _resLocks, doDelete, doCopy, READ_ONLY));
        register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
        register("OPTIONS", new DoOptions(store, _resLocks));
        register("PUT", new DoPut(store, _resLocks, READ_ONLY,
                lazyFolderCreationOnPut));
        register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
        register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
        register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
    }
    public static IMethodExecutor register(String methodName, IMethodExecutor method){
        webdavMap.put(methodName,method);
        return method;
    }
    static {
        store = new WebosStore();
        init(null,null,-1,false);
    }

    /**
     * 将uri地址转化成中文地址
     * @param path
     * @return
     */
    public static String cnPath(String path){
        path = URLUtil.decode(path);
        String prefix = prefixPath();
        path = path.substring(prefix.length());
        if(path.startsWith("/")){
            path = path.substring(1);
        }
        if(path.endsWith("/")){
            path = path.substring(0,path.length()-1);
        }
        return path;
    }
    public static void servlet(Context ctx) throws Exception{
        /*if(ctx.uri().getPath().indexOf("/._")!=-1){
            return;
        }*/
        IMethodExecutor inface = webdavMap.get(ctx.method());
        if(inface == null){
            inface = webdavMap.get("*NO*IMPL*");
        }
        HttpServletRequest req = ctx2req(ctx);
        HttpServletResponse resp = ctx2resp(ctx);
        ITransaction transaction = null;
        boolean needRollback = false;
        try {
            Principal userPrincipal = req.getUserPrincipal();
            transaction =store.begin(userPrincipal, req, resp);
            needRollback = true;
            store.checkAuthentication(transaction);
            resp.setStatus(WebdavStatus.SC_OK);
            try {
                inface.execute(transaction, req, resp);
                store.commit(transaction);
                /** Clear not consumed data
                 *
                 * Clear input stream if available otherwise later access
                 * include current input.  These cases occure if the client
                 * sends a request with body to an not existing resource.
                 */
                if (req.getContentLength() != 0 && req.getInputStream().available() > 0) {
                    while (req.getInputStream().available() > 0) {
                        req.getInputStream().read();
                    }
                }
                needRollback = false;
            } catch (IOException e) {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                store.rollback(transaction);
                throw new RuntimeException(e);
            }
        } catch (UnauthenticatedException e) {
            ctx.headerAdd("WWW-Authenticate","Basic realm=\"webos\"");
            resp.sendError(e.getCode());
        } catch (WebdavException e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            throw new RuntimeException(e);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
        } finally {
            if (needRollback)
                store.rollback(transaction);
        }
    }

    private static HttpServletResponse ctx2resp(Context ctx) {
        return new HttpServletResponse() {
            @Override
            public void sendError(int status) {
                ctx.status(status);
            }

            @Override
            public void sendError(int status, String statusText) {
                ctx.status(status);
                ctx.output(statusText);
            }

            @Override
            public void setStatus(int status) {
                ctx.status(status);
            }

            @Override
            public Writer getWriter() {
                try{
                    return IoUtil.getUtf8Writer(ctx.outputStream());
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void addHeader(String key, String val) {
                ctx.headerAdd(key,val);
            }
            @Override
            public void setContentType(String s) {
                ctx.contentType(s);
            }

            @Override
            public OutputStream getOutputStream() {
                try{
                    return ctx.outputStream();
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setCharacterEncoding(String str) {
                ctx.charset(str);
            }

            @Override
            public String encodeRedirectURL(String url) {
                return url;
            }

            @Override
            public void sendRedirect(String url) {
                ctx.redirect(url);
            }

            @Override
            public void setDateHeader(String s, long lastModified) {
                ctx.headerSet(s,new Date(lastModified).toString());
            }

            @Override
            public void setContentLength(int length) {
                ctx.contentLength(length);
            }

            @Override
            public void setHeader(String s, String s1) {
                ctx.headerSet(s,s1);
            }
        };
    }

    private static HttpServletRequest ctx2req(Context ctx) {
        return new HttpServletRequest() {
            @Override
            public String getHeader(String key) {
                return ctx.header(key);
            }

            @Override
            public String getServerName() {
                return ctx.uri().getHost();
            }

            @Override
            public String getContextPath() {
                return "";
            }

            @Override
            public String getPathInfo() {
                return ctx.uri().getRawPath();
            }

            @Override
            public String getServletPath() {
                return "";
            }

            @Override
            public String getAttribute(String s) {
                return ctx.attr(s);
            }

            @Override
            public String getRequestURI() {
                return ctx.uri().getPath();
            }

            @Override
            public String getMethod() {
                return ctx.method();
            }

            @Override
            public long getContentLength() {
                return ctx.contentLength();
            }

            @Override
            public InputStream getInputStream() {
                try{
                    return ctx.bodyAsStream();
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Principal getUserPrincipal() {
                String authorization = ctx.header("authorization");
                if(StrUtil.isBlank(authorization)){
                    return null;
                }
                String[] sz = authorization.split(" ");
                if(sz.length != 2){
                    return null;
                }
                return () -> sz[1];
            }

            @Override
            public Locale getLocale() {
                return ctx.getLocale();
            }
        };
    }

    public static String prefixPath() {
        return "/webdav";
    }
}

/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.tenfell.webos.common.webdav.sf.methods;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.URLUtil;
import cn.tenfell.webos.common.webdav.sf.*;
import cn.tenfell.webos.common.webdav.sf.locking.ResourceLocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class DoGet extends DoHead {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoGet.class);

    public DoGet(IWebdavStore store, String dftIndexFile, String insteadOf404,
            ResourceLocks resourceLocks, IMimeTyper mimeTyper,
            int contentLengthHeader) {
        super(store, dftIndexFile, insteadOf404, resourceLocks, mimeTyper,
                contentLengthHeader);

    }

    protected void doBody(ITransaction transaction, HttpServletResponse resp,
            String path) {

        try {
            StoredObject so = _store.getStoredObject(transaction, path);
            if (so.isNullResource()) {
                String methodsAllowed = DeterminableMethod
                        .determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }
            OutputStream out = resp.getOutputStream();
            InputStream in = _store.getResourceContent(transaction, path);
            try {
                if (in != null) {
                    LOG.debug("开始 {}, ", path);
                    IoUtil.copy(in, out);
                    LOG.debug("结束 {}", path);
                }
            } finally {
                // flushing causes a IOE if a file is opened on the webserver
                // client disconnected before server finished sending response
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    LOG.warn("{} Closing InputStream causes Exception!\n", path
                            ,e);
                }
                try {
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    LOG.warn("{} Flushing OutputStream causes Exception!\n", path
                            ,e);
                }
            }
        } catch (Exception e) {
            LOG.warn("{} doBody causes Exception!\n", path
                    ,e);
            LOG.trace(e.toString());
        }
    }

    protected void folderBody(ITransaction transaction, String path,
            HttpServletResponse resp, HttpServletRequest req)
            throws IOException {

        StoredObject so = _store.getStoredObject(transaction, path);
        if (so == null) {
            resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                    .getRequestURI());
        } else {

            if (so.isNullResource()) {
                String methodsAllowed = DeterminableMethod
                        .determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }

            if (so.isFolder()) {
                // TODO some folder response (for browsers, DAV tools
                // use propfind) in html?
                Locale locale = req.getLocale();
                DateFormat shortDF= getDateTimeFormat(req.getLocale());
                resp.setContentType("text/html");
                resp.setCharacterEncoding("UTF8");
                OutputStream out = resp.getOutputStream();
                String[] children = _store.getChildrenNames(transaction, path);
                // Make sure it's not null
                children = children == null ? new String[] {} : children;
                // Sort by name
                Arrays.sort(children);
                StringBuilder childrenTemp = new StringBuilder();
                childrenTemp.append("<html><head><title>Content of folder");
                childrenTemp.append(path);
                childrenTemp.append("</title><style type=\"text/css\">");
                childrenTemp.append(getCSS());
                childrenTemp.append("</style></head>");
                childrenTemp.append("<body>");
                childrenTemp.append(getHeader(transaction, path, resp, req));
                childrenTemp.append("<table>");
                childrenTemp.append("<tr><th>Name</th><th>Size</th><th>Created</th><th>Modified</th></tr>");
                childrenTemp.append("<tr>");
                childrenTemp.append("<td colspan=\"4\"><a href=\"../\">Parent</a></td></tr>");
                boolean isEven= false;
                for (String child : children)
                {
                    isEven= !isEven;
                    childrenTemp.append("<tr class=\"");
                    childrenTemp.append(isEven ? "even" : "odd");
                    childrenTemp.append("\">");
                    childrenTemp.append("<td>");
                    childrenTemp.append("<a href=\"");
                    childrenTemp.append(URLUtil.encode(child));
                    StoredObject obj= _store.getStoredObject(transaction, path+"/"+child);
                    if (obj == null)
                    {
                        LOG.error("Should not return null for "+path+"/"+child);
                    }
                    if (obj != null && obj.isFolder())
                    {
                        childrenTemp.append("/");
                    }
                    childrenTemp.append("\">");
                    childrenTemp.append(child);
                    childrenTemp.append("</a></td>");
                    if (obj != null && obj.isFolder())
                    {
                        childrenTemp.append("<td>Folder</td>");
                    }
                    else
                    {
                        childrenTemp.append("<td>");
                        if (obj != null )
                        {
                            childrenTemp.append(obj.getResourceLength());
                        }
                        else
                        {
                            childrenTemp.append("Unknown");
                        }
                        childrenTemp.append(" Bytes</td>");
                    }
                    if (obj != null && obj.getCreationDate() != null)
                    {
                        childrenTemp.append("<td>");
                        childrenTemp.append(shortDF.format(obj.getCreationDate()));
                        childrenTemp.append("</td>");
                    }
                    else
                    {
                        childrenTemp.append("<td></td>");
                    }
                    if (obj != null  && obj.getLastModified() != null)
                    {
                        childrenTemp.append("<td>");
                        childrenTemp.append(shortDF.format(obj.getLastModified()));
                        childrenTemp.append("</td>");
                    }
                    else
                    {
                        childrenTemp.append("<td></td>");
                    }
                    childrenTemp.append("</tr>");
                }
                childrenTemp.append("</table>");
                childrenTemp.append(getFooter(transaction, path, resp, req));
                childrenTemp.append("</body></html>");
                out.write(childrenTemp.toString().getBytes("UTF-8"));
            }
        }
    }

    /**
     * Return the CSS styles used to display the HTML representation
     * of the webdav content.
     *
     * @return
     */
    protected String getCSS()
    {
        // The default styles to use
       String retVal= "body {\n"+
                "	font-family: Arial, Helvetica, sans-serif;\n"+
                "}\n"+
                "h1 {\n"+
                "	font-size: 1.5em;\n"+
                "}\n"+
                "th {\n"+
                "	background-color: #9DACBF;\n"+
                "}\n"+
                "table {\n"+
                "	border-top-style: solid;\n"+
                "	border-right-style: solid;\n"+
                "	border-bottom-style: solid;\n"+
                "	border-left-style: solid;\n"+
                "}\n"+
                "td {\n"+
                "	margin: 0px;\n"+
                "	padding-top: 2px;\n"+
                "	padding-right: 5px;\n"+
                "	padding-bottom: 2px;\n"+
                "	padding-left: 5px;\n"+
                "}\n"+
                "tr.even {\n"+
                "	background-color: #CCCCCC;\n"+
                "}\n"+
                "tr.odd {\n"+
                "	background-color: #FFFFFF;\n"+
                "}\n"+
                "";
        try
        {
            // Try loading one via class loader and use that one instead
            ClassLoader cl = getClass().getClassLoader();
            InputStream iStream = cl.getResourceAsStream("webdav.css");
            if(iStream != null)
            {
                // Found css via class loader, use that one
                StringBuffer out = new StringBuffer();
                byte[] b = new byte[4096];
                for (int n; (n = iStream.read(b)) != -1;)
                {
                    out.append(new String(b, 0, n));
}
                retVal= out.toString();
            }
        }
        catch (Exception ex)
        {
            LOG.error("Error in reading webdav.css", ex);
        }

        return retVal;
    }

    /**
     * Return the header to be displayed in front of the folder content
     *
     * @param transaction
     * @param path
     * @param resp
     * @param req
     * @return
     */
    protected String getHeader(ITransaction transaction, String path,
            HttpServletResponse resp, HttpServletRequest req)
    {
        return "<h1>Content of folder "+path+"</h1>";
    }

    /**
     * Return the footer to be displayed after the folder content
     *
     * @param transaction
     * @param path
     * @param resp
     * @param req
     * @return
     */
    protected String getFooter(ITransaction transaction, String path,
            HttpServletResponse resp, HttpServletRequest req)
    {
        return "";
    }

    /**
     * Return this as the Date/Time format for displaying Creation + Modification dates
     *
     * @param browserLocale
     * @return DateFormat used to display creation and modification dates
     */
    protected DateFormat getDateTimeFormat(Locale browserLocale)
    {
        return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, browserLocale);
    }
 }

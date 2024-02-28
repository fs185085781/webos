package cn.tenfell.webos.common.webdav.sf.methods;

import cn.tenfell.webos.common.webdav.sf.IMethodExecutor;
import cn.tenfell.webos.common.webdav.sf.ITransaction;
import cn.tenfell.webos.common.webdav.sf.WebdavStatus;

import cn.tenfell.webos.common.webdav.sf.HttpServletRequest;
import cn.tenfell.webos.common.webdav.sf.HttpServletResponse;
import java.io.IOException;

public class DoNotImplemented implements IMethodExecutor {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoNotImplemented.class);
    private boolean _readOnly;

    public DoNotImplemented(boolean readOnly) {
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        LOG.trace("-- " + req.getMethod());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else
            resp.sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
    }
}

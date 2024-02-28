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

import cn.tenfell.webos.common.webdav.sf.ITransaction;
import cn.tenfell.webos.common.webdav.sf.IWebdavStore;
import cn.tenfell.webos.common.webdav.sf.WebdavStatus;
import cn.tenfell.webos.common.webdav.sf.exceptions.AccessDeniedException;
import cn.tenfell.webos.common.webdav.sf.exceptions.LockFailedException;
import cn.tenfell.webos.common.webdav.sf.exceptions.ObjectAlreadyExistsException;
import cn.tenfell.webos.common.webdav.sf.exceptions.WebdavException;
import cn.tenfell.webos.common.webdav.sf.locking.ResourceLocks;

import cn.tenfell.webos.common.webdav.sf.HttpServletRequest;
import cn.tenfell.webos.common.webdav.sf.HttpServletResponse;
import java.io.IOException;
import java.util.Hashtable;

public class DoMove extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoMove.class);
    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private DoDelete _doDelete;
    private DoCopy _doCopy;
    private boolean _readOnly;

    public DoMove(IWebdavStore _store, ResourceLocks resourceLocks, DoDelete doDelete,
            DoCopy doCopy, boolean readOnly) {
        this._store = _store;
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _doCopy = doCopy;
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {

        if (!_readOnly) {
            LOG.trace("-- " + this.getClass().getName());

            String sourcePath = getRelativePath(req);
            Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

            if (!checkLocks(transaction, req, resp, _resourceLocks, sourcePath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            String destinationPath = parseDestinationHeader(req, resp);
            if (destinationPath == null) {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }

            if (!checkLocks(transaction, req, resp, _resourceLocks,
                    destinationPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            String tempLockOwner = "doMove" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(transaction, sourcePath, tempLockOwner,
                    false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    boolean ok = _store.moveObject(transaction, destinationPath, sourcePath);
                    if (!ok) {
                        if (_doCopy.copyResource(transaction, req, resp)) {

                            errorList = new Hashtable<String, Integer>();
                            _doDelete.deleteResource(transaction, sourcePath,
                                    errorList, req, resp);
                            if (!errorList.isEmpty()) {
                                sendReport(req, resp, errorList);
                            }
                        }
                    }


                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction,
                            sourcePath, tempLockOwner);
                }
            } else {
                errorList.put(req.getHeader("Destination"),
                        WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        }

    }

}

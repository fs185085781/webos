package cn.tenfell.webos.common.webdav.sf.exceptions;

public class LockFailedException extends WebdavException {

    public LockFailedException() {
        super();
    }

    public LockFailedException(String message) {
        super(message);
    }

    public LockFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockFailedException(Throwable cause) {
        super(cause);
    }
}

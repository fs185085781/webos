package cn.tenfell.webos.common.webdav.sf;

public interface IMimeTyper {

    /**
     * Detect the mime type of this object
     * 
     * @param transaction
     * @param path
     * @return 
     */
    String getMimeType(ITransaction transaction, String path);
}

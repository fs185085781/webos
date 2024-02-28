package cn.tenfell.webos.common.webdav.sf.locking;

import cn.tenfell.webos.common.webdav.sf.ITransaction;
import cn.tenfell.webos.common.webdav.sf.exceptions.LockFailedException;

public interface IResourceLocks {

    /**
     * Tries to lock the resource at "path".
     * 
     * @param transaction
     * @param path
     *      what resource to lock
     * @param owner
     *      the owner of the lock
     * @param exclusive
     *      if the lock should be exclusive (or shared)
     * @param depth
     *      depth
     * @param timeout
     *      Lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException
     */
    boolean lock(ITransaction transaction, String path, String owner,
                 boolean exclusive, int depth, int timeout, boolean temporary)
            throws LockFailedException;

    /**
     * Unlocks all resources at "path" (and all subfolders if existing)<p/> that
     * have the same owner.
     * 
     * @param transaction
     * @param id
     *      id to the resource to unlock
     * @param owner
     *      who wants to unlock
     */
    boolean unlock(ITransaction transaction, String id, String owner);

    /**
     * Unlocks all resources at "path" (and all subfolders if existing)<p/> that
     * have the same owner.
     * 
     * @param transaction
     * @param path
     *      what resource to unlock
     * @param owner
     *      who wants to unlock
     */
    void unlockTemporaryLockedObjects(ITransaction transaction, String path,
            String owner);

    /**
     * Deletes LockedObjects, where timeout has reached.
     * 
     * @param transaction
     * @param temporary
     *      Check timeout on temporary or real locks
     */
    void checkTimeouts(ITransaction transaction, boolean temporary);

    /**
     * Tries to lock the resource at "path" exclusively.
     * 
     * @param transaction
     *      Transaction
     * @param path
     *      what resource to lock
     * @param owner
     *      the owner of the lock
     * @param depth
     *      depth
     * @param timeout
     *      Lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException
     */
    boolean exclusiveLock(ITransaction transaction, String path, String owner,
            int depth, int timeout) throws LockFailedException;

    /**
     * Tries to lock the resource at "path" shared.
     * 
     * @param transaction
     *      Transaction
     * @param path
     *      what resource to lock
     * @param owner
     *      the owner of the lock
     * @param depth
     *      depth
     * @param timeout
     *      Lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException
     */
    boolean sharedLock(ITransaction transaction, String path, String owner,
            int depth, int timeout) throws LockFailedException;

    /**
     * Gets the LockedObject corresponding to specified id.
     * 
     * @param transaction
     * @param id
     *      LockToken to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getLockedObjectByID(ITransaction transaction, String id);

    /**
     * Gets the LockedObject on specified path.
     * 
     * @param transaction
     * @param path
     *      Path to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getLockedObjectByPath(ITransaction transaction, String path);

    /**
     * Gets the LockedObject corresponding to specified id (locktoken).
     * 
     * @param transaction
     * @param id
     *      LockToken to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getTempLockedObjectByID(ITransaction transaction, String id);

    /**
     * Gets the LockedObject on specified path.
     * 
     * @param transaction
     * @param path
     *      Path to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getTempLockedObjectByPath(ITransaction transaction, String path);

}

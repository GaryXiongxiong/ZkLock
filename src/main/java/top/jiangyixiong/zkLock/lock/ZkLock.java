package top.jiangyixiong.zkLock.lock;

/**
 * @author Yixiong
 * Zookeeper look interfaces, provide basic block lock operations
 */
public interface ZkLock {
    /**
     * Acquire this lock blockly
     *
     */
    void lock();

    /**
     * Release this lock if it is occupied by current thread.
     *
     * @return true if release successfully.
     */
    boolean releaseLock();
}

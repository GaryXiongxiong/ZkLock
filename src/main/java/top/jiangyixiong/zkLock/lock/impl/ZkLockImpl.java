package top.jiangyixiong.zkLock.lock.impl;

import org.I0Itec.zkclient.IZkChildListener;
import top.jiangyixiong.zkLock.exception.LockException;
import top.jiangyixiong.zkLock.lock.ZkLock;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

public class ZkLockImpl implements ZkLock {

    private static final Logger LOG = LoggerFactory.getLogger(ZkLock.class);

    private final String lockPath;
    private final ZkClient zkClient;
    private final String servers;
    private final int sessionTimeout;
    private final int connectionTimeout;
    private final ThreadLocal<String> curNode = new ThreadLocal<>();

    /**
     * Constructor for basic ZkLock
     *
     * @param servers           Servers list for zookeeper, see {@link ZkClient#ZkClient(java.lang.String, int, int)}
     * @param sessionTimeout    Session Timeout for zookeeper, see {@link ZkClient#ZkClient(java.lang.String, int, int)}
     * @param connectionTimeout Connection Timeout for zookeeper, see {@link ZkClient#ZkClient(java.lang.String, int, int)}
     * @param lockPath          the path of this lock in zookeeper
     */
    public ZkLockImpl(String servers, int sessionTimeout, int connectionTimeout, String lockPath) {
        this.lockPath = lockPath;
        this.servers = servers;
        this.sessionTimeout = sessionTimeout;
        this.connectionTimeout = connectionTimeout;
        this.zkClient = new ZkClient(servers, sessionTimeout, connectionTimeout);
        if (!zkClient.exists(lockPath)) {
            zkClient.createPersistent(lockPath);
            LOG.info("Connected to [{}], lock path:[{}] created", servers, lockPath);
        } else {
            LOG.info("Connected to [{}], lock path:[{}] existed", servers, lockPath);
        }
    }

    public String getLockPath() {
        return lockPath;
    }

    public String getServers() {
        return servers;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Try if the thread occupied lock currently.
     *
     * @return Ture if thread occupied the lock
     */
    protected boolean checkLock() {
        if (Objects.isNull(curNode.get())) return false;
        List<String> lockQueue = zkClient.getChildren(lockPath);
        Collections.sort(lockQueue);
        if (lockQueue.size() > 0 && (lockPath + "/" + lockQueue.get(0)).equals(curNode.get())) {
            LOG.debug("Lock [{}] acquired", lockPath);
            return true;
        } else {
            LOG.debug("Lock [{}] is occupied by others", lockPath);
            return false;
        }
    }

    @Override
    public void lock() {
        Semaphore semaphore = new Semaphore(0);

        IZkChildListener listener = (dataPath, currentChilds) -> {
            LOG.debug("Handling Child Change at [{}], children:[{}]", lockPath,currentChilds);
            semaphore.release();
        };

        zkClient.subscribeChildChanges(lockPath, listener);

        if (Objects.isNull(curNode.get())) {
            curNode.set(zkClient.createEphemeralSequential(lockPath + "/", "Lock"));
            LOG.debug("curNode [{}] has been created", curNode.get());
        } else {
            throw new LockException("ZkLock is not reentrant");
        }

        while (!checkLock()) {
            try {
                LOG.debug("Thread blocked in lock [{}], listening path-[{}]", lockPath, lockPath);
                semaphore.acquire();
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
            }
            LOG.debug("Queue [{}] has changed, retry to acquire lock ",lockPath);
        }
        zkClient.unsubscribeChildChanges(lockPath, listener);
    }

    @Override
    public boolean tryLock() {
        if (Objects.isNull(curNode.get())) {
            curNode.set(zkClient.createEphemeralSequential(lockPath + "/", "Lock"));
            LOG.debug("curNode [{}] has been created", curNode.get());
        } else {
            throw new LockException("ZkLock is not reentrant");
        }
        if (checkLock()) {
            return true;
        } else if (zkClient.exists(curNode.get())) {
            if (zkClient.delete(curNode.get())) {
                LOG.debug("Unable to acquire lock [{}], Node [{}] has been deleted", lockPath, curNode.get());
                curNode.remove();
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean unlock() {
        if (Objects.nonNull(curNode.get()) && zkClient.exists(curNode.get()) && checkLock()) {
            if (zkClient.delete(curNode.get())) {
                LOG.debug("Lock [{}] released, Node [{}] has been deleted", lockPath, curNode.get());
                curNode.remove();
                return true;
            }
        }
        LOG.error("Illegally lock release");
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZkLockImpl zkLock = (ZkLockImpl) o;
        return Objects.equals(getLockPath(), zkLock.getLockPath()) &&
                Objects.equals(getServers(), zkLock.getServers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLockPath(), getServers());
    }
}

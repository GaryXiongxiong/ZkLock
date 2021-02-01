package top.jiangyixiong.zkLock.lock;

import org.springframework.stereotype.Service;
import top.jiangyixiong.zkLock.config.ZkLockConfig;
import top.jiangyixiong.zkLock.lock.impl.ZkLockImpl;
import top.jiangyixiong.zkLock.lock.impl.ZkReentrantLock;

import javax.annotation.Resource;

/**
 * Factory for {@link top.jiangyixiong.zkLock.lock.impl.ZkLockImpl}, use config to generate ZkLock easily. Set config for {@code zklock.zk-servers}, {@code zklock.connection-timeout}, {@code zklock.session-timeout} before use. <br/>
 * Demo config: <br/>
 *
 *  zklock:<br/>
 *      zk-servers: "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183"<br/>
 *      connection-timeout: 30000<br/>
 *      session-timeout: 30000<br/>
 */
@Service
public class ZkLockFactory {

    @Resource
    private ZkLockConfig config;

    /**
     * Get a ZkLock with path set in config file
     * @return The ZkLock
     */
    public ZkLock getLock() {
        return getLock(config.getLockPath());
    }

    /**
     * Get a ZkLock with given path
     * @param lockPath Path of lock in zookeeper
     * @return The ZkLock
     */
    public ZkLock getLock(String lockPath) {
        return new ZkLockImpl(config.getZkServers(), config.getSessionTimeout(), config.getConnectionTimeout(), lockPath);
    }

    public ZkLock getReentrantLock(){
        return getReentrantLock(config.getLockPath());
    }

    public ZkLock getReentrantLock(String lockPath){
        return new ZkReentrantLock(config.getZkServers(), config.getSessionTimeout(), config.getConnectionTimeout(), lockPath);
    }
}

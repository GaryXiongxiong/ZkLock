package top.jiangyixiong.zkLock.lock.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkReentrantLock extends ZkLockImpl{

    private static final Logger LOG = LoggerFactory.getLogger(ZkReentrantLock.class);
    private final ThreadLocal<Integer> reentries = ThreadLocal.withInitial(()->0);

    /**
     * Constructor for basic ZkLock
     *
     * @param servers           Servers list for zookeeper, see {@link ZkClient#ZkClient(String, int, int)}
     * @param sessionTimeout    Session Timeout for zookeeper, see {@link ZkClient#ZkClient(String, int, int)}
     * @param connectionTimeout Connection Timeout for zookeeper, see {@link ZkClient#ZkClient(String, int, int)}
     * @param lockPath          the path of this lock in zookeeper
     */
    public ZkReentrantLock(String servers, int sessionTimeout, int connectionTimeout, String lockPath) {
        super(servers, sessionTimeout, connectionTimeout, lockPath);
    }

    @Override
    public void lock() {
        if(!super.checkLock()){
            super.lock();
        }
        reentries.set(reentries.get()+1);
        LOG.debug("Lock entry: {}",reentries);
    }

    @Override
    public boolean tryLock() {
        if(!super.checkLock()){
            if(!super.tryLock()) return false;
        }
        reentries.set(reentries.get()+1);
        LOG.debug("Lock entry: {}",reentries);
        return true;
    }

    @Override
    public boolean unlock() {
        if(reentries.get()<1){
            LOG.error("Illegally lock release");
            return false;
        }
        reentries.set(reentries.get()-1);
        if(reentries.get()<1){
            return super.unlock();
        }
        return true;
    }
}

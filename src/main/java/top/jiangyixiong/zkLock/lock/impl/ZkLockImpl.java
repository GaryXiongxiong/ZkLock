package top.jiangyixiong.zkLock.lock.impl;

import org.I0Itec.zkclient.IZkDataListener;
import top.jiangyixiong.zkLock.exception.LockException;
import top.jiangyixiong.zkLock.lock.ZkLock;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZkLockImpl implements ZkLock {

    private static final Logger LOG = LoggerFactory.getLogger(ZkLock.class);

    private final String lockPath;
    private final ZkClient zkClient;
    private final ThreadLocal<String> curNode = new ThreadLocal<>();
    private final ThreadLocal<String> preNode = new ThreadLocal<>();

    /**
     * Constructor for basic ZkLock
     *
     * @param servers Servers list for zookeeper, see {@link ZkClient#ZkClient(java.lang.String, int, int)}
     * @param sessionTimeout Session Timeout for zookeeper, see {@link ZkClient#ZkClient(java.lang.String, int, int)}
     * @param connectionTimeout Connection Timeout for zookeeper, see {@link ZkClient#ZkClient(java.lang.String, int, int)}
     * @param lockPath the path of this lock in zookeeper
     */
    public ZkLockImpl(String servers, int sessionTimeout, int connectionTimeout, String lockPath){
        this.lockPath = lockPath;
        this.zkClient = new ZkClient(servers, sessionTimeout, connectionTimeout);
        if(!zkClient.exists(lockPath)){
            zkClient.createPersistent(lockPath);
            LOG.info("Connected to [{}], lock path:[{}] created",servers,lockPath);
        }else {
            LOG.info("Connected to [{}], lock path:[{}] existed",servers,lockPath);
        }
    }

    /**
     * Try if the thread occupied lock currently.
     *
     * @return Ture if thread occupied the lock
     */
    private boolean tryLock() {
        List<String> lockQueue = zkClient.getChildren(lockPath);
        Collections.sort(lockQueue);
        if(lockQueue.size()>0&&(lockPath+"/"+lockQueue.get(0)).equals(curNode.get())){
            LOG.debug("Lock [{}] acquired",lockPath);
            return true;
        }else{
            int index = lockQueue.indexOf(curNode.get().substring(lockPath.length()+1));
            preNode.set(lockPath+"/"+lockQueue.get(index-1));
            LOG.debug("Lock [{}] is occupied, set preNode to [{}]",lockPath,preNode.get());
            return false;
        }
    }

    public void lock() {
        CountDownLatch latch = new CountDownLatch(1);

        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, Object data) {

            }

            @Override
            public void handleDataDeleted(String dataPath) {
                LOG.debug("Node [{}] has been deleted",dataPath);
                latch.countDown();
            }
        };

        if(null==curNode.get()){
            curNode.set(zkClient.createEphemeralSequential(lockPath+"/","Lock"));
            LOG.debug("curNode [{}] has been created",curNode.get());
        }else {
            throw new LockException("ZkLock is not reentrant");
        }

        if(!tryLock()&&zkClient.exists(preNode.get())){
            zkClient.subscribeDataChanges(preNode.get(),listener);
            while(zkClient.exists(preNode.get())&&!tryLock()){
                try {
                    LOG.debug("Thread blocked in lock [{}]",lockPath);
                    latch.await();
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }
            }
            zkClient.unsubscribeDataChanges(preNode.get(),listener);
        }
    }

    public boolean releaseLock() {
        if(null!=curNode.get()&&zkClient.exists(curNode.get())&&tryLock()){
            if(zkClient.delete(curNode.get())){
                LOG.debug("Lock [{}] released, Node [{}] deleted",lockPath,curNode.get());
                curNode.remove();
                return true;
            }
        }
        LOG.error("Illegally lock release");
        return false;
    }
}
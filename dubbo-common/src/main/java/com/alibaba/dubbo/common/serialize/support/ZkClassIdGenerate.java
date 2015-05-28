package com.alibaba.dubbo.common.serialize.support;

import com.alibaba.dubbo.common.Constants;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lubin.Xuan on 2015/3/17.
 * ie.
 */
public class ZkClassIdGenerate {

    private static final Logger logger = LoggerFactory.getLogger(ZkClassIdGenerate.class);

    private static final String ZK_P = "dubbo.zk.quorum";

    private ZkClient zkClient;

    private static final String BASE_DIR = "/dubbo_class_seq";

    private static final String LOCK_DIR = BASE_DIR + "/lock";

    private String lockName;

    private static final String CLASS_SEQ_DIR = BASE_DIR + "/seq";
    private static final String CLASS_DIR = BASE_DIR + "/class";

    private String waitNode;//等待前一个锁
    private String myZnode;//当前锁
    private CountDownLatch latch;//计数器
    private int sessionTimeout = 30000;
    private List<Exception> exception = new ArrayList<>();

    private int classStartSeq = 100000000;

    private static String zkLocation;

    static {
        InputStream inputStream = ZkClassIdGenerate.class.getResourceAsStream("/META-INF/dubbo.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new Error(e);
        }
        zkLocation = properties.getProperty(ZK_P);
    }

    public ZkClassIdGenerate() throws Exception {


        this.lockName = "duboo-serialize";
        //String zkLocation = "10.2.30.221:2181,10.2.30.222:2181,10.2.30.223:2181";
        zkClient = new ZkClient(zkLocation, Constants.DEFAULT_SESSION_TIMEOUT, Constants.DEFAULT_REGISTRY_CONNECT_TIMEOUT);

        if (!zkClient.exists(BASE_DIR)) {
            zkClient.createPersistent(BASE_DIR);
        }

        if (!zkClient.exists(LOCK_DIR)) {
            zkClient.createPersistent(LOCK_DIR);
        }

        if (!zkClient.exists(CLASS_SEQ_DIR)) {
            zkClient.createPersistent(CLASS_SEQ_DIR);
        }

        if (!zkClient.exists(CLASS_DIR)) {
            zkClient.createPersistent(CLASS_DIR);
        }
    }

    public void lock() {
        if (exception.size() > 0) {
            throw new LockException(exception.get(0));
        }
        try {
            if (this.tryLock()) {
                logger.debug("Thread " + Thread.currentThread().getId() + " " + myZnode + " get lock true");
            } else {
                waitForLock(waitNode, sessionTimeout);//等待锁
            }
        } catch (Exception e) {
            throw new LockException(e);
        }
    }

    protected boolean tryLock() {
        try {
            String splitStr = "_lock_";
            if (lockName.contains(splitStr))
                throw new LockException("lockName can not contains \\u000B  " + splitStr);
            //创建临时子节点
            myZnode = zkClient.createEphemeralSequential(LOCK_DIR + "/" + lockName + splitStr, "Id Locker");
            logger.debug(myZnode + " is created ");
            //取出所有子节点
            List<String> subNodes = zkClient.getChildren(LOCK_DIR);
            //取出所有lockName的锁
            List<String> lockObjNodes = new ArrayList<String>();
            for (String node : subNodes) {
                String _node = node.split(splitStr)[0];
                if (_node.equals(lockName)) {
                    lockObjNodes.add(node);
                }
            }
            Collections.sort(lockObjNodes);
            logger.debug(myZnode + "==" + lockObjNodes.get(0));
            if (myZnode.equals(LOCK_DIR + "/" + lockObjNodes.get(0))) {
                //如果是最小的节点,则表示取得锁
                return true;
            }
            //如果不是最小的节点，找到比自己小1的节点
            String subMyZnode = myZnode.substring(myZnode.lastIndexOf("/") + 1);
            waitNode = lockObjNodes.get(Collections.binarySearch(lockObjNodes, subMyZnode) - 1);
        } catch (Exception e) {
            throw new LockException(e);
        }
        return false;
    }

    private boolean waitForLock(String lower, long waitTime) throws InterruptedException, KeeperException {
        Boolean stat = zkClient.exists(LOCK_DIR + "/" + lower);
        //判断比自己小一个数的节点是否存在,如果不存在则无需等待锁,同时注册监听
        if (stat) {
            logger.info("Thread " + Thread.currentThread().getId() + " waiting for " + LOCK_DIR + "/" + lower);
            this.latch = new CountDownLatch(1);
            this.latch.await(waitTime, TimeUnit.MILLISECONDS);
            this.latch = null;
        }
        return true;
    }

    public void unlock() {
        try {
            logger.debug("unlock {}", myZnode);
            zkClient.delete(myZnode);
            myZnode = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getId(Class clazz) {

        int id;

        if (zkClient.exists(CLASS_DIR + "/" + clazz.getName())) {
            id = zkClient.readData(CLASS_DIR + "/" + clazz.getName());
        } else {
            String clazzNode = zkClient.createEphemeralSequential(CLASS_SEQ_DIR + "/serialize_class@", clazz);
            String idStr = clazzNode.split("serialize_class@")[1];
            id = classStartSeq + Integer.parseInt(idStr.substring(2));
            zkClient.createPersistent(CLASS_DIR + "/" + clazz.getName(), id);
        }

        logger.debug("序列化ID {}:{}", id, clazz);

        return id;
    }

    public Integer tryFind(Class clazz) {
        if (zkClient.exists(CLASS_DIR + "/" + clazz.getName())) {
            return zkClient.readData(CLASS_DIR + "/" + clazz.getName());
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        ZkClassIdGenerate idGenerate = new ZkClassIdGenerate();

        idGenerate.lock();

        System.out.println(idGenerate.getId(int.class));

        idGenerate.unlock();

    }


    public class LockException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public LockException(String e) {
            super(e);
        }

        public LockException(Exception e) {
            super(e);
        }
    }

}

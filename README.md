# ZkLock
A distribution lock encapsulation using ZooKeeper. See [分布式锁的Redis与Zookeeper实现
](https://jiangyixiong.top/2020/10/12/%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81%E7%9A%84Redis%E4%B8%8EZooKeeper%E5%AE%9E%E7%8E%B0/)

## Quick Start

Install this project via Maven.

```bash
mvn install
```

Include this dependency in `pom.xml`

```xml
<dependency>
    <groupId>top.jiangyixiong</groupId>
    <artifactId>zkLock</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Set `application.yml`

```yml
zklock:
  # ZooKeeper servers list
  zk-servers: "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183"
  # Connection timeout
  connection-timeout: 5000
  # Session timeout
  session-timeout: 8000
  # Path of locks in zoo keeper, optional
  lock-path: /lock
```

Use `ZkLockFactory` get `ZkLock` object

```java
ZkLock lock = zkLockFactory.getLock("/testLock");
```

## Usage demo

```java
@SpringBootTest
class DemoApplicationTests {

    @Resource
    ZkLockFactory zkLockFactory;

    @Test
    void testZkLock(){
        
        Thread t1 = new Thread(()->{
            ZkLock lock = zkLockFactory.getLock("/testLock");
            for(int i=0;i<5;i++){
                System.out.println(Thread.currentThread().getName()+" -> "+i);
            }
            lock.lock();
            System.out.println("Thread A occupied the lock");
            for(int i=0;i<5;i++){
                System.out.println(Thread.currentThread().getName()+" [Locked]-> "+i);
            }
            lock.releaseLock();
        },"ThreadA");
        
        Thread t2 = new Thread(()->{
            ZkLock lock = zkLockFactory.getLock("/testLock");
            for(int i=0;i<5;i++){
                System.out.println(Thread.currentThread().getName()+" -> "+i);
            }
            lock.lock();
            System.out.println("Thread B occupied the lock");
            for(int i=0;i<5;i++){
                System.out.println(Thread.currentThread().getName()+" [Locked]-> "+i);
            }
            lock.releaseLock();
        },"ThreadB");
        
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

## Todo:

- [x] Block non-reentrant lock
- [ ] Block reentrant lock
- [ ] Non-block method to acquire lock

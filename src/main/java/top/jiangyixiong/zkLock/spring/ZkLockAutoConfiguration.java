package top.jiangyixiong.zkLock.spring;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import top.jiangyixiong.zkLock.config.ZkLockConfig;

@Configuration
@ComponentScan("top.jiangyixiong.zkLock.*")
@EnableConfigurationProperties(ZkLockConfig.class)
public class ZkLockAutoConfiguration {

}

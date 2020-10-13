package top.jiangyixiong.zkLock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Yixiong
 *
 * Auto config for SpringBoot
 */
@Data
@ConfigurationProperties(prefix = "zklock")
public class ZkLockConfig {
    private String zkServers = "127.0.0.1:2181";
    private int sessionTimeout = 8000;
    private int connectionTimeout = 5000;
    private String lockPath = "/zklock";
}

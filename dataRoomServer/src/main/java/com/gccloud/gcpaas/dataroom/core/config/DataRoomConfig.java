package com.gccloud.gcpaas.dataroom.core.config;

import com.gccloud.gcpaas.dataroom.core.config.bean.Cors;
import com.gccloud.gcpaas.dataroom.core.config.bean.Jwt;
import com.gccloud.gcpaas.dataroom.core.config.bean.OutboundHttp;
import com.gccloud.gcpaas.dataroom.core.config.bean.ResourceBean;
import com.gccloud.gcpaas.dataroom.core.config.bean.Sso;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dataroom")
public class DataRoomConfig {
    /**
     * 公钥
     */
    private String publicKey;
    /**
     * 私钥
     */
    private String privateKey;
    /**
     * jwt
     */
    private Jwt jwt = new Jwt();
    /**
     * 素材资源存储访问配置
     */
    private ResourceBean resource = new ResourceBean();
    /**
     * 跨域配置
     */
    private Cors cors = new Cors();
    /**
     * 服务端出网 HTTP/WebSocket 安全配置
     */
    private OutboundHttp outboundHttp = new OutboundHttp();
    /**
     * 单点登录配置
     */
    private Sso sso = new Sso();
}

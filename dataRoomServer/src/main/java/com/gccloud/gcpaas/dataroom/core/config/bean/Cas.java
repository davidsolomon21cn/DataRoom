package com.gccloud.gcpaas.dataroom.core.config.bean;

import lombok.Data;

import java.time.Duration;

/**
 * CAS 单点登录配置。
 */
@Data
public class Cas {

    private Boolean enable = false;

    private String serverUrlPrefix = "";

    private String service = "";

    private String serviceValidateSuffix = "/p3/serviceValidate";

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(5);
}

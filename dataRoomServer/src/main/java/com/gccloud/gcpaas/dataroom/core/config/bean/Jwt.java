package com.gccloud.gcpaas.dataroom.core.config.bean;

import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;

@Data
public class Jwt {
    /**
     * 颁发jwt者
     */
    private String issuer;
    /**
     * 密钥
     */
    private String secret;
    /**
     * 签名算法
     */
    private String alg = SignatureAlgorithm.HS256.getValue();
    /**
     * jwt时效（单位为秒）
     */
    private Long expiration = 36000L;
    /**
     * tokenKey
     */
    private String tokenKey = "dataRoomToken";
}

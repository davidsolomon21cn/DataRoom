package com.gccloud.gcpaas.dataroom.core.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.config.bean.Jwt;
import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenService {

    @Resource
    private DataRoomConfig dataRoomConfig;
    /**
     * 缓存token
     */
    private static Cache<String, String> TOKEN_CACHE = null;

    @PostConstruct
    public void init() {
        TOKEN_CACHE = Caffeine.newBuilder()
                .expireAfterWrite(dataRoomConfig.getJwt().getExpiration() + 60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    /**
     * 生成指定用户的token
     *
     * @param username
     * @return
     */
    public String createToken(String username) {
        Jwt jwtConfig = dataRoomConfig.getJwt();
        Map<String, Object> claims = new HashMap<>(16);
        String tid = UUID.randomUUID().toString().replace("-", "");
        claims.put("account", username);
        claims.put("tid", tid);
        JwtBuilder builder = Jwts.builder().signWith(SignatureAlgorithm.forName(jwtConfig.getAlg()), jwtConfig.getSecret()).setClaims(claims).setIssuer(jwtConfig.getIssuer()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration() * 1000L));
        String id = IdWorker.getIdStr();
        builder.setId(id);
        String token = builder.compact();
        TOKEN_CACHE.put(tid, token);
        return token;
    }

    public String getAccountFromToken(String accessToken) {
        Jwt jwtConfig = dataRoomConfig.getJwt();
        Claims claims = Jwts.parser().setSigningKey(jwtConfig.getSecret()).build().parseClaimsJws(accessToken).getBody();
        String cachedToken = TOKEN_CACHE.getIfPresent(claims.get("tid").toString());
        if (!Objects.equals(accessToken, cachedToken)) {
            throw new DataRoomException("认证失败", 401);
        }
        // 解析token，然后获取用户相关信息
        String account = claims.get("account", String.class);
        if (StringUtils.isBlank(account)) {
            throw new DataRoomException("认证失败", 401);
        }
        return account;
    }
}

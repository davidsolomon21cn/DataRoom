package com.gccloud.gcpaas.core.user.service;

import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;
import com.gccloud.gcpaas.dataroom.core.user.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    @Test
    void removeTokenInvalidatesCachedToken() {
        DataRoomConfig dataRoomConfig = new DataRoomConfig();
        dataRoomConfig.getJwt().setIssuer("dataroom-test");
        dataRoomConfig.getJwt().setSecret("8cgZ3e8BGbj+GDyZW2vs4A5/qmDfshHLEm6FrciK3eI=");
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "dataRoomConfig", dataRoomConfig);
        tokenService.init();
        String token = tokenService.createToken("alice");

        assertEquals("alice", tokenService.getAccountFromToken(token));

        tokenService.removeToken(token);

        DataRoomException exception = assertThrows(
                DataRoomException.class,
                () -> tokenService.getAccountFromToken(token)
        );
        assertEquals(401, exception.getCode());
    }
}

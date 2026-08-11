package com.gccloud.gcpaas.core.cas;

import com.gccloud.gcpaas.dataroom.core.cas.CasAuthenticationException;
import com.gccloud.gcpaas.dataroom.core.cas.CasErrorCode;
import com.gccloud.gcpaas.dataroom.core.cas.CasLoginResult;
import com.gccloud.gcpaas.dataroom.core.cas.CasLoginService;
import com.gccloud.gcpaas.dataroom.core.cas.CasTicketValidator;
import com.gccloud.gcpaas.dataroom.core.constant.UserStatus;
import com.gccloud.gcpaas.dataroom.core.entity.UserEntity;
import com.gccloud.gcpaas.dataroom.core.user.service.TokenService;
import com.gccloud.gcpaas.dataroom.core.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CasLoginServiceTest {

    @Test
    void loginCreatesLocalTokenForActiveExistingUser() {
        CasTicketValidator validator = mock(CasTicketValidator.class);
        UserService userService = mock(UserService.class);
        TokenService tokenService = mock(TokenService.class);
        UserEntity user = user("alice", UserStatus.NORMAL, null);
        when(validator.validate("ST-valid")).thenReturn("alice");
        when(userService.getByAccount("alice")).thenReturn(user);
        when(tokenService.createToken("alice")).thenReturn("local-jwt");

        CasLoginResult result = new CasLoginService(validator, userService, tokenService).login("ST-valid");

        assertEquals("alice", result.account());
        assertEquals("local-jwt", result.token());
    }

    @Test
    void loginRejectsUnknownLocalUserWithoutCreatingToken() {
        CasTicketValidator validator = mock(CasTicketValidator.class);
        UserService userService = mock(UserService.class);
        TokenService tokenService = mock(TokenService.class);
        when(validator.validate("ST-valid")).thenReturn("missing");
        when(userService.getByAccount("missing")).thenReturn(null);

        CasAuthenticationException exception = assertThrows(CasAuthenticationException.class,
                () -> new CasLoginService(validator, userService, tokenService).login("ST-valid"));

        assertEquals(CasErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(tokenService, never()).createToken("missing");
    }

    @Test
    void loginRejectsDisabledLockedAndExpiredUsers() {
        for (UserEntity user : new UserEntity[]{
                user("disabled", UserStatus.DISABLED, null),
                user("locked", UserStatus.LOCKED, new Date(System.currentTimeMillis() + 60_000L)),
                user("expired", UserStatus.NORMAL, new Date(System.currentTimeMillis() - 60_000L))
        }) {
            CasTicketValidator validator = mock(CasTicketValidator.class);
            UserService userService = mock(UserService.class);
            TokenService tokenService = mock(TokenService.class);
            when(validator.validate("ST-valid")).thenReturn(user.getAccount());
            when(userService.getByAccount(user.getAccount())).thenReturn(user);

            CasAuthenticationException exception = assertThrows(CasAuthenticationException.class,
                    () -> new CasLoginService(validator, userService, tokenService).login("ST-valid"));

            assertEquals(CasErrorCode.USER_UNAVAILABLE, exception.getErrorCode());
            verify(tokenService, never()).createToken(user.getAccount());
        }
    }

    private static UserEntity user(String account, UserStatus status, Date expireDate) {
        UserEntity user = new UserEntity();
        user.setAccount(account);
        user.setStatus(status);
        user.setExpireDate(expireDate);
        return user;
    }
}

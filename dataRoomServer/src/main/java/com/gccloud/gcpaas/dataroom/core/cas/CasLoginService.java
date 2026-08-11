package com.gccloud.gcpaas.dataroom.core.cas;

import com.gccloud.gcpaas.dataroom.core.constant.UserStatus;
import com.gccloud.gcpaas.dataroom.core.entity.UserEntity;
import com.gccloud.gcpaas.dataroom.core.user.service.TokenService;
import com.gccloud.gcpaas.dataroom.core.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class CasLoginService {

    private final CasTicketValidator ticketValidator;
    private final UserService userService;
    private final TokenService tokenService;

    public CasLoginService(CasTicketValidator ticketValidator, UserService userService, TokenService tokenService) {
        this.ticketValidator = ticketValidator;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    public CasLoginResult login(String ticket) {
        String account = ticketValidator.validate(ticket);
        UserEntity user = userService.getByAccount(account);
        if (user == null) {
            throw new CasAuthenticationException(CasErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != UserStatus.NORMAL || UserService.isExpired(user)) {
            throw new CasAuthenticationException(CasErrorCode.USER_UNAVAILABLE);
        }
        return new CasLoginResult(account, tokenService.createToken(account));
    }
}

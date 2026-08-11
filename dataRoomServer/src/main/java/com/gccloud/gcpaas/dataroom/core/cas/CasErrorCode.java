package com.gccloud.gcpaas.dataroom.core.cas;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CasErrorCode {

    DISABLED("disabled"),
    TICKET_MISSING("ticketMissing"),
    TICKET_INVALID("ticketInvalid"),
    SERVICE_UNAVAILABLE("serviceUnavailable"),
    USER_NOT_FOUND("userNotFound"),
    USER_UNAVAILABLE("userUnavailable"),
    LOGIN_ERROR("loginError");

    private final String code;
}

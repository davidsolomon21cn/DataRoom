package com.gccloud.gcpaas.dataroom.core.cas;

import lombok.Getter;

@Getter
public class CasAuthenticationException extends RuntimeException {

    private final CasErrorCode errorCode;

    public CasAuthenticationException(CasErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public CasAuthenticationException(CasErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
    }
}

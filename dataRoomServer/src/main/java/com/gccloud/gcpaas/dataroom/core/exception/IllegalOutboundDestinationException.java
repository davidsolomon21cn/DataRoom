package com.gccloud.gcpaas.dataroom.core.exception;

public class IllegalOutboundDestinationException extends DataRoomException {

    public IllegalOutboundDestinationException() {
        super("目的地址非法，禁止访问");
    }

    public IllegalOutboundDestinationException(String message) {
        super(message);
    }
}

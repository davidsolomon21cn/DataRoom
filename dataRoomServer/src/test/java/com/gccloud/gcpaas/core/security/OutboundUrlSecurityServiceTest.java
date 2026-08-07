package com.gccloud.gcpaas.core.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.config.bean.OutboundHttp;
import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;
import com.gccloud.gcpaas.dataroom.core.security.OutboundUrlSecurityService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundUrlSecurityServiceTest {

    @Test
    void returnsOriginalPublicUrlWhenDestinationIsSafe() {
        OutboundUrlSecurityService service = service(List.of());
        String url = "https://8.8.8.8/dns-query?name=example.com";

        assertEquals(url, service.validateAndResolve(url, Set.of("http", "https")));
        assertEquals("wss://8.8.8.8/socket",
                service.validateAndResolve("wss://8.8.8.8/socket", Set.of("ws", "wss")));
    }

    @Test
    void rejectsSchemeOutsideCallerAllowlist() {
        OutboundUrlSecurityService service = service(List.of());

        DataRoomException exception = assertThrows(DataRoomException.class,
                () -> service.validateAndResolve("ws://8.8.8.8/socket", Set.of("http", "https")));

        assertEquals("目的地址使用未允许的协议 ws，禁止访问", exception.getMessage());
    }

    @Test
    void rejectsLoopbackPrivateAndLinkLocalDestinations() {
        OutboundUrlSecurityService service = service(List.of());

        assertIllegal(service, "http://127.0.0.1:8080/actuator");
        assertIllegal(service, "http://localhost:8080/actuator");
        assertIllegal(service, "http://10.0.0.1/internal");
        assertIllegal(service, "http://169.254.169.254/latest/meta-data");
        assertIllegal(service, "http://[::1]/internal");
        assertIllegal(service, "http://[fc00::1]/internal");
    }

    @Test
    void logsSpecificReasonWhenLoopbackDestinationIsRejected() {
        OutboundUrlSecurityService service = service(List.of());
        Logger logger = (Logger) LoggerFactory.getLogger(OutboundUrlSecurityService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            DataRoomException exception = assertThrows(DataRoomException.class,
                    () -> service.validateAndResolve("http://127.0.0.1:8080/internal", Set.of("http", "https")));

            assertEquals("127.0.0.1:8080 地址属于回环地址，禁止访问", exception.getMessage());
            assertTrue(appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("127.0.0.1:8080 地址属于回环地址，禁止访问")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void formatsIpv6DestinationForDirectWhitelistUse() {
        OutboundUrlSecurityService service = service(List.of());

        DataRoomException exception = assertThrows(DataRoomException.class,
                () -> service.validateAndResolve("http://[::1]/internal", Set.of("http", "https")));

        assertEquals("[::1]:80 地址属于回环地址，禁止访问", exception.getMessage());
    }

    @Test
    void allowsOnlyExactConfiguredInternalHostAndPort() {
        OutboundUrlSecurityService service = service(List.of("127.0.0.1:9200"));
        String allowedUrl = "http://127.0.0.1:9200/orders/_search";

        assertEquals(allowedUrl, service.validateAndResolve(allowedUrl, Set.of("http", "https")));
        assertIllegal(service, "http://127.0.0.1:8080/actuator");
    }

    @Test
    void rejectsUrlWithEmbeddedUserInformation() {
        OutboundUrlSecurityService service = service(List.of());

        assertIllegal(service, "http://user:password@8.8.8.8/data");
    }

    private static OutboundUrlSecurityService service(List<String> allowedInternalTargets) {
        OutboundHttp outboundHttp = new OutboundHttp();
        outboundHttp.setAllowedInternalTargets(allowedInternalTargets);
        DataRoomConfig config = new DataRoomConfig();
        config.setOutboundHttp(outboundHttp);
        return new OutboundUrlSecurityService(config);
    }

    private static void assertIllegal(OutboundUrlSecurityService service, String url) {
        DataRoomException exception = assertThrows(DataRoomException.class,
                () -> service.validateAndResolve(url, Set.of("http", "https")));
        assertTrue(exception.getMessage().endsWith("禁止访问"));
    }
}

package com.gccloud.gcpaas.core.cas;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gccloud.gcpaas.dataroom.core.cas.CasAuthenticationException;
import com.gccloud.gcpaas.dataroom.core.cas.CasErrorCode;
import com.gccloud.gcpaas.dataroom.core.cas.CasTicketValidator;
import com.gccloud.gcpaas.dataroom.core.config.bean.Cas;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CasTicketValidatorTest {

    @Test
    void validateReturnsUserFromCas3SuccessResponseAndEncodesQueryParameters() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        Cas cas = casConfig();
        CasTicketValidator validator = new CasTicketValidator(restTemplate, cas);
        server.expect(requestTo("https://cas.example.com/cas/p3/serviceValidate?service=https%3A%2F%2Fapp.example.com%2FdataRoom%2Fcas%2Flogin%3Fsource%3Dcas&ticket=ST-1%2Bvalue"))
                .andRespond(withSuccess("""
                        <cas:serviceResponse xmlns:cas="http://www.yale.edu/tp/cas">
                          <cas:authenticationSuccess>
                            <cas:user>alice</cas:user>
                          </cas:authenticationSuccess>
                        </cas:serviceResponse>
                        """, MediaType.APPLICATION_XML));

        String username = validator.validate("ST-1+value");

        assertEquals("alice", username);
        server.verify();
    }

    @Test
    void validateMapsAuthenticationFailureToTicketInvalid() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        CasTicketValidator validator = new CasTicketValidator(restTemplate, casConfig());
        server.expect(requestTo("https://cas.example.com/cas/p3/serviceValidate?service=https%3A%2F%2Fapp.example.com%2FdataRoom%2Fcas%2Flogin%3Fsource%3Dcas&ticket=ST-invalid"))
                .andRespond(withSuccess("""
                        <cas:serviceResponse xmlns:cas="http://www.yale.edu/tp/cas">
                          <cas:authenticationFailure code="INVALID_TICKET">Ticket not recognized</cas:authenticationFailure>
                        </cas:serviceResponse>
                        """, MediaType.APPLICATION_XML));

        CasAuthenticationException exception = assertThrows(CasAuthenticationException.class,
                () -> validator.validate("ST-invalid"));

        assertEquals(CasErrorCode.TICKET_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void validateRejectsXmlWithDoctypeWithoutResolvingExternalEntity() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        CasTicketValidator validator = new CasTicketValidator(restTemplate, casConfig());
        server.expect(requestTo("https://cas.example.com/cas/p3/serviceValidate?service=https%3A%2F%2Fapp.example.com%2FdataRoom%2Fcas%2Flogin%3Fsource%3Dcas&ticket=ST-xxe"))
                .andRespond(withSuccess("""
                        <?xml version="1.0"?>
                        <!DOCTYPE serviceResponse [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                        <cas:serviceResponse xmlns:cas="http://www.yale.edu/tp/cas">
                          <cas:authenticationSuccess><cas:user>&xxe;</cas:user></cas:authenticationSuccess>
                        </cas:serviceResponse>
                        """, MediaType.APPLICATION_XML));

        CasAuthenticationException exception = assertThrows(CasAuthenticationException.class,
                () -> validator.validate("ST-xxe"));

        assertEquals(CasErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
        server.verify();
    }

    @Test
    void validateRedactsTicketFromTransportExceptionLog() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenThrow(new ResourceAccessException(
                        "I/O error on GET request for https://cas.example.com/cas/p3/serviceValidate?ticket=ST-secret"));
        Logger logger = (Logger) LoggerFactory.getLogger(CasTicketValidator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            CasTicketValidator validator = new CasTicketValidator(restTemplate, casConfig());

            assertThrows(CasAuthenticationException.class, () -> validator.validate("ST-secret"));

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + right);
            assertFalse(logs.contains("ST-secret"));
            assertTrue(logs.contains(ResourceAccessException.class.getName()));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static Cas casConfig() {
        Cas cas = new Cas();
        cas.setServerUrlPrefix("https://cas.example.com/cas");
        cas.setServiceValidateSuffix("/p3/serviceValidate");
        cas.setService("https://app.example.com/dataRoom/cas/login?source=cas");
        return cas;
    }
}

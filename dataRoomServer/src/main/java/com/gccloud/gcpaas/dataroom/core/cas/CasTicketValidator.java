package com.gccloud.gcpaas.dataroom.core.cas;

import com.gccloud.gcpaas.dataroom.core.config.bean.Cas;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class CasTicketValidator {

    private final RestTemplate restTemplate;
    private final Cas cas;

    public CasTicketValidator(@Qualifier("casRestTemplate") RestTemplate restTemplate, Cas cas) {
        this.restTemplate = restTemplate;
        this.cas = cas;
    }

    public String validate(String ticket) {
        URI validateUri = buildValidateUri(ticket);
        ResponseEntity<String> response;
        long start = System.currentTimeMillis();
        try {
            response = restTemplate.getForEntity(validateUri, String.class);
        } catch (RestClientException e) {
            log.error(redactedStackTrace(e, ticket));
            throw new CasAuthenticationException(CasErrorCode.SERVICE_UNAVAILABLE, e);
        }
        log.info("CAS ticket validation completed, endpoint: {}, elapsed: {} ms",
                validationEndpoint(), System.currentTimeMillis() - start);
        if (!response.getStatusCode().is2xxSuccessful() || StringUtils.isBlank(response.getBody())) {
            throw new CasAuthenticationException(CasErrorCode.SERVICE_UNAVAILABLE);
        }
        return parseUsername(response.getBody());
    }

    private URI buildValidateUri(String ticket) {
        String url = validationEndpoint()
                + "?service=" + encodeQueryParam(cas.getService())
                + "&ticket=" + encodeQueryParam(ticket);
        return URI.create(url);
    }

    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String redactedStackTrace(Throwable throwable, String ticket) {
        String stackTrace = ExceptionUtils.getStackTrace(throwable);
        stackTrace = StringUtils.replace(stackTrace, ticket, "[REDACTED]");
        stackTrace = StringUtils.replace(stackTrace, encodeQueryParam(ticket), "[REDACTED]");
        return stackTrace.replaceAll("(?i)(ticket=)[^&\\s]+", "$1[REDACTED]");
    }

    private String validationEndpoint() {
        return StringUtils.removeEnd(cas.getServerUrlPrefix(), "/")
                + "/" + StringUtils.removeStart(cas.getServiceValidateSuffix(), "/");
    }

    private String parseUsername(String responseBody) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            var documentBuilder = factory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            Document document = documentBuilder.parse(new InputSource(new StringReader(responseBody)));
            if (document.getElementsByTagNameNS("*", "authenticationFailure").getLength() > 0) {
                throw new CasAuthenticationException(CasErrorCode.TICKET_INVALID);
            }
            NodeList userNodes = document.getElementsByTagNameNS("*", "user");
            if (userNodes.getLength() == 0 || StringUtils.isBlank(userNodes.item(0).getTextContent())) {
                throw new CasAuthenticationException(CasErrorCode.TICKET_INVALID);
            }
            return userNodes.item(0).getTextContent().trim();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new CasAuthenticationException(CasErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}

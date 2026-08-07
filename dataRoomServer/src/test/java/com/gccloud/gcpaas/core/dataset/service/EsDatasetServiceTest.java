package com.gccloud.gcpaas.core.dataset.service;

import com.gccloud.gcpaas.dataroom.core.bean.Rsa;
import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.dataset.DatasetRunRequest;
import com.gccloud.gcpaas.dataroom.core.dataset.bean.EsDataset;
import com.gccloud.gcpaas.dataroom.core.dataset.service.EsDatasetService;
import com.gccloud.gcpaas.dataroom.core.datasource.bean.EsDatasource;
import com.gccloud.gcpaas.dataroom.core.datasource.service.DatasourceService;
import com.gccloud.gcpaas.dataroom.core.entity.DataSourceEntity;
import com.gccloud.gcpaas.dataroom.core.entity.DatasetEntity;
import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;
import com.gccloud.gcpaas.dataroom.core.exception.IllegalOutboundDestinationException;
import com.gccloud.gcpaas.dataroom.core.security.OutboundUrlSecurityService;
import com.gccloud.gcpaas.dataroom.core.util.RsaUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EsDatasetServiceTest {

    @Test
    void runPropagatesIllegalDestinationWithoutSendingRequest() {
        EsDatasetService service = new EsDatasetService() {
            @Override
            public Map<String, Object> getDefaultInputParam() {
                return Map.of();
            }
        };
        RestTemplate restTemplate = mock(RestTemplate.class);
        DatasourceService datasourceService = mock(DatasourceService.class);
        OutboundUrlSecurityService securityService = mock(OutboundUrlSecurityService.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "datasourceService", datasourceService);
        ReflectionTestUtils.setField(service, "outboundUrlSecurityService", securityService);
        EsDatasource datasource = new EsDatasource();
        datasource.setBaseUrl("http://127.0.0.1:9200");
        DataSourceEntity datasourceEntity = new DataSourceEntity();
        datasourceEntity.setDataSource(datasource);
        when(datasourceService.getByCode("es-source")).thenReturn(datasourceEntity);
        String url = "http://127.0.0.1:9200/orders/_search";
        when(securityService.validateAndResolve(url, Set.of("http", "https")))
                .thenThrow(new IllegalOutboundDestinationException("127.0.0.1:9200 地址属于回环地址，禁止访问"));
        EsDataset dataset = new EsDataset();
        dataset.setPath("/orders/_search");
        dataset.setMethod("POST");
        dataset.setBody("{}");
        DatasetEntity entity = new DatasetEntity();
        entity.setDataSourceCode("es-source");
        entity.setDataset(dataset);

        DataRoomException exception = assertThrows(DataRoomException.class,
                () -> service.run(new DatasetRunRequest(), entity));

        assertEquals("127.0.0.1:9200 地址属于回环地址，禁止访问", exception.getMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void runValidatesFinalUrlBeforeSendingRequest() {
        EsDatasetService service = new EsDatasetService() {
            @Override
            public Map<String, Object> getDefaultInputParam() {
                return Map.of();
            }
        };
        RestTemplate restTemplate = mock(RestTemplate.class);
        DatasourceService datasourceService = mock(DatasourceService.class);
        OutboundUrlSecurityService securityService = mock(OutboundUrlSecurityService.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "datasourceService", datasourceService);
        ReflectionTestUtils.setField(service, "outboundUrlSecurityService", securityService);

        EsDatasource datasource = new EsDatasource();
        datasource.setBaseUrl("http://8.8.8.8:9200");
        DataSourceEntity datasourceEntity = new DataSourceEntity();
        datasourceEntity.setDataSource(datasource);
        when(datasourceService.getByCode("es-source")).thenReturn(datasourceEntity);
        String finalUrl = "http://8.8.8.8:9200/orders/_search";
        when(securityService.validateAndResolve(finalUrl, Set.of("http", "https"))).thenReturn(finalUrl);
        when(restTemplate.exchange(org.mockito.ArgumentMatchers.eq(finalUrl), org.mockito.ArgumentMatchers.eq(HttpMethod.POST), any(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        EsDataset dataset = new EsDataset();
        dataset.setPath("/orders/_search");
        dataset.setMethod("POST");
        dataset.setBody("{}");
        DatasetEntity entity = new DatasetEntity();
        entity.setDataSourceCode("es-source");
        entity.setDataset(dataset);

        service.run(new DatasetRunRequest(), entity);

        verify(securityService).validateAndResolve(finalUrl, Set.of("http", "https"));
    }

    @Test
    void buildRequestUrlJoinsBaseUrlAndConfiguredPath() throws Exception {
        Method method = serviceMethod("buildRequestUrl", String.class, String.class);

        assertEquals(
                "http://localhost:9200/orders/_search",
                method.invoke(null, "http://localhost:9200", "/orders/_search")
        );
        assertEquals(
                "http://localhost:9200/orders/_search",
                method.invoke(null, "http://localhost:9200/", "orders/_search")
        );
        assertEquals(
                "http://localhost:9200/_cat/indices/orders?v",
                method.invoke(null, "http://localhost:9200/", "/_cat/indices/orders?v")
        );
    }

    @Test
    void buildAuthorizationHeaderSupportsEsAuthModes() throws Exception {
        Method method = serviceMethod(
                "buildAuthorizationHeader",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );

        assertNull(method.invoke(null, "none", "elastic", "pwd", "token", "api-key"));
        assertEquals(
                "Basic ZWxhc3RpYzpwd2Q=",
                method.invoke(null, "basic", "elastic", "pwd", "", "")
        );
        assertEquals(
                "Bearer bearer-token",
                method.invoke(null, "bearer", "", "", "bearer-token", "")
        );
        assertEquals(
                "ApiKey encoded-api-key",
                method.invoke(null, "apiKey", "", "", "", "encoded-api-key")
        );
    }

    @Test
    void buildHeadersDecryptsEncryptedEsCredentialValues() throws Exception {
        Rsa rsa = RsaUtils.generateRsaKeyPair();
        assertNotNull(rsa);
        DataRoomConfig dataRoomConfig = new DataRoomConfig();
        dataRoomConfig.setPrivateKey(rsa.getPrivateKey());
        EsDatasetService service = new EsDatasetService();
        ReflectionTestUtils.setField(service, "dataRoomConfig", dataRoomConfig);

        EsDatasource basicDatasource = new EsDatasource();
        basicDatasource.setAuthType("basic");
        basicDatasource.setUsername("elastic");
        basicDatasource.setPassword(RsaUtils.encryptByPublicKey("pwd", rsa.getPublicKey()));

        HttpHeaders basicHeaders = buildHeaders(service, basicDatasource);
        assertEquals("Basic ZWxhc3RpYzpwd2Q=", basicHeaders.getFirst(HttpHeaders.AUTHORIZATION));

        EsDatasource bearerDatasource = new EsDatasource();
        bearerDatasource.setAuthType("bearer");
        bearerDatasource.setBearerToken(RsaUtils.encryptByPublicKey("bearer-token", rsa.getPublicKey()));

        HttpHeaders bearerHeaders = buildHeaders(service, bearerDatasource);
        assertEquals("Bearer bearer-token", bearerHeaders.getFirst(HttpHeaders.AUTHORIZATION));

        EsDatasource apiKeyDatasource = new EsDatasource();
        apiKeyDatasource.setAuthType("apiKey");
        apiKeyDatasource.setApiKey(RsaUtils.encryptByPublicKey("encoded-api-key", rsa.getPublicKey()));

        HttpHeaders apiKeyHeaders = buildHeaders(service, apiKeyDatasource);
        assertEquals("ApiKey encoded-api-key", apiKeyHeaders.getFirst(HttpHeaders.AUTHORIZATION));
    }

    private static Method serviceMethod(String methodName, Class<?>... parameterTypes) throws Exception {
        Class<?> serviceClass = Class.forName("com.gccloud.gcpaas.dataroom.core.dataset.service.EsDatasetService");
        Method method = serviceClass.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static HttpHeaders buildHeaders(EsDatasetService service, EsDatasource datasource) throws Exception {
        Method method = serviceMethod("buildHeaders", EsDatasource.class);
        return (HttpHeaders) method.invoke(service, datasource);
    }
}

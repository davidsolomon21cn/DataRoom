package com.gccloud.gcpaas.core.cas;

import com.gccloud.gcpaas.dataroom.core.cas.CasAuthenticationException;
import com.gccloud.gcpaas.dataroom.core.cas.CasController;
import com.gccloud.gcpaas.dataroom.core.cas.CasErrorCode;
import com.gccloud.gcpaas.dataroom.core.cas.CasLoginResult;
import com.gccloud.gcpaas.dataroom.core.cas.CasLoginService;
import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.config.bean.Cas;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CasControllerTest {

    @Test
    void loginRedirectsSuccessfulAuthenticationToUiWithEncodedToken() throws Exception {
        DataRoomConfig config = dataRoomConfig(true, "customToken", "https://app.example.com/#/");
        CasLoginService loginService = mock(CasLoginService.class);
        when(loginService.login("ST-valid")).thenReturn(new CasLoginResult("alice", "jwt+value/="));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CasController(config, loginService).login("ST-valid", response);

        assertEquals("https://app.example.com/#/?customToken=jwt%2Bvalue%2F%3D", response.getRedirectedUrl());
    }

    @Test
    void loginUsesConfiguredHistoryUiUrlWithoutInspectingRouterMode() throws Exception {
        DataRoomConfig config = dataRoomConfig(true, "customToken", "https://app.example.com/");
        CasLoginService loginService = mock(CasLoginService.class);
        when(loginService.login("ST-valid")).thenReturn(new CasLoginResult("alice", "jwt-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CasController(config, loginService).login("ST-valid", response);

        assertEquals("https://app.example.com/?customToken=jwt-token", response.getRedirectedUrl());
    }

    @Test
    void loginRedirectsMissingTicketToErrorPageWithoutCallingService() throws Exception {
        Cas cas = casConfig(true);
        CasLoginService loginService = mock(CasLoginService.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CasController(dataRoomConfig(cas), loginService).login(" ", response);

        assertEquals("https://app.example.com/#/error?code=ticketMissing", response.getRedirectedUrl());
        verify(loginService, never()).login(" ");
    }

    @Test
    void loginErrorUsesConfiguredHistoryUiUrlWithoutInspectingRouterMode() throws Exception {
        Cas cas = casConfig(true);
        CasLoginService loginService = mock(CasLoginService.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CasController(dataRoomConfig(cas, "https://app.example.com/"), loginService).login(" ", response);

        assertEquals("https://app.example.com/error?code=ticketMissing", response.getRedirectedUrl());
    }

    @Test
    void loginRedirectsKnownAuthenticationFailureToStableErrorCode() throws Exception {
        Cas cas = casConfig(true);
        CasLoginService loginService = mock(CasLoginService.class);
        when(loginService.login("ST-invalid"))
                .thenThrow(new CasAuthenticationException(CasErrorCode.USER_NOT_FOUND));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CasController(dataRoomConfig(cas), loginService).login("ST-invalid", response);

        assertEquals("https://app.example.com/#/error?code=userNotFound", response.getRedirectedUrl());
    }

    @Test
    void loginRedirectsToDisabledErrorWhenCasIsOff() throws Exception {
        Cas cas = casConfig(false);
        CasLoginService loginService = mock(CasLoginService.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CasController(dataRoomConfig(cas), loginService).login("ST-valid", response);

        assertEquals("https://app.example.com/#/error?code=disabled", response.getRedirectedUrl());
        verify(loginService, never()).login("ST-valid");
    }

    private static Cas casConfig(boolean enabled) {
        Cas cas = new Cas();
        cas.setEnable(enabled);
        return cas;
    }

    private static DataRoomConfig dataRoomConfig(boolean enabled, String tokenKey, String uiUrl) {
        DataRoomConfig config = dataRoomConfig(casConfig(enabled), uiUrl);
        config.getJwt().setTokenKey(tokenKey);
        return config;
    }

    private static DataRoomConfig dataRoomConfig(Cas cas) {
        return dataRoomConfig(cas, "https://app.example.com/#/");
    }

    private static DataRoomConfig dataRoomConfig(Cas cas, String uiUrl) {
        DataRoomConfig config = new DataRoomConfig();
        config.setCas(cas);
        config.setUiUrl(uiUrl);
        return config;
    }
}

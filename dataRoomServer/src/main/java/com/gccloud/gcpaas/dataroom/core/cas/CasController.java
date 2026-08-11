package com.gccloud.gcpaas.dataroom.core.cas;

import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.config.bean.Cas;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequestMapping("/cas")
public class CasController {

    private final DataRoomConfig dataRoomConfig;
    private final CasLoginService loginService;

    public CasController(DataRoomConfig dataRoomConfig, CasLoginService loginService) {
        this.dataRoomConfig = dataRoomConfig;
        this.loginService = loginService;
    }

    @GetMapping("/login")
    public void login(@RequestParam(required = false) String ticket, HttpServletResponse response) throws IOException {
        long start = System.currentTimeMillis();
        Cas cas = dataRoomConfig.getCas();
        if (!Boolean.TRUE.equals(cas.getEnable())) {
            redirectError(response, CasErrorCode.DISABLED);
            return;
        }
        if (StringUtils.isBlank(ticket)) {
            redirectError(response, CasErrorCode.TICKET_MISSING);
            return;
        }
        try {
            CasLoginResult result = loginService.login(ticket);
            log.info("CAS login succeeded, account: {}, elapsed: {} ms", result.account(), System.currentTimeMillis() - start);
            response.sendRedirect(uiUrl() + "?"
                    + URLEncoder.encode(dataRoomConfig.getJwt().getTokenKey(), StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(result.token(), StandardCharsets.UTF_8));
        } catch (CasAuthenticationException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("CAS login failed, code: {}, elapsed: {} ms", e.getErrorCode().getCode(), System.currentTimeMillis() - start);
            redirectError(response, e.getErrorCode());
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("CAS login failed, code: {}, elapsed: {} ms", CasErrorCode.LOGIN_ERROR.getCode(), System.currentTimeMillis() - start);
            redirectError(response, CasErrorCode.LOGIN_ERROR);
        }
    }

    private void redirectError(HttpServletResponse response, CasErrorCode errorCode) throws IOException {
        response.sendRedirect(uiUrl() + "error?code=" + errorCode.getCode());
    }

    private String uiUrl() {
        return StringUtils.appendIfMissing(StringUtils.defaultString(dataRoomConfig.getUiUrl()), "/");
    }
}

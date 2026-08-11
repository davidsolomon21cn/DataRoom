package com.gccloud.gcpaas.core.shiro;

import com.gccloud.gcpaas.dataroom.core.shiro.ShiroConfiguration;
import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.config.ShiroFilterConfiguration;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ShiroConfigurationCasTest {

    @Test
    void casLoginCallbackIsAnonymousWithoutChangingDefaultAuthenticationRule() {
        ShiroFilterFactoryBean filter = new ShiroConfiguration().shiroFilter(
                mock(SecurityManager.class),
                new ShiroFilterConfiguration(),
                dataRoomConfig()
        );

        assertEquals("anon", filter.getFilterChainDefinitionMap().get("/cas/login"));
        assertEquals("OAUTH", filter.getFilterChainDefinitionMap().get("/**"));
    }

    private static DataRoomConfig dataRoomConfig() {
        DataRoomConfig config = new DataRoomConfig();
        config.getJwt().setTokenKey("customToken");
        return config;
    }
}

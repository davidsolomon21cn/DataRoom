package com.gccloud.gcpaas.dataroom.core.config;

import com.gccloud.gcpaas.dataroom.core.script.DisabledScriptExecutor;
import com.gccloud.gcpaas.dataroom.core.script.ScriptExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ScriptExecutorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ScriptExecutor.class)
    public ScriptExecutor disabledScriptExecutor() {
        return new DisabledScriptExecutor();
    }
}

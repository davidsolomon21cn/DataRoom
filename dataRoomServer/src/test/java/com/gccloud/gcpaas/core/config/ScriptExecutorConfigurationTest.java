package com.gccloud.gcpaas.core.config;

import com.gccloud.gcpaas.dataroom.core.config.ScriptExecutorConfiguration;
import com.gccloud.gcpaas.dataroom.core.script.DisabledScriptExecutor;
import com.gccloud.gcpaas.dataroom.core.script.ScriptExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptExecutorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void registersDisabledExecutorWhenNoCustomExecutorExists() {
        contextRunner
                .withUserConfiguration(ScriptExecutorConfiguration.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(ScriptExecutor.class)
                        .getBean(ScriptExecutor.class)
                        .isInstanceOf(DisabledScriptExecutor.class));
    }

    @Test
    void backsOffWhenCustomExecutorExists() {
        contextRunner
                .withUserConfiguration(
                        CustomScriptExecutorConfiguration.class,
                        ScriptExecutorConfiguration.class
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(ScriptExecutor.class)
                        .getBean(ScriptExecutor.class)
                        .isSameAs(context.getBean("customScriptExecutor")));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomScriptExecutorConfiguration {

        @Bean
        ScriptExecutor customScriptExecutor() {
            return request -> request.bindings();
        }
    }
}

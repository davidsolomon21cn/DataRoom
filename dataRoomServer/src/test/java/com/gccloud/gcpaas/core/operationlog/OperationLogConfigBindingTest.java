package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationLogConfigBindingTest {

    @Test
    void bindsOperationLogExcludePathsFromDocumentedPropertyName() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "dataroom.operation-log.exclude-paths[0]", "/a/**",
                "dataroom.operation-log.exclude-paths[1]", "/dataRoom/captcha/**"
        ));

        DataRoomConfig config = new Binder(source)
                .bind("dataroom", Bindable.of(DataRoomConfig.class))
                .orElseThrow(() -> new AssertionError("dataroom 配置应可绑定"));

        assertEquals(List.of("/a/**", "/dataRoom/captcha/**"),
                config.getOperationLog().getExcludePaths());
    }
}

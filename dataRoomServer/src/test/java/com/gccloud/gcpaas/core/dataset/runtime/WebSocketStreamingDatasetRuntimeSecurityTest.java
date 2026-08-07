package com.gccloud.gcpaas.core.dataset.runtime;

import com.gccloud.gcpaas.dataroom.core.dataset.bean.WebSocketDataset;
import com.gccloud.gcpaas.dataroom.core.dataset.runtime.WebSocketStreamingDatasetRuntime;
import com.gccloud.gcpaas.dataroom.core.dataset.service.StreamingDatasetMessageProcessor;
import com.gccloud.gcpaas.dataroom.core.dataset.ws.RealtimeDatasetSessionRegistry;
import com.gccloud.gcpaas.dataroom.core.entity.DatasetEntity;
import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;
import com.gccloud.gcpaas.dataroom.core.security.OutboundUrlSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketStreamingDatasetRuntimeSecurityTest {

    @Test
    void externalWebSocketClientDoesNotFollowRedirects() {
        WebSocketDataset dataset = new WebSocketDataset();
        dataset.setUrl("wss://8.8.8.8/socket");
        DatasetEntity entity = new DatasetEntity();
        entity.setDataset(dataset);
        WebSocketStreamingDatasetRuntime runtime = new WebSocketStreamingDatasetRuntime(
                entity,
                Map.of(),
                mock(StreamingDatasetMessageProcessor.class),
                mock(RealtimeDatasetSessionRegistry.class),
                mock(OutboundUrlSecurityService.class)
        );

        StandardWebSocketClient client = (StandardWebSocketClient) ReflectionTestUtils.getField(runtime, "webSocketClient");

        assertEquals(0, client.getUserProperties().get("org.apache.tomcat.websocket.MAX_REDIRECTIONS"));
    }

    @Test
    void startValidatesUrlBeforeOpeningConnection() {
        String url = "ws://127.0.0.1:8080/internal";
        OutboundUrlSecurityService securityService = mock(OutboundUrlSecurityService.class);
        when(securityService.validateAndResolve(url, Set.of("ws", "wss")))
                .thenThrow(new DataRoomException("目的地址非法，禁止访问"));
        WebSocketDataset dataset = new WebSocketDataset();
        dataset.setUrl(url);
        DatasetEntity entity = new DatasetEntity();
        entity.setCode("dataset-1");
        entity.setDataset(dataset);
        WebSocketStreamingDatasetRuntime runtime = new WebSocketStreamingDatasetRuntime(
                entity,
                Map.of(),
                mock(StreamingDatasetMessageProcessor.class),
                mock(RealtimeDatasetSessionRegistry.class),
                securityService
        );

        assertThrows(DataRoomException.class, runtime::start);
        assertFalse(runtime.isRunning());
        verify(securityService).validateAndResolve(url, Set.of("ws", "wss"));
    }
}

package com.gccloud.gcpaas.core.dataset.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gccloud.gcpaas.dataroom.core.dataset.bean.WebSocketDataset;
import com.gccloud.gcpaas.dataroom.core.dataset.service.StreamingDatasetMessageProcessor;
import com.gccloud.gcpaas.dataroom.core.entity.DatasetEntity;
import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;
import com.gccloud.gcpaas.dataroom.core.script.DisabledScriptExecutor;
import com.gccloud.gcpaas.dataroom.core.script.ScriptExecutionRequest;
import com.gccloud.gcpaas.dataroom.core.script.ScriptExecutor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamingDatasetMessageProcessorTest {

    @Test
    void rejectsScriptWhenNoScriptExecutorIsInstalled() {
        StreamingDatasetMessageProcessor processor = new StreamingDatasetMessageProcessor(
                new ObjectMapper(), new DisabledScriptExecutor());

        DataRoomException exception = assertThrows(DataRoomException.class,
                () -> processor.process(null, "{\"value\":1}", "return payload", Map.of()));

        assertEquals("为了安全，默认关闭脚本执行权限，请自行引入脚本执行实现", exception.getMessage());
    }

    @Test
    void delegatesExistingBindingsToScriptExecutor() {
        AtomicReference<ScriptExecutionRequest> capturedRequest = new AtomicReference<>();
        ScriptExecutor scriptExecutor = request -> {
            capturedRequest.set(request);
            return request.bindings().get("payload");
        };
        StreamingDatasetMessageProcessor processor = new StreamingDatasetMessageProcessor(new ObjectMapper(), scriptExecutor);
        DatasetEntity datasetEntity = new DatasetEntity();
        WebSocketDataset dataset = new WebSocketDataset();
        dataset.setScript("return payload");
        datasetEntity.setCode("websocket-demo");
        datasetEntity.setDataset(dataset);

        Object result = processor.process(datasetEntity, dataset, "{\"value\":1}", Map.of("name", "demo"));

        assertEquals(Map.of("value", 1), result);
        ScriptExecutionRequest request = capturedRequest.get();
        assertEquals(dataset.getScript(), request.script());
        assertEquals("{\"value\":1}", request.bindings().get("message"));
        assertEquals(Map.of("value", 1), request.bindings().get("payload"));
        assertEquals(Map.of("name", "demo"), request.bindings().get("params"));
        assertEquals(datasetEntity, request.bindings().get("dataset"));
        assertEquals(dataset, request.bindings().get("datasetConfig"));
        assertEquals(dataset, request.bindings().get("streamingDataset"));
    }

    @Test
    void returnsParsedPayloadWithoutInvokingScriptExecutor() {
        StreamingDatasetMessageProcessor processor = new StreamingDatasetMessageProcessor(
                new ObjectMapper(), request -> {
                    throw new AssertionError("Blank scripts must not invoke the executor");
                });

        Object result = processor.process(null, "{\"value\":1}", "", Map.of());

        Map<?, ?> resultMap = assertInstanceOf(Map.class, result);
        assertEquals(1, resultMap.get("value"));
    }
}

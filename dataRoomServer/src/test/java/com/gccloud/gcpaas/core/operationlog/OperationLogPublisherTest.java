package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.entity.OperationLogEntity;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPersistService;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OperationLogPublisherTest {

    @Test
    void publishDispatchesContextToPersistService() {
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        Executor executor = Runnable::run;
        OperationLogPublisher publisher = new OperationLogPublisher(executor, persistService);

        OperationLogEntity context = new OperationLogEntity();
        context.setRequestUri("/dataRoom/dataset/run");

        publisher.publish(context);

        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(persistService).persist(captor.capture());
        assertEquals("/dataRoom/dataset/run", captor.getValue().getRequestUri());
    }

    @Test
    void publishIgnoresNullContext() {
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        OperationLogPublisher publisher = new OperationLogPublisher(Runnable::run, persistService);

        publisher.publish(null);

        verifyNoInteractions(persistService);
    }
}

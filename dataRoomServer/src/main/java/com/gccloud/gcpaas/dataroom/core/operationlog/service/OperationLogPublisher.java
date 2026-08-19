package com.gccloud.gcpaas.dataroom.core.operationlog.service;

import com.gccloud.gcpaas.dataroom.core.entity.OperationLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class OperationLogPublisher {

    private final Executor executor;
    private final OperationLogPersistService persistService;

    public void publish(OperationLogEntity entity) {
        if (entity == null) {
            return;
        }
        executor.execute(() -> {
            try {
                persistService.persist(entity);
            } catch (Exception e) {
                log.error("publish operation log failed", e);
            }
        });
    }
}

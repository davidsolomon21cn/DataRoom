package com.gccloud.gcpaas.dataroom.core.operationlog.service;

import com.gccloud.gcpaas.dataroom.core.entity.OperationLogEntity;
import com.gccloud.gcpaas.dataroom.core.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogPersistService {

    private final OperationLogMapper operationLogMapper;

    public void persist(OperationLogEntity entity) {
        try {
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}

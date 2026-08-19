package com.gccloud.gcpaas.dataroom.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "操作日志")
@TableName("dr_operation_log")
public class OperationLogEntity extends BaseEntity {

    private String traceId;
    private String operatorId;
    private String operatorName;
    private String operatorRole;
    /** 操作说明，取自 @Operation.summary() */
    private String operationSummary;
    /** 操作描述，取自 @Operation.description() */
    private String operationDescription;
    /** 业务模块，取自类级 @Tag.name() */
    private String businessModule;
    private String requestUri;
    private String requestMethod;
    private String clientIp;
    private String userAgent;
    private String contentType;
    private String queryParams;
    private String resultStatus;
    private Integer responseCode;
    private String responseMessage;
    private String exceptionType;
    private String exceptionStack;
    private Date requestTime;
    private Long durationMs;
}

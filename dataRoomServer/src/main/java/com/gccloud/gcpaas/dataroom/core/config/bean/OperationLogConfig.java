package com.gccloud.gcpaas.dataroom.core.config.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OperationLogConfig {

    /**
     * 不记录操作日志的 HTTP 请求路径，使用 Spring PathPattern 语法。
     */
    private List<String> excludePaths = new ArrayList<>();
}

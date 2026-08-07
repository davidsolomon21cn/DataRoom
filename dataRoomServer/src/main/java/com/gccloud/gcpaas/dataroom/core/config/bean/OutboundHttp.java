package com.gccloud.gcpaas.dataroom.core.config.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutboundHttp {

    /**
     * 允许访问的内部目标，使用 host:port 精确配置。
     */
    private List<String> allowedInternalTargets = new ArrayList<>();
}

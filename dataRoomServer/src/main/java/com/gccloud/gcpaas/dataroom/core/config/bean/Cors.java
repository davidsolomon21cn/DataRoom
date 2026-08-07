package com.gccloud.gcpaas.dataroom.core.config.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cors {

    private String pathPattern = "/**";

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

    private Boolean allowCredentials = true;

    private List<String> allowedMethods = new ArrayList<>(List.of("*"));

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private List<String> exposedHeaders = new ArrayList<>(List.of("*"));
}

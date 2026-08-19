package com.gccloud.gcpaas.dataroom.core.operationlog.service;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

public class OperationLogPathMatcher {

    private final List<PathPattern> excludePatterns;

    public OperationLogPathMatcher(List<String> excludePaths) {
        List<String> paths = excludePaths == null ? List.of() : excludePaths;
        this.excludePatterns = paths.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .map(PathPatternParser.defaultInstance::parse)
                .toList();
    }

    public boolean isExcluded(HttpServletRequest request) {
        if (request == null || excludePatterns.isEmpty()) {
            return false;
        }
        PathContainer requestPath = PathContainer.parsePath(resolveRequestPath(request));
        return excludePatterns.stream().anyMatch(pattern -> pattern.matches(requestPath));
    }

    public String resolveRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}

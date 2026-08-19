package com.gccloud.gcpaas.dataroom.core.operationlog.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 操作日志策略：仅负责长度截断，不再做敏感字段脱敏。
 *
 * 说明：当前只记录 URL 查询参数（? 之后的部分），URL 参数非敏感信息，原样保留即可。
 * 请求体/响应体不记录，因此移除了原先的 BeanWrapper 反射脱敏逻辑，也从根本上避免了
 * 对 HttpServletResponse 等框架对象做反射时其 getter 自引用（如 getResponse()）导致的
 * StackOverflowError。
 */
@Slf4j
public class OperationLogPolicy {

    private static final int MAX_TEXT_LENGTH = 4000;

    /**
     * 记录 URL 查询参数（? 之后的部分），原样保留，仅做长度截断防止超长撑爆表字段。
     */
    public String sanitizeQueryString(String queryString) {
        if (StringUtils.isBlank(queryString)) {
            return null;
        }
        return truncate(queryString, MAX_TEXT_LENGTH);
    }

    public String truncateStack(String stack) {
        return truncate(stack, MAX_TEXT_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}

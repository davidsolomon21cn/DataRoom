package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogPolicyTest {

    private final OperationLogPolicy policy = new OperationLogPolicy();

    /**
     * URL 参数非敏感信息，应原样保留，绝不做脱敏替换（如 password 掩码）。
     */
    @Test
    void sanitizeQueryStringReturnsRawWhenNotSensitive() {
        String query = "page=1&size=10&keyword=admin&password=secret&token=abc";
        String result = policy.sanitizeQueryString(query);
        assertEquals(query, result, "URL 参数应原样记录，不做敏感字段替换");
    }

    /**
     * 空 / 空白 / null 一律存 null。
     */
    @Test
    void sanitizeQueryStringBlankReturnsNull() {
        assertNull(policy.sanitizeQueryString(null));
        assertNull(policy.sanitizeQueryString(""));
        assertNull(policy.sanitizeQueryString("   "));
    }

    /**
     * 超长 query 仍需截断，避免撑爆表字段。
     */
    @Test
    void sanitizeQueryStringTruncatesWhenTooLong() {
        String longQuery = "k=" + "a".repeat(5000);
        String result = policy.sanitizeQueryString(longQuery);
        assertTrue(result != null && result.length() <= 4003, "超长 query 应被截断");
        assertTrue(result.endsWith("..."), "截断后应带省略号");
    }

    @Test
    void truncateStackTruncatesWhenTooLong() {
        String longStack = "x".repeat(5000);
        String result = policy.truncateStack(longStack);
        assertTrue(result != null && result.length() <= 4003);
        assertTrue(result.endsWith("..."));
    }
}

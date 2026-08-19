package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogPathMatcherTest {

    @Test
    void springDoubleWildcardMatchesPathAndNestedPaths() {
        OperationLogPathMatcher matcher = new OperationLogPathMatcher(List.of("/a/**"));

        assertTrue(matcher.isExcluded(request("/a", "")));
        assertTrue(matcher.isExcluded(request("/a/", "")));
        assertTrue(matcher.isExcluded(request("/a/b", "")));
        assertTrue(matcher.isExcluded(request("/a/b/c", "")));
        assertFalse(matcher.isExcluded(request("/ab/c", "")));
    }

    @Test
    void removesContextPathAndIgnoresQueryString() {
        OperationLogPathMatcher matcher = new OperationLogPathMatcher(List.of("/a/**"));
        MockHttpServletRequest request = request("/runtime/a/b", "/runtime");
        request.setQueryString("page=1");

        assertTrue(matcher.isExcluded(request));
    }

    @Test
    void emptyBlankAndDuplicatePatternsDoNotExcludeUnrelatedRequests() {
        OperationLogPathMatcher matcher = new OperationLogPathMatcher(List.of("", "  ", "/a/**", "/a/**"));

        assertFalse(matcher.isExcluded(request("/b", "")));
        assertFalse(new OperationLogPathMatcher(List.of()).isExcluded(request("/a/b", "")));
        assertFalse(matcher.isExcluded(null));
    }

    @Test
    void rejectsInvalidSpringPathPattern() {
        assertThrows(PatternParseException.class,
                () -> new OperationLogPathMatcher(List.of("/a/**/b")));
    }

    private static MockHttpServletRequest request(String requestUri, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setContextPath(contextPath);
        return request;
    }
}

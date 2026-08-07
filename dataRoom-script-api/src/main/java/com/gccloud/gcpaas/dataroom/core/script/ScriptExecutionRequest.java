package com.gccloud.gcpaas.dataroom.core.script;

import java.util.Map;

/**
 * Language-neutral script execution request.
 *
 * @param script script source code
 * @param bindings variables exposed to the script
 */
public record ScriptExecutionRequest(String script, Map<String, Object> bindings) {
}

package com.gccloud.gcpaas.dataroom.core.script;

/**
 * Executes a script using an implementation supplied outside the core application.
 */
@FunctionalInterface
public interface ScriptExecutor {

    Object execute(ScriptExecutionRequest request);
}

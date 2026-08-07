package com.gccloud.gcpaas.dataroom.core.script;

import com.gccloud.gcpaas.dataroom.core.exception.DataRoomException;

/**
 * Default executor used when no external script implementation is installed.
 */
public class DisabledScriptExecutor implements ScriptExecutor {

    @Override
    public Object execute(ScriptExecutionRequest request) {
        throw new DataRoomException("为了安全，默认关闭脚本执行权限，请自行引入脚本执行实现");
    }
}

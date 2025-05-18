package ai.kastrax.core.agent

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * 使用kastrax协程运行时构建Agent
 *
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 * @return 构建的Agent
 */
fun AgentBuilder.buildWithKastraxRuntime(
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
): Agent {
    return KastraxRuntimeAgent(
        name = name,
        instructions = instructions,
        model = model,
        tools = tools,
        memory = memory,
        defaultGenerateOptions = defaultGenerateOptions,
        defaultStreamOptions = defaultStreamOptions,
        toolsets = toolsets,
        sessionManager = sessionManager,
        stateManager = stateManager,
        versionManager = versionManager,
        runtime = runtime
    )
}

package ai.kastrax.actor.multimodal

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.MultimodalRequest
import ai.kastrax.actor.MultimodalResponse
import ai.kastrax.actor.MultimodalStreamChunk
import ai.kastrax.actor.MultimodalStreamRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 发送多模态消息
 *
 * @param target 目标 PID
 * @param message 多模态消息
 */
fun ActorSystem.sendMultiModalMessage(target: PID, message: MultimodalMessage) {
    root.send(target, MultimodalRequest(message))
}

/**
 * 请求-响应模式，向 Agent 发送多模态请求并等待响应
 *
 * @param target 目标 PID
 * @param message 多模态消息
 * @return 多模态响应
 */
suspend fun ActorSystem.askMultiModalMessage(target: PID, message: MultimodalMessage): MultimodalResponse {
    return root.requestAwait(target, MultimodalRequest(message))
}

/**
 * 流式请求-响应模式，向 Agent 发送多模态流式请求并返回响应流
 *
 * @param target 目标 PID
 * @param message 多模态消息
 * @return 多模态响应流
 */
fun ActorSystem.streamMultiModalMessage(target: PID, message: MultimodalMessage): Flow<MultimodalMessage> = flow {
    // 流式实现将在后续版本中完成
    emit(message)
}

// 流式处理相关的代码将在后续版本中实现

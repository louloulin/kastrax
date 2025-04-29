package ai.kastrax.actor.multimodal

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.MultimodalRequest
import ai.kastrax.actor.MultimodalResponse
import ai.kastrax.core.agent.AgentGenerateOptions
import java.io.File
import java.net.URL

/**
 * 发送文本多模态消息
 *
 * @param target 目标 Agent 的 PID
 * @param text 文本内容
 * @param options 生成选项
 */
fun ActorSystem.sendTextMessage(
    target: PID,
    text: String,
    options: AgentGenerateOptions = AgentGenerateOptions()
) {
    val message = MultimodalMessage(text, MultimodalType.TEXT)
    root.send(target, MultimodalRequest(message, options))
}

/**
 * 发送图像多模态消息
 *
 * @param target 目标 Agent 的 PID
 * @param image 图像内容
 * @param options 生成选项
 */
fun ActorSystem.sendImageMessage(
    target: PID,
    image: MultimodalContent.Image,
    options: AgentGenerateOptions = AgentGenerateOptions()
) {
    val message = MultimodalMessage(image, MultimodalType.IMAGE)
    root.send(target, MultimodalRequest(message, options))
}

/**
 * 发送音频多模态消息
 *
 * @param target 目标 Agent 的 PID
 * @param audio 音频内容
 * @param options 生成选项
 */
fun ActorSystem.sendAudioMessage(
    target: PID,
    audio: MultimodalContent.Audio,
    options: AgentGenerateOptions = AgentGenerateOptions()
) {
    val message = MultimodalMessage(audio, MultimodalType.AUDIO)
    root.send(target, MultimodalRequest(message, options))
}

/**
 * 发送视频多模态消息
 *
 * @param target 目标 Agent 的 PID
 * @param video 视频内容
 * @param options 生成选项
 */
fun ActorSystem.sendVideoMessage(
    target: PID,
    video: MultimodalContent.Video,
    options: AgentGenerateOptions = AgentGenerateOptions()
) {
    val message = MultimodalMessage(video, MultimodalType.VIDEO)
    root.send(target, MultimodalRequest(message, options))
}

/**
 * 发送文件多模态消息
 *
 * @param target 目标 Agent 的 PID
 * @param file 文件内容
 * @param options 生成选项
 */
fun ActorSystem.sendFileMessage(
    target: PID,
    file: MultimodalContent.FileContent,
    options: AgentGenerateOptions = AgentGenerateOptions()
) {
    val message = MultimodalMessage(file, MultimodalType.FILE)
    root.send(target, MultimodalRequest(message, options))
}

/**
 * 发送混合多模态消息
 *
 * @param target 目标 Agent 的 PID
 * @param mixed 混合内容
 * @param options 生成选项
 */
fun ActorSystem.sendMixedMessage(
    target: PID,
    mixed: MultimodalContent.Mixed,
    options: AgentGenerateOptions = AgentGenerateOptions()
) {
    val message = MultimodalMessage(mixed, MultimodalType.MIXED)
    root.send(target, MultimodalRequest(message, options))
}

/**
 * 请求-响应模式，向 Agent 发送多模态请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param content 多模态内容
 * @param type 多模态类型
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askMultimodalMessage(
    target: PID,
    content: Any,
    type: MultimodalType,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    val message = MultimodalMessage(content, type)
    return root.requestAwait(target, MultimodalRequest(message, options))
}

/**
 * 请求-响应模式，向 Agent 发送文本请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param text 文本内容
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askTextMessage(
    target: PID,
    text: String,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    return askMultimodalMessage(target, text, MultimodalType.TEXT, options)
}

/**
 * 请求-响应模式，向 Agent 发送图像请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param image 图像内容
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askImageMessage(
    target: PID,
    image: MultimodalContent.Image,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    return askMultimodalMessage(target, image, MultimodalType.IMAGE, options)
}

/**
 * 请求-响应模式，向 Agent 发送音频请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param audio 音频内容
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askAudioMessage(
    target: PID,
    audio: MultimodalContent.Audio,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    return askMultimodalMessage(target, audio, MultimodalType.AUDIO, options)
}

/**
 * 请求-响应模式，向 Agent 发送视频请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param video 视频内容
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askVideoMessage(
    target: PID,
    video: MultimodalContent.Video,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    return askMultimodalMessage(target, video, MultimodalType.VIDEO, options)
}

/**
 * 请求-响应模式，向 Agent 发送文件请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param file 文件内容
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askFileMessage(
    target: PID,
    file: MultimodalContent.FileContent,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    return askMultimodalMessage(target, file, MultimodalType.FILE, options)
}

/**
 * 请求-响应模式，向 Agent 发送混合请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param mixed 混合内容
 * @param options 生成选项
 * @return 多模态响应
 */
suspend fun ActorSystem.askMixedMessage(
    target: PID,
    mixed: MultimodalContent.Mixed,
    options: AgentGenerateOptions = AgentGenerateOptions()
): MultimodalResponse {
    return askMultimodalMessage(target, mixed, MultimodalType.MIXED, options)
}

/**
 * 从文本内容创建多模态消息
 *
 * @param text 文本内容
 * @return 多模态消息
 */
fun text(text: String): MultimodalMessage {
    return MultimodalMessage(text, MultimodalType.TEXT)
}

/**
 * 从图像文件创建多模态消息
 *
 * @param filePath 文件路径
 * @return 多模态消息
 */
fun imageFromFile(filePath: String): MultimodalMessage {
    val image = MultimodalProcessor.createImageFromFile(filePath)
    return MultimodalMessage(image, MultimodalType.IMAGE)
}

/**
 * 从图像 URL 创建多模态消息
 *
 * @param url 图像 URL
 * @return 多模态消息
 */
fun imageFromUrl(url: String): MultimodalMessage {
    val image = MultimodalProcessor.createImageFromUrl(url)
    return MultimodalMessage(image, MultimodalType.IMAGE)
}

/**
 * 从音频文件创建多模态消息
 *
 * @param filePath 文件路径
 * @return 多模态消息
 */
fun audioFromFile(filePath: String): MultimodalMessage {
    val audio = MultimodalProcessor.createAudioFromFile(filePath)
    return MultimodalMessage(audio, MultimodalType.AUDIO)
}

/**
 * 从音频 URL 创建多模态消息
 *
 * @param url 音频 URL
 * @return 多模态消息
 */
fun audioFromUrl(url: String): MultimodalMessage {
    val audio = MultimodalProcessor.createAudioFromUrl(url)
    return MultimodalMessage(audio, MultimodalType.AUDIO)
}

/**
 * 从视频文件创建多模态消息
 *
 * @param filePath 文件路径
 * @return 多模态消息
 */
fun videoFromFile(filePath: String): MultimodalMessage {
    val video = MultimodalProcessor.createVideoFromFile(filePath)
    return MultimodalMessage(video, MultimodalType.VIDEO)
}

/**
 * 从视频 URL 创建多模态消息
 *
 * @param url 视频 URL
 * @return 多模态消息
 */
fun videoFromUrl(url: String): MultimodalMessage {
    val video = MultimodalProcessor.createVideoFromUrl(url)
    return MultimodalMessage(video, MultimodalType.VIDEO)
}

/**
 * 从文件创建多模态消息
 *
 * @param filePath 文件路径
 * @return 多模态消息
 */
fun fileFromPath(filePath: String): MultimodalMessage {
    val file = MultimodalProcessor.createFileContent(filePath)
    return MultimodalMessage(file, MultimodalType.FILE)
}

/**
 * 创建混合多模态消息
 *
 * @param contents 内容列表
 * @return 多模态消息
 */
fun mixed(vararg contents: MultimodalContent): MultimodalMessage {
    val mixed = MultimodalProcessor.createMixedContent(*contents)
    return MultimodalMessage(mixed, MultimodalType.MIXED)
}

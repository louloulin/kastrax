# Kastrax-Codex (ProxyAI) 架构文档

## 1. 概述

Kastrax-Codex（原名 ProxyAI/CodeGPT）是一个功能丰富的 JetBrains IDE 插件，为开发者提供 AI 驱动的代码辅助功能。本文档详细分析了 Kastrax-Codex 的现有架构、核心功能和实现细节，为未来的改进和集成提供基础。

## 2. 系统架构

### 2.1 整体架构

Kastrax-Codex 采用模块化架构，主要包括以下核心组件：

1. **UI 层**：负责用户界面和交互，包括工具窗口、编辑器集成和设置面板
2. **业务逻辑层**：处理核心功能，如代码补全、聊天和 Git 集成
3. **LLM 集成层**：连接各种 LLM 提供商，处理 API 请求和响应
4. **数据存储层**：管理对话历史、设置和上下文信息

```
+---------------------+
|      UI 层          |
| (工具窗口、编辑器)   |
+----------+----------+
           |
+----------v----------+
|    业务逻辑层        |
| (代码补全、聊天等)   |
+----------+----------+
           |
+----------v----------+
|    LLM 集成层       |
| (OpenAI、Anthropic等)|
+----------+----------+
           |
+----------v----------+
|    数据存储层        |
| (对话、设置等)       |
+---------------------+
```

### 2.2 核心组件

#### 2.2.1 插件入口

`CodeGPTPlugin` 类是插件的主入口点，负责初始化和管理插件生命周期：

```java
public final class CodeGPTPlugin {
  public static final PluginId CODEGPT_ID = PluginId.getId("ee.carlrobert.chatgpt");
  
  // 获取插件版本
  public static @NotNull String getVersion() {
    return requireNonNull(PluginManagerCore.getPlugin(CODEGPT_ID)).getVersion();
  }
  
  // 获取插件基础路径
  public static @NotNull Path getPluginBasePath() {
    return requireNonNull(PluginManagerCore.getPlugin(CODEGPT_ID)).getPluginPath();
  }
  
  // 获取插件选项路径
  public static @NotNull String getPluginOptionsPath() {
    return PathManager.getOptionsPath() + separator + "CodeGPT";
  }
}
```

#### 2.2.2 LLM 提供商集成

`CompletionClientProvider` 类负责创建和管理与不同 LLM 提供商的连接：

```java
public class CompletionClientProvider {
  // 获取 CodeGPT 客户端
  public static CodeGPTClient getCodeGPTClient() {
    return new CodeGPTClient(
        getCredential(CredentialKey.CodeGptApiKey.INSTANCE),
        getDefaultClientBuilder());
  }
  
  // 获取 OpenAI 客户端
  public static OpenAIClient getOpenAIClient() {
    return new OpenAIClient.Builder(getCredential(CredentialKey.OpenaiApiKey.INSTANCE))
        .setOrganization(OpenAISettings.getCurrentState().getOrganization())
        .build(getDefaultClientBuilder());
  }
  
  // 获取 Claude 客户端
  public static ClaudeClient getClaudeClient() {
    return new ClaudeClient.Builder(getCredential(CredentialKey.AnthropicApiKey.INSTANCE))
        .setApiVersion(AnthropicSettings.getCurrentState().getApiVersion())
        .build(getDefaultClientBuilder());
  }
  
  // 其他 LLM 提供商...
}
```

支持的 LLM 提供商包括：
- OpenAI
- Anthropic (Claude)
- Azure OpenAI
- Google (Gemini)
- Llama.cpp (本地)
- Ollama (本地)
- 自定义 OpenAI 兼容 API

#### 2.2.3 完成请求服务

`CompletionRequestService` 类是核心服务，负责处理各种 AI 完成请求：

```java
@Service
public final class CompletionRequestService {
  // 获取聊天完成
  public String getChatCompletion(CompletionRequest request) {
    // 根据不同的请求类型和服务类型处理
    if (request instanceof OpenAIChatCompletionRequest completionRequest) {
      var response = switch (GeneralSettings.getSelectedService()) {
        case OPENAI -> CompletionClientProvider.getOpenAIClient()
            .getChatCompletion(completionRequest);
        case AZURE -> CompletionClientProvider.getAzureClient()
            .getChatCompletion(completionRequest);
        // 其他服务...
      };
      return tryExtractContent(response).orElseThrow();
    }
    // 其他请求类型...
  }
  
  // 异步获取聊天完成（流式）
  public EventSource getChatCompletionAsync(
      CompletionRequest request,
      CompletionEventListener<String> eventListener) {
    // 类似的模式，但返回 EventSource 用于流式处理
  }
  
  // 特定功能的请求方法
  public EventSource getCommitMessageAsync(
      CommitMessageCompletionParameters params,
      CompletionEventListener<String> eventListener) {
    // 创建提交消息请求并处理
  }
  
  public EventSource getEditCodeCompletionAsync(
      EditCodeCompletionParameters params,
      CompletionEventListener<String> eventListener) {
    // 创建编辑代码请求并处理
  }
}
```

#### 2.2.4 对话管理

`ConversationService` 类管理用户与 AI 的对话：

```java
@Service
public class ConversationService {
  // 创建新对话
  public Conversation createConversation(String clientCode) {
    var conversation = new Conversation();
    conversation.setId(UUID.randomUUID());
    conversation.setClientCode(clientCode);
    conversation.setCreatedOn(LocalDateTime.now());
    conversation.setUpdatedOn(LocalDateTime.now());
    conversation.setModel(getModelForSelectedService(GeneralSettings.getSelectedService()));
    return conversation;
  }
  
  // 保存消息
  public void saveMessage(String response, ChatCompletionParameters callParameters) {
    var conversation = callParameters.getConversation();
    var message = callParameters.getMessage();
    // 保存消息逻辑...
  }
  
  // 开始新对话
  public Conversation startConversation() {
    var completionCode = GeneralSettings.getSelectedService().getCompletionCode();
    var conversation = createConversation(completionCode);
    conversationState.setCurrentConversation(conversation);
    addConversation(conversation);
    return conversation;
  }
}
```

#### 2.2.5 设置管理

系统使用多个设置类管理不同方面的配置：

```java
@State(name = "CodeGPT_GeneralSettings_270", storages = @Storage("CodeGPT_GeneralSettings_270.xml"))
public class GeneralSettings implements PersistentStateComponent<GeneralSettingsState> {
  // 获取当前状态
  public static GeneralSettingsState getCurrentState() {
    return getInstance().getState();
  }
  
  // 获取选择的服务
  public static ServiceType getSelectedService() {
    return getCurrentState().getSelectedService();
  }
}

// 特定服务的设置
@State(name = "CodeGPT_OpenAISettings_210", storages = @Storage("CodeGPT_OpenAISettings_210.xml"))
public class OpenAISettings implements PersistentStateComponent<OpenAISettingsState> {
  // 实现...
}

@State(name = "CodeGPT_AnthropicSettings", storages = @Storage("CodeGPT_AnthropicSettings.xml"))
public class AnthropicSettings implements PersistentStateComponent<AnthropicSettingsState> {
  // 实现...
}
```

## 3. 核心功能

### 3.1 聊天功能

聊天功能允许用户与 AI 进行交互，获取编码建议和解释：

#### 3.1.1 聊天工具窗口

`ChatToolWindowTabPanel` 类实现了聊天界面：

```java
public class ChatToolWindowTabPanel implements Disposable {
  private final ChatSession chatSession;
  private final Project project;
  private final JPanel rootPanel;
  private final Conversation conversation;
  private final UserInputPanel userInputPanel;
  private final ConversationService conversationService;
  private final TotalTokensPanel totalTokensPanel;
  private final ChatToolWindowScrollablePanel toolWindowScrollablePanel;
  
  // 处理用户提交
  private void handleSubmit(String text, List<EditorTagDetails> tags) {
    // 处理用户输入并发送到 AI
  }
  
  // 显示对话
  private void displayConversation() {
    // 显示现有对话
  }
  
  // 显示登陆视图
  private void displayLandingView() {
    // 显示初始界面
  }
}
```

#### 3.1.2 消息处理

`ChatMessageResponseBody` 类处理 AI 响应的显示：

```java
public class ChatMessageResponseBody extends JPanel {
  // 处理流式响应
  public void handleStreamResponse(String text) {
    // 处理流式文本并更新 UI
  }
  
  // 处理完整响应
  public void handleCompleteResponse(String text) {
    // 处理完整响应并更新 UI
  }
}
```

### 3.2 代码补全功能

代码补全功能提供实时的代码建议：

#### 3.2.1 内联补全

`DebouncedCodeCompletionProvider` 类实现了内联代码补全：

```java
public class DebouncedCodeCompletionProvider implements InlineCompletionProvider {
  // 提供内联补全
  @Override
  public @Nullable InlineCompletionElement provideInlineCompletion(
      @NotNull InlineCompletionContext context) {
    // 实现代码补全逻辑
  }
}
```

#### 3.2.2 代码编辑

`EditCodeContextMenuAction` 类实现了代码编辑功能：

```java
public class EditCodeContextMenuAction extends AnAction {
  // 执行动作
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    // 实现代码编辑逻辑
  }
}
```

### 3.3 Git 集成

Git 集成功能帮助生成提交消息和审查代码变更：

#### 3.3.1 提交消息生成

`GenerateCommitMessageAction` 类实现了提交消息生成：

```java
public class GenerateCommitMessageAction extends AnAction {
  // 执行动作
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    // 实现提交消息生成逻辑
  }
}
```

### 3.4 本地 LLM 集成

本地 LLM 集成允许离线使用 AI 功能：

#### 3.4.1 Llama.cpp 集成

`LlamaClient` 类实现了与 Llama.cpp 的集成：

```java
public class LlamaClient {
  // 获取聊天完成
  public LlamaCompletionResponse getChatCompletion(LlamaCompletionRequest request) {
    // 实现 Llama.cpp 调用逻辑
  }
}
```

#### 3.4.2 Ollama 集成

`OllamaClient` 类实现了与 Ollama 的集成：

```java
public class OllamaClient {
  // 获取聊天完成
  public OllamaCompletionResponse getChatCompletion(OllamaCompletionRequest request) {
    // 实现 Ollama 调用逻辑
  }
}
```

## 4. 用户界面

### 4.1 工具窗口

工具窗口是用户与 AI 交互的主要界面：

#### 4.1.1 聊天工具窗口

`ProjectToolWindowFactory` 类创建聊天工具窗口：

```java
public class ProjectToolWindowFactory implements ToolWindowFactory {
  // 创建工具窗口内容
  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    // 实现工具窗口创建逻辑
  }
}
```

#### 4.1.2 用户输入面板

`UserInputPanel` 类实现了用户输入界面：

```java
public class UserInputPanel extends JPanel {
  // 处理用户输入
  private void handleSubmit() {
    // 处理用户提交逻辑
  }
}
```

### 4.2 设置界面

设置界面允许用户配置插件：

#### 4.2.1 通用设置

`GeneralSettingsConfigurable` 类实现了通用设置界面：

```java
public class GeneralSettingsConfigurable implements Configurable {
  // 创建组件
  @Override
  public @Nullable JComponent createComponent() {
    // 创建设置界面组件
  }
}
```

#### 4.2.2 服务设置

`ServiceConfigurable` 类实现了服务设置界面：

```java
public class ServiceConfigurable implements Configurable {
  // 创建组件
  @Override
  public @Nullable JComponent createComponent() {
    // 创建服务设置界面组件
  }
}
```

## 5. 数据模型

### 5.1 对话模型

`Conversation` 类表示用户与 AI 的对话：

```java
public class Conversation {
  private UUID id;
  private List<Message> messages = new ArrayList<>();
  private String clientCode;
  private String model;
  private LocalDateTime createdOn;
  private LocalDateTime updatedOn;
  private boolean discardTokenLimit;
  
  // 添加消息
  public void addMessage(Message message) {
    messages.add(message);
  }
}
```

### 5.2 消息模型

`Message` 类表示对话中的单条消息：

```java
public class Message {
  private final UUID id;
  private String prompt;
  private String response;
  private List<String> referencedFilePaths;
  private String imageFilePath;
  private boolean webSearchIncluded;
  private DocumentationDetails documentationDetails;
  private String personaName;
  
  // 构造函数和访问器
}
```

### 5.3 设置模型

`GeneralSettingsState` 类存储通用设置：

```java
public class GeneralSettingsState {
  private ServiceType selectedService = ServiceType.OPENAI;
  private boolean checkForPluginUpdates = true;
  private boolean checkForNewScreenshots = true;
  private boolean methodNameGenerationEnabled = true;
  private boolean autoFormattingEnabled = true;
  private CodeCompletionSettings codeCompletionSettings = new CodeCompletionSettings();
  private ChatCompletionSettings chatCompletionSettings = new ChatCompletionSettings();
  
  // 访问器和修改器
}
```

## 6. 扩展点

### 6.1 服务类型

`ServiceType` 枚举定义了支持的 LLM 服务：

```java
public enum ServiceType {
  CODEGPT("CODEGPT", "service.codegpt.title", "codegpt.chat.completion"),
  OPENAI("OPENAI", "service.openai.title", "chat.completion"),
  CUSTOM_OPENAI("CUSTOM_OPENAI", "service.custom.openai.title", "custom.openai.chat.completion"),
  ANTHROPIC("ANTHROPIC", "service.anthropic.title", "anthropic.chat.completion"),
  AZURE("AZURE", "service.azure.title", "azure.chat.completion"),
  GOOGLE("GOOGLE", "service.google.title", "google.chat.completion"),
  LLAMA_CPP("LLAMA_CPP", "service.llama.title", "llama.chat.completion"),
  OLLAMA("OLLAMA", "service.ollama.title", "ollama.chat.completion");
  
  // 实现细节
}
```

### 6.2 动作类型

`ActionType` 枚举定义了支持的动作：

```java
public enum ActionType {
  CLEAR_CHAT_WINDOW,
  CREATE_NEW_CHAT,
  DELETE_ALL_CONVERSATIONS,
  DELETE_CONVERSATION,
  DISCARD_TOKEN_LIMIT,
  OPEN_CONVERSATION_IN_EDITOR,
  DIFF_CODE,
  EDIT_CODE,
  CREATE_NEW_FILE,
  COPY_CODE,
  AUTO_APPLY,
  REPLACE_IN_MAIN_EDITOR,
  INSERT_AT_CARET,
  RELOAD_MESSAGE,
  CHANGE_PROVIDER
}
```

## 7. 技术栈

### 7.1 核心技术

- **Java/Kotlin**：主要编程语言
- **IntelliJ Platform SDK**：IDE 集成
- **OkHttp**：HTTP 客户端
- **Kotlinx.serialization**：JSON 序列化
- **Swing**：UI 组件

### 7.2 外部依赖

- **LLM API**：OpenAI、Anthropic、Azure、Google 等
- **Llama.cpp**：本地 LLM 运行时
- **Ollama**：本地 LLM 管理

## 8. 架构评估

### 8.1 优势

1. **模块化设计**：系统采用模块化设计，各组件职责明确
2. **多 LLM 提供商支持**：支持多种 LLM 提供商，包括云服务和本地模型
3. **丰富的功能集**：提供代码补全、聊天、Git 集成等多种功能
4. **可扩展性**：设计允许添加新的 LLM 提供商和功能

### 8.2 局限性

1. **传统客户端-服务器模型**：直接与 LLM API 交互，没有利用现代 AI agent 架构
2. **有限的上下文管理**：缺乏高级记忆系统和上下文管理
3. **工具集成受限**：无法让 AI 使用复杂的工具和 API
4. **缺乏自适应能力**：无法根据用户反馈自动调整行为

### 8.3 改进机会

1. **集成 Kastrax Agent 框架**：利用 Kastrax 的 Agent 架构增强功能
2. **增强记忆系统**：实现高级记忆系统，提升上下文理解
3. **工具系统集成**：允许 AI 使用 IDE 特定工具
4. **RAG 系统集成**：实现检索增强生成，提高代码相关知识检索
5. **分布式处理**：利用 Actor 模型实现并行和分布式处理

## 9. 结论

Kastrax-Codex（ProxyAI）是一个功能丰富的 JetBrains IDE 插件，提供多种 AI 驱动的代码辅助功能。其模块化架构和多 LLM 提供商支持为用户提供了灵活的选择。然而，当前架构基于传统的客户端-服务器模型，没有利用现代 AI agent 架构的优势。

通过集成 Kastrax AI agent 框架，Kastrax-Codex 可以显著提升其能力，包括更好的上下文理解、工具使用和自适应学习。这将使插件能够提供更智能、更个性化的代码辅助体验，同时保持与 JetBrains IDE 的深度集成。

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use thiserror::Error;

// 全局状态管理
lazy_static::lazy_static! {
    static ref AGENTS: Mutex<HashMap<i64, Agent>> = Mutex::new(HashMap::new());
    static ref NEXT_AGENT_ID: Mutex<i64> = Mutex::new(1);
    static ref CONFIG: Mutex<Option<Config>> = Mutex::new(None);
}

// 错误类型
#[derive(Error, Debug)]
pub enum Error {
    #[error("JNI错误: {0}")]
    Jni(#[from] jni::errors::Error),
    
    #[error("JSON错误: {0}")]
    Json(#[from] serde_json::Error),
    
    #[error("初始化错误: {0}")]
    Init(String),
    
    #[error("Agent错误: {0}")]
    Agent(String),
    
    #[error("工具错误: {0}")]
    Tool(String),
}

// 配置
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    pub api_key: String,
    pub log_level: String,
}

impl Config {
    pub fn new() -> Self {
        Self {
            api_key: String::new(),
            log_level: "info".to_string(),
        }
    }
    
    pub fn with_api_key(mut self, api_key: &str) -> Self {
        self.api_key = api_key.to_string();
        self
    }
    
    pub fn with_log_level(mut self, log_level: &str) -> Self {
        self.log_level = log_level.to_string();
        self
    }
}

// Agent配置
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AgentConfig {
    pub name: String,
    pub model: String,
    pub temperature: Option<f64>,
    pub max_tokens: Option<i32>,
}

// Agent状态
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AgentState {
    pub id: i64,
    pub name: String,
    pub model: String,
    pub tool_count: usize,
}

// Agent实现
#[derive(Debug)]
pub struct Agent {
    id: i64,
    name: String,
    model: String,
    temperature: f64,
    max_tokens: i32,
    tools: Vec<Tool>,
}

impl Agent {
    pub fn builder() -> AgentBuilder {
        AgentBuilder::new()
    }
    
    pub fn run(&self, input: &str) -> Result<String, Error> {
        // 在实际实现中，这里会调用JNI方法来运行Agent
        // 现在我们只返回一个模拟响应
        let tool_names = self.tools.iter()
            .map(|t| t.name.clone())
            .collect::<Vec<_>>()
            .join(", ");
            
        Ok(format!(
            "Agent '{}' (ID: {}) using model '{}' with tools [{}] response to: {}",
            self.name, self.id, self.model, tool_names, input
        ))
    }
    
    pub fn add_tool(&mut self, tool: Tool) -> Result<(), Error> {
        self.tools.push(tool);
        Ok(())
    }
    
    pub fn get_state(&self) -> AgentState {
        AgentState {
            id: self.id,
            name: self.name.clone(),
            model: self.model.clone(),
            tool_count: self.tools.len(),
        }
    }
}

// Agent构建器
pub struct AgentBuilder {
    name: Option<String>,
    model: Option<String>,
    temperature: f64,
    max_tokens: i32,
}

impl AgentBuilder {
    fn new() -> Self {
        Self {
            name: None,
            model: None,
            temperature: 0.7,
            max_tokens: 1000,
        }
    }
    
    pub fn name(mut self, name: &str) -> Self {
        self.name = Some(name.to_string());
        self
    }
    
    pub fn model(mut self, model: &str) -> Self {
        self.model = Some(model.to_string());
        self
    }
    
    pub fn temperature(mut self, temperature: f64) -> Self {
        self.temperature = temperature;
        self
    }
    
    pub fn max_tokens(mut self, max_tokens: i32) -> Self {
        self.max_tokens = max_tokens;
        self
    }
    
    pub fn build(self) -> Result<Agent, Error> {
        let name = self.name.ok_or_else(|| Error::Agent("Agent name is required".to_string()))?;
        let model = self.model.ok_or_else(|| Error::Agent("Model name is required".to_string()))?;
        
        let mut next_id = NEXT_AGENT_ID.lock().unwrap();
        let id = *next_id;
        *next_id += 1;
        
        let agent = Agent {
            id,
            name,
            model,
            temperature: self.temperature,
            max_tokens: self.max_tokens,
            tools: Vec::new(),
        };
        
        AGENTS.lock().unwrap().insert(id, agent.clone());
        
        Ok(agent)
    }
}

impl Clone for Agent {
    fn clone(&self) -> Self {
        Self {
            id: self.id,
            name: self.name.clone(),
            model: self.model.clone(),
            temperature: self.temperature,
            max_tokens: self.max_tokens,
            tools: self.tools.clone(),
        }
    }
}

// 工具类型
#[derive(Debug, Clone)]
pub struct Tool {
    name: String,
    description: Option<String>,
    function: Arc<dyn Fn(&str) -> Result<String, String> + Send + Sync>,
}

impl Tool {
    pub fn new<F>(name: &str, function: F) -> Self
    where
        F: Fn(&str) -> Result<String, String> + Send + Sync + 'static,
    {
        Self {
            name: name.to_string(),
            description: None,
            function: Arc::new(function),
        }
    }
    
    pub fn with_description(mut self, description: &str) -> Self {
        self.description = Some(description.to_string());
        self
    }
    
    pub fn execute(&self, input: &str) -> Result<String, String> {
        (self.function)(input)
    }
}

// 公共API

/// 初始化SDK
pub fn init(config: Config) -> Result<(), Error> {
    // 设置日志级别
    std::env::set_var("RUST_LOG", &config.log_level);
    env_logger::init();
    
    // 存储配置
    *CONFIG.lock().unwrap() = Some(config);
    
    Ok(())
}

/// 关闭SDK并释放资源
pub fn shutdown() {
    AGENTS.lock().unwrap().clear();
    *CONFIG.lock().unwrap() = None;
}

// JNI导出函数

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_graal_bridge_RustBridge_initialize(
    env: JNIEnv,
    _class: JClass,
    config_json: JString,
) -> jint {
    let result = || -> Result<(), Error> {
        let config_str: String = env.get_string(config_json)?.into();
        let config: Config = serde_json::from_str(&config_str)?;
        init(config)?;
        Ok(())
    }();
    
    match result {
        Ok(_) => 0,
        Err(e) => {
            log::error!("初始化错误: {:?}", e);
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_graal_bridge_RustBridge_createAgent(
    env: JNIEnv,
    _class: JClass,
    agent_json: JString,
) -> jlong {
    let result = || -> Result<jlong, Error> {
        let agent_str: String = env.get_string(agent_json)?.into();
        let agent_config: AgentConfig = serde_json::from_str(&agent_str)?;
        
        let agent = Agent::builder()
            .name(&agent_config.name)
            .model(&agent_config.model);
            
        let agent = if let Some(temp) = agent_config.temperature {
            agent.temperature(temp)
        } else {
            agent
        };
        
        let agent = if let Some(tokens) = agent_config.max_tokens {
            agent.max_tokens(tokens)
        } else {
            agent
        };
        
        let agent = agent.build()?;
        Ok(agent.id as jlong)
    }();
    
    match result {
        Ok(id) => id,
        Err(e) => {
            log::error!("创建Agent错误: {:?}", e);
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_graal_bridge_RustBridge_runAgent(
    env: JNIEnv,
    _class: JClass,
    agent_id: jlong,
    input: JString,
) -> jstring {
    let result = || -> Result<String, Error> {
        let input_str: String = env.get_string(input)?.into();
        let agents = AGENTS.lock().unwrap();
        let agent = agents.get(&(agent_id as i64))
            .ok_or_else(|| Error::Agent(format!("Agent with ID {} not found", agent_id)))?;
        agent.run(&input_str)
    }();
    
    match result {
        Ok(output) => {
            let output_j = env.new_string(output).expect("Couldn't create Java string");
            output_j.into_raw()
        },
        Err(e) => {
            log::error!("运行Agent错误: {:?}", e);
            let error_msg = format!("Error: {:?}", e);
            let error_j = env.new_string(error_msg).expect("Couldn't create Java string");
            error_j.into_raw()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_graal_bridge_RustBridge_addTool(
    env: JNIEnv,
    _class: JClass,
    agent_id: jlong,
    tool_json: JString,
) -> jint {
    let result = || -> Result<(), Error> {
        let tool_str: String = env.get_string(tool_json)?.into();
        let tool_data: serde_json::Value = serde_json::from_str(&tool_str)?;
        
        let name = tool_data["name"].as_str()
            .ok_or_else(|| Error::Tool("Tool name is required".to_string()))?;
            
        let description = tool_data["description"].as_str();
        
        // 创建一个简单的工具，实际实现中可能需要更复杂的逻辑
        let tool = Tool::new(name, |input| {
            Ok(format!("Tool '{}' processed: {}", name, input))
        });
        
        let tool = if let Some(desc) = description {
            tool.with_description(desc)
        } else {
            tool
        };
        
        let mut agents = AGENTS.lock().unwrap();
        let agent = agents.get_mut(&(agent_id as i64))
            .ok_or_else(|| Error::Agent(format!("Agent with ID {} not found", agent_id)))?;
            
        agent.add_tool(tool)?;
        
        Ok(())
    }();
    
    match result {
        Ok(_) => 0,
        Err(e) => {
            log::error!("添加工具错误: {:?}", e);
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_graal_bridge_RustBridge_releaseAgent(
    _env: JNIEnv,
    _class: JClass,
    agent_id: jlong,
) {
    let mut agents = AGENTS.lock().unwrap();
    agents.remove(&(agent_id as i64));
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_graal_bridge_RustBridge_shutdown(
    _env: JNIEnv,
    _class: JClass,
) {
    shutdown();
}

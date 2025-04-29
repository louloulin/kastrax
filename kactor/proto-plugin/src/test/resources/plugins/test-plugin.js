// 测试JavaScript插件

// 接收中间件函数
function receiveMiddleware(context) {
    console.log("JavaScript plugin: Processing message", context.message);
    
    // 修改消息
    if (typeof context.message === 'string') {
        context.message = "js-modified-" + context.message;
    }
    
    // 返回true表示继续处理
    return true;
}

// 发送中间件函数
function senderMiddleware(context, pid, envelope) {
    console.log("JavaScript plugin: Sending message to", pid.id);
    
    // 返回true表示继续处理
    return true;
}

// 导出函数
module.exports = {
    receiveMiddleware: receiveMiddleware,
    senderMiddleware: senderMiddleware
};

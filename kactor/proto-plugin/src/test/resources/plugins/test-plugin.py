# 测试Python插件

# 接收中间件函数
def receive_middleware(context):
    print("Python plugin: Processing message", context.message)
    
    # 修改消息
    if isinstance(context.message, str):
        context.message = "py-modified-" + context.message
    
    # 返回True表示继续处理
    return True

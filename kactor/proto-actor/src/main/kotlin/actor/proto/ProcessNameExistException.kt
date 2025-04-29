package actor.proto

/**
 * 进程名称已存在异常
 * @param name 已存在的进程名称
 */
class ProcessNameExistException(name: String) : RuntimeException("Process name '$name' already exists")

package ai.kastrax.datasource

import ai.kastrax.datasource.common.DataSource
import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.ApiConnector
import ai.kastrax.datasource.common.DatabaseConnector
import ai.kastrax.datasource.common.FileSystemConnector
import ai.kastrax.datasource.api.RestApiConnector
import ai.kastrax.datasource.database.MySqlConnector
import ai.kastrax.datasource.filesystem.LocalFileSystemConnector

// 这个文件现在只是一个包装器，实际的定义已经移动到 kastrax-datasource-common 模块中

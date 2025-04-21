package ai.kastrax.datasource

import ai.kastrax.datasource.common.DataSource
import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.ApiConnectorBase
import ai.kastrax.datasource.common.RestApiConnectorBase
import ai.kastrax.datasource.common.DatabaseConnectorBase
import ai.kastrax.datasource.common.MySqlConnectorBase
import ai.kastrax.datasource.common.FileSystemConnectorBase
import ai.kastrax.datasource.common.LocalFileSystemConnectorBase
import ai.kastrax.datasource.api.ApiConnector
import ai.kastrax.datasource.api.RestApiConnector
import ai.kastrax.datasource.database.DatabaseConnector
import ai.kastrax.datasource.database.MySqlConnector
import ai.kastrax.datasource.filesystem.FileSystemConnector
import ai.kastrax.datasource.filesystem.LocalFileSystemConnector

// 这个文件现在只是一个包装器，实际的定义已经移动到 kastrax-datasource-common 模块中

package com.kastrax.ai2db

import io.micronaut.runtime.Micronaut

/**
 * KastraX AI2DB Micronaut Application
 * 
 * 迁移自Spring Boot，使用Micronaut框架提供更好的性能和插件支持
 */
object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
            .args(*args)
            .packages("com.kastrax.ai2db")
            .start()
    }
}
// 阿里云Maven仓库发布配置

plugins {
    `maven-publish`
    `signing`
}

publishing {
    repositories {
        maven {
            name = "AliyunMaven"
            url = uri(findProperty("aliyunMavenUrl") as String? ?: "")
            credentials {
                username = findProperty("aliyunMavenUsername") as String?
                password = findProperty("aliyunMavenPassword") as String?
            }
        }
        
        maven {
            name = "AliyunPrivate"
            url = uri(findProperty("aliyunPrivateUrl") as String? ?: "")
            credentials {
                username = findProperty("aliyunPrivateUsername") as String?
                password = findProperty("aliyunPrivatePassword") as String?
            }
        }
    }
    
    publications {
        create<MavenPublication>("aliyunMaven") {
            from(components["java"])
            
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            
            pom {
                name.set(project.name)
                description.set(project.description ?: "Kastrax AI Framework Module")
                url.set("https://github.com/louloulin/kastrax")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("louloulin")
                        name.set("louloulin")
                        email.set("729883852@qq.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:https://github.com/louloulin/kastrax.git")
                    developerConnection.set("scm:git:ssh://github.com/louloulin/kastrax.git")
                    url.set("https://github.com/louloulin/kastrax")
                }
            }
        }
    }
}

// 签名配置 (可选)
signing {
    val signingKeyId = findProperty("signing.keyId") as String?
    val signingPassword = findProperty("signing.password") as String?
    val signingSecretKey = findProperty("signing.secretKey") as String?
    
    if (!signingKeyId.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
        if (!signingSecretKey.isNullOrEmpty()) {
            useInMemoryPgpKeys(signingKeyId, signingSecretKey, signingPassword)
        } else {
            useGpgCmd()
        }
        sign(publishing.publications["aliyunMaven"])
    }
}

// 发布任务
tasks.register("publishToAliyunMaven") {
    dependsOn("publishAliyunMavenPublicationToAliyunMavenRepository")
    group = "publishing"
    description = "发布到阿里云Maven仓库"
}

tasks.register("publishToAliyunPrivate") {
    dependsOn("publishAliyunMavenPublicationToAliyunPrivateRepository")
    group = "publishing"
    description = "发布到阿里云私有仓库"
}

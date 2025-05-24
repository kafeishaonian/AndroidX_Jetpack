pluginManagement {
    repositories {
        maven {
//            url = uri("file:///${rootDir}/local-maven-repo/")
            url = uri(rootDir.resolve("local-maven-repo").toURI())
            content {
                includeGroup("com.example") // 限定仅扫描该组
            }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            setUrl("https://maven.aliyun.com/repository/jcenter")
        }
        maven {
            setUrl("https://jitpack.io")
        }
    }
    plugins {
        kotlin("jvm") version "1.9.0"
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.example.router-plugin") {
                useModule("com.example:router-plugin:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    //强制优先使用Settings仓库
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            url = uri(rootDir.toPath().resolve("local-maven-repo").toUri())
        }
        google()
        mavenCentral()
        maven {
            setUrl("https://maven.aliyun.com/repository/jcenter")
        }
        maven {
            setUrl("https://jitpack.io")
        }
    }
}

rootProject.name = "AndroidX_Jetpack"
include(":app")
include(":JetpackMvvm")
include(":Thread-P2P-Module")
include(":Router-Module")
include(":router-plugin")

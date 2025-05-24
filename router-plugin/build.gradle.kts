plugins {
    id("kotlin")
    id("maven-publish")
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin.jvmName)
}

dependencies {
    compileOnly(libs.asm.gradle)
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.asm.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.util)
    compileOnly(gradleApi())
}

gradlePlugin {
    plugins {
        create("routerPlugin") {
            id = "com.example.router-plugin"
            implementationClass = "com.example.router_plugin.RouterPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.example"
            artifactId = "router-plugin"
            version = "1.0.0"
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(
                rootDir.resolve("local-maven-repo").toURI()
                    .toString().replace("\\", "/")
            )
        }
    }
}
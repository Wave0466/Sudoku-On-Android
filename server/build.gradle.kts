plugins {
    id("java-library")
    kotlin("jvm")
}

dependencies {
    // Ktor 用于网络通信
    implementation("io.ktor:ktor-network:2.3.10")
    // Kotlin 协程核心库
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
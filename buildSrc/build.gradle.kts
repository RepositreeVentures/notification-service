plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.3.5")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.6")
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.4.3")
}

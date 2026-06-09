plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.google.cloud.tools.jib")
}
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
springBoot { buildInfo() }
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-parameters"))
}
repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "CodeArtifact"
        url = uri("https://repositree-955413563895.d.codeartifact.ap-south-1.amazonaws.com/maven/platform-libs/")
        credentials {
            username = "aws"
            password = providers.environmentVariable("CODEARTIFACT_AUTH_TOKEN").orElse("").get()
        }
    }
}
dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5") }
}
jib {
    from { image = "eclipse-temurin:21-jre-jammy" }
    to {
        image = "ghcr.io/repositreeventures/${project.name}"
        tags = setOf(System.getenv("IMAGE_TAG") ?: "latest", "latest")
        auth {
            username = System.getenv("GHCR_USERNAME") ?: ""
            password = System.getenv("GHCR_TOKEN") ?: ""
        }
    }
    container {
        user = "1000:1000"
        jvmFlags = listOf("-XX:+UseZGC", "-XX:+ZGenerational", "-XX:MaxRAMPercentage=75.0")
        ports = listOf("8080")
        labels = mapOf("org.opencontainers.image.source" to "https://github.com/RepositreeVentures/notification-service")
    }
}
tasks.withType<Test> { useJUnitPlatform() }

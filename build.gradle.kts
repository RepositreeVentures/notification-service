plugins {
    id("repositree.spring-service")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

val platformLibsVersion = "0.1.3"

dependencies {
    // platform-libs
    implementation("io.repositree.libs:repositree-common:$platformLibsVersion")
    implementation("io.repositree.libs:repositree-tenant:$platformLibsVersion")
    implementation("io.repositree.libs:repositree-outbox:$platformLibsVersion")
    implementation("io.repositree.libs:repositree-audit:$platformLibsVersion")
    implementation("io.repositree.libs:repositree-observability:$platformLibsVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("io.repositree.libs:repositree-test:$platformLibsVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.postgresql:postgresql")
}

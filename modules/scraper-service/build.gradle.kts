plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
  id("org.springframework.boot")
  id("io.spring.dependency-management")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":modules:scraper-domain"))
  implementation(project(":modules:scraper-application"))
  implementation(project(":modules:scraper-adapter-kafka"))
  implementation(project(":modules:scraper-adapter-pg"))
  implementation(project(":modules:db-changelog"))
  implementation(project(":modules:shared-agent-core"))
  implementation(platform(libs.spring.boot))
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("org.liquibase:liquibase-core:4.28.0")
  runtimeOnly("org.postgresql:postgresql:42.7.3")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.kafka:spring-kafka")
  testImplementation("org.springframework.kafka:spring-kafka-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
  testRuntimeOnly("com.h2database:h2")

  // Testcontainers dependencies
  testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.6"))
  testImplementation("org.testcontainers:kafka")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.awaitility:awaitility:4.2.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed", "standardOut", "standardError")
  }
}

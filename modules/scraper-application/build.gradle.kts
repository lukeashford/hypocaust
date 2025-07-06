plugins {
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":modules:scraper-domain"))
  implementation(project(":modules:shared-agent-core"))
  implementation(platform(libs.spring.boot))
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("io.micrometer:micrometer-core")
  implementation("org.jsoup:jsoup:1.15.3")
  implementation("org.slf4j:slf4j-api")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.23.1")
  implementation("com.squareup.okhttp3:okhttp:4.9.3")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

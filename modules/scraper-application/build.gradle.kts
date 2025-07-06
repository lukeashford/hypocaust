plugins {
  kotlin("jvm")
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

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
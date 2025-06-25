plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
}

dependencies {
  implementation(project(":modules:scraper-domain"))
  implementation(project(":modules:scraper-application"))
  implementation(project(":modules:shared-agent-core"))
  implementation(platform(libs.spring.boot))
  implementation(libs.spring.kafka)
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.slf4j:slf4j-api")
}

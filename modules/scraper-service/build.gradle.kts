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
  implementation(platform(libs.spring.boot))
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.liquibase:liquibase-core:4.28.0")
  runtimeOnly("org.postgresql:postgresql:42.7.3")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed", "standardOut", "standardError")
  }
}

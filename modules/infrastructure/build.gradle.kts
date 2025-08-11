plugins {
  java
  id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-parameters")
}

repositories {
  mavenCentral()
}

dependencies {
  // Spring Boot dependencies (without starter)
  implementation("org.springframework.boot:spring-boot-starter")

  // Lombok for reducing boilerplate code
  compileOnly("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
  annotationProcessor("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
  testCompileOnly("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
  testAnnotationProcessor("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")

  // Test dependencies
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
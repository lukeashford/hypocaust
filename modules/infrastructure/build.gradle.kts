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

extra["springBootVersion"] = "3.5.0"

dependencies {
  // Spring Boot dependencies (without starter)
  implementation("org.springframework.boot:spring-boot-starter")

  // Lombok for reducing boilerplate code
  compileOnly("org.projectlombok:lombok:1.18.30")
  annotationProcessor("org.projectlombok:lombok:1.18.30")
  testCompileOnly("org.projectlombok:lombok:1.18.30")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

  // Test dependencies
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:${extra["springBootVersion"]}")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
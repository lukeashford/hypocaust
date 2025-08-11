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

extra["springAiVersion"] = "1.0.0"
extra["springBootVersion"] = "3.5.0"
extra["langchain4jVersion"] = "1.1.0"
extra["langgraph4jVersion"] = "1.6.0-rc4"
extra["lombokVersion"] = "1.18.30"

dependencies {
  // Root project has no direct dependencies - all dependencies moved to appropriate modules
}

subprojects {
  apply(plugin = "io.spring.dependency-management")

  dependencyManagement {
    imports {
      mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.extra["springBootVersion"]}")
      mavenBom("org.springframework.ai:spring-ai-bom:${rootProject.extra["springAiVersion"]}")
      mavenBom("dev.langchain4j:langchain4j-bom:${rootProject.extra["langchain4jVersion"]}")
      mavenBom("org.bsc.langgraph4j:langgraph4j-bom:${rootProject.extra["langgraph4jVersion"]}")
    }
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
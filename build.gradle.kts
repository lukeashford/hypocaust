plugins {
  java
  kotlin("jvm") version "1.9.20"
  kotlin("plugin.spring") version "1.9.20"
  id("org.springframework.boot") version "3.3.0"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

allprojects {
  repositories {
    mavenCentral()
  }

  plugins.withId("org.jetbrains.kotlin.jvm") {
    kotlin {
      jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
      }
    }
  }
}

extra["springAiVersion"] = "1.0.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.ai:spring-ai-starter-model-openai")

  // Lombok for reducing boilerplate code
  compileOnly("org.projectlombok:lombok:1.18.30")
  annotationProcessor("org.projectlombok:lombok:1.18.30")
  testCompileOnly("org.projectlombok:lombok:1.18.30")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.ai:spring-ai-bom:${extra["springAiVersion"]}")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

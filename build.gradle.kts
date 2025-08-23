plugins {
  java
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Core Spring Boot functionality (BOM managed)
  implementation(libs.bundles.spring.boot.core)
  implementation(libs.postgresql)
  implementation(libs.bundles.flyway)

  // Spring AI (BOM managed)
  implementation(libs.spring.ai.starter.model.openai)

  // Annotation processing tools (explicitly versioned)
  compileOnly(libs.lombok)
  implementation(libs.mapstruct)
  annotationProcessor(libs.lombok)
  annotationProcessor(libs.mapstruct.processor)
  testCompileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)
  testAnnotationProcessor(libs.mapstruct.processor)

  // Testing dependencies (BOM managed)
  testImplementation(libs.bundles.testing.core)
  testImplementation(libs.bundles.testing.containers)
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.spring.ai.get()}")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

// Podman development tasks
tasks.register<Exec>("ensurePodmanMachineRunning") {
  group = "podman-dev"
  commandLine("sh", "podman/scripts/preparePodman.sh")
  isIgnoreExitValue = true
}

tasks.register<Exec>("pods-clear") {
  description = "Remove all Pods and Volumes"
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/clear.sh")
}

tasks.register<Exec>("pods-stop") {
  description = "Stops and removes previously started pods."
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/stop.sh")
}

tasks.register<Exec>("pods-start") {
  description = "Start pods"
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/start.sh")
}

tasks.register<Exec>("pods-create") {
  dependsOn("ensurePodmanMachineRunning")
  description = "create and start pods, replace all previously pods."
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/create.sh")
}

tasks.register<Exec>("pods-clearPostgres") {
  description = "recreate the_machine postgres database"
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/clearPostgres.sh")
}
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.jpa)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

kotlin {
  jvmToolchain(21)
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
    freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Kotlin essentials
  implementation(libs.bundles.kotlin.core)

  // Kotlin Serialization
  implementation(libs.kotlinx.serialization.json)

  // Core Spring Boot functionality (BOM managed)
  implementation(libs.bundles.spring.boot.core)
  implementation(libs.postgresql)
  implementation(libs.bundles.flyway)

  // UUID generators
  implementation(libs.java.uuid.generator)

  // Spring AI (BOM managed)
  implementation(libs.spring.ai.openai)
  implementation(libs.spring.ai.anthropic)

  // Annotation processing tools
  implementation(libs.mapstruct)
  kapt(libs.mapstruct.processor)

  // Lombok for test files
  testImplementation(libs.lombok)

  // Testing dependencies (BOM managed)
  testImplementation(libs.mockk)
  testImplementation(libs.bundles.testing.core)
  testImplementation(libs.bundles.testing.containers)
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.spring.ai.get()}")
  }
}

tasks.withType<Test>().configureEach {
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
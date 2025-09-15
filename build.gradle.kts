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

val mockitoAgent by configurations.creating { isTransitive = false }

dependencies {
  // Core Spring Boot functionality (BOM managed)
  implementation(libs.bundles.spring.boot.core)
  implementation(libs.bundles.db)

  // Tools
  implementation(libs.bundles.tools)

  // Spring AI (BOM managed)
  implementation(libs.bundles.spring.ai)

  // Annotation processing tools (explicitly versioned)
  compileOnly(libs.lombok)
  implementation(libs.mapstruct)
  annotationProcessor(libs.lombok)
  annotationProcessor(libs.mapstruct.processor)
  testCompileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)
  testAnnotationProcessor(libs.mapstruct.processor)

  // Testing dependencies (BOM managed)
  testImplementation(libs.mockito.core)
  testImplementation(libs.bundles.testing.core)
  testImplementation(libs.bundles.testing.containers)
  mockitoAgent(libs.mockito.core)
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.spring.ai.get()}")
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  // Load Mockito as a javaagent; also silence the CDS warning seen with agents
  jvmArgs(
    "-javaagent:${configurations["mockitoAgent"].asPath}",
    "-Xshare:off"
  )
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
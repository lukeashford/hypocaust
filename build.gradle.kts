plugins {
  java
  alias(libs.plugins.spring.boot)
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

val mockitoAgent by configurations.creating

dependencies {
  implementation(platform(libs.springboot.dependencies))
  implementation(platform(libs.springai.bom))

  // Core Spring Boot functionality (BOM managed)
  implementation(libs.bundles.spring.boot.core)
  implementation(libs.bundles.db)

  // Tools
  implementation(libs.bundles.tools)

  // Storage
  implementation(libs.minio)

  // OpenAPI / Swagger
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

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
  mockitoAgent(platform(libs.springboot.dependencies))
  mockitoAgent(libs.mockito.core)
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("io.swagger.core.v3:swagger-annotations"))
      .using(module("io.swagger.core.v3:swagger-annotations-jakarta:2.2.29"))
      .because("Universal migration to Jakarta annotations to avoid split-package conflicts between springdoc and spring-ai")
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  // Load Mockito as a javaagent; also silence the CDS warning seen with agents
  jvmArgs(
    "-javaagent:${configurations["mockitoAgent"].filter { it.name.startsWith("mockito-core") }.asPath}",
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
  description = "recreate hypocaust postgres database"
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/clearPostgres.sh")
}

tasks.register<Exec>("pods-restart") {
  description = "Restart all pods"
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/restart.sh")
}

tasks.register<Exec>("pods-delete") {
  description = "Delete all pods and containers (keep volumes)"
  group = "podman-dev"
  commandLine("sh", "./podman/scripts/delete.sh")
}

tasks.register<Exec>("pods-status") {
  description = "Show status of all pods and containers"
  group = "podman-dev"
  commandLine("sh", "-c", "podman ps -a --filter name=hypocaust")
}
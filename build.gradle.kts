plugins {
  java
  id("org.springframework.boot") version "3.5.0"
  id("io.spring.dependency-management") version "1.1.7"
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

extra["springAiVersion"] = "1.0.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.ai:spring-ai-starter-model-openai")
  implementation("org.postgresql:postgresql")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  
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
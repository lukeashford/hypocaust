plugins {
  java
  id("org.springframework.boot") version "3.5.1"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.github.node-gradle.node") version "7.0.2"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

node {
  version = "22.15.0"  // Match system version
  npmVersion = "10.7.0"
  download = true
  workDir = file("src/main/frontend")
  nodeProjectDir = file("src/main/frontend")
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-parameters")
}

repositories {
  mavenCentral()
}

dependencies {
  // Module dependencies
  implementation(project(":modules:ai-graph"))

  // Spring Boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web")

  // OpenAPI documentation
  implementation(platform("org.springdoc:springdoc-openapi-bom:2.8.9"))
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

  // Force newer commons-lang3 to override vulnerable transitive dependency
  implementation("org.apache.commons:commons-lang3:3.18.0")

  // LangChain4j dependencies
  implementation("dev.langchain4j:langchain4j")

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

// API type generation task
tasks.register<com.github.gradle.node.npm.task.NpmTask>("generateApiTypes") {
  group = "frontend"
  description =
    "Generate TypeScript types from OpenAPI specification (requires running backend server)"
  dependsOn("npmInstall")

  workingDir.set(file("src/main/frontend"))
  args.set(listOf("run", "generate:api-types"))

  inputs.property("api-endpoint", "http://localhost:8080/api-docs")
  outputs.file("src/main/frontend/src/generated/api.ts")
  outputs.cacheIf { true }
}

// Frontend build tasks using Node.js plugin
tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildFrontend") {
  dependsOn("npmInstall")
  workingDir.set(file("src/main/frontend"))
  args.set(listOf("run", "build"))

  // Better caching with comprehensive inputs
  inputs.dir("src/main/frontend/src")
  inputs.file("src/main/frontend/package.json")
  inputs.file("src/main/frontend/vite.config.mjs")
  inputs.file("src/main/frontend/postcss.config.js")
  inputs.files(fileTree("src/main/frontend") {
    include("tailwind.config.*")
    include("tsconfig.json")
    include("index.html")
  })
  outputs.dir("src/main/resources/static")
  outputs.cacheIf { true }
}

tasks.named("processResources") {
  dependsOn("buildFrontend")
}
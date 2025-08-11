plugins {
  `java-library`
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
  // Module dependencies
  implementation(project(":modules:infrastructure"))

  // Spring Boot dependencies (without starter)
  implementation("org.springframework.boot:spring-boot-starter")

  implementation("dev.langchain4j:langchain4j")
  implementation("dev.langchain4j:langchain4j-open-ai")
  implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2")
  implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom")

  // LangGraph4j dependencies
  api("org.bsc.langgraph4j:langgraph4j-core")
  api("org.bsc.langgraph4j:langgraph4j-langchain4j")

  // Lucene dependencies for BM25 scoring
  implementation("org.apache.lucene:lucene-core:9.10.0")
  implementation("org.apache.lucene:lucene-queryparser:9.10.0")

  // Jsoup for HTML parsing
  implementation("org.jsoup:jsoup:1.17.2")

  // Content extraction libraries
  implementation("net.dankito.readability4j:readability4j:1.0.8")

  // Force newer kotlin-stdlib to override vulnerable transitive dependency
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")

  // Caffeine cache
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

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
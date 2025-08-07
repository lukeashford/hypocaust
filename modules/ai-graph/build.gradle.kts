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

extra["springAiVersion"] = "1.0.0"
extra["springBootVersion"] = "3.5.0"

dependencies {
  // Module dependencies
  implementation(project(":modules:infrastructure"))

  // Spring Boot dependencies (without starter)
  implementation("org.springframework.boot:spring-boot-starter")

  // LangChain4j dependencies
  implementation(platform("dev.langchain4j:langchain4j-bom:1.1.0"))
  api(platform("org.bsc.langgraph4j:langgraph4j-bom:1.6.0-rc4"))

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

  // Caffeine cache
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

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
    mavenBom("org.springframework.ai:spring-ai-bom:${extra["springAiVersion"]}")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
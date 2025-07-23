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

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-parameters")
}

repositories {
  mavenCentral()
}

extra["springAiVersion"] = "1.0.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.ai:spring-ai-starter-model-openai")

  // LangChain4j dependencies
  implementation(platform("dev.langchain4j:langchain4j-bom:1.1.0"))

  implementation("dev.langchain4j:langchain4j")
  implementation("dev.langchain4j:langchain4j-open-ai")
  implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom")

  // Lucene dependencies for BM25 scoring
  implementation("org.apache.lucene:lucene-core:9.10.0")
  implementation("org.apache.lucene:lucene-queryparser:9.10.0")

  // Jsoup for HTML parsing
  implementation("org.jsoup:jsoup:1.17.2")

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
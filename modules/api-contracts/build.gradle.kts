plugins {
  `java-library`
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
  // Jackson for JSON serialization
  implementation("com.fasterxml.jackson.core:jackson-annotations")

  // Lombok for reducing boilerplate code
  compileOnly("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
  annotationProcessor("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
  testCompileOnly("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
  testAnnotationProcessor("org.projectlombok:lombok:${rootProject.extra["lombokVersion"]}")
}
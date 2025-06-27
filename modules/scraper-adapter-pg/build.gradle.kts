plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
}

dependencies {
  implementation(project(":modules:scraper-application"))
  implementation(project(":modules:scraper-domain"))
  implementation(platform(libs.spring.boot))
  implementation(libs.exposed.jdbc)
  implementation("org.jetbrains.exposed:exposed-java-time:0.46.0")
  implementation("org.postgresql:postgresql")
  implementation("com.zaxxer:HikariCP")
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
}

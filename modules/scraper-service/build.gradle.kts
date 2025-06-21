plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":modules:scraper-domain"))
    implementation(project(":modules:scraper-application"))
    implementation(project(":modules:scraper-adapter-kafka"))
    implementation(project(":modules:scraper-adapter-pg"))
    implementation(platform(libs.spring.boot))
    implementation("org.springframework.boot:spring-boot-starter")
}
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:scraper-application"))
    implementation(platform(libs.spring.boot))
    implementation(libs.spring.kafka)
}

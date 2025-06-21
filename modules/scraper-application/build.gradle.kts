plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:scraper-domain"))
    implementation(platform(libs.spring.boot))
}
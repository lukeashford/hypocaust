plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:scraper-domain"))
    implementation(project(":modules:shared-agent-core"))
    implementation(platform(libs.spring.boot))
}

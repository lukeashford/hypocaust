rootProject.name = "the_machine"

include(
  "modules:scraper-domain",
  "modules:scraper-application",
  "modules:scraper-adapter-kafka",
  "modules:scraper-adapter-pg",
  "modules:scraper-service",
  "modules:db-changelog",
  "modules:shared-agent-core"
)

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      library("spring-boot", "org.springframework.boot:spring-boot-dependencies:3.3.0")
      version("spring-kafka", "3.1.0")
      version("kotlin-coroutines", "1.7.3")
      version("exposed", "0.46.0")

      library(
        "spring-kafka",
        "org.springframework.kafka",
        "spring-kafka"
      ).versionRef("spring-kafka")
      library("exposed-jdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
    }
  }
}

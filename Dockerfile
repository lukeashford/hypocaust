FROM eclipse-temurin:21-jre

WORKDIR /app

COPY modules/scraper-service/build/libs/scraper-service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
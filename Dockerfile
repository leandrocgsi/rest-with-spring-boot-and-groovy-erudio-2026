FROM eclipse-temurin:25-jdk
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

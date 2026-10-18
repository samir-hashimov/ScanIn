# 1-ci Mərhələ: Gradle versiyasını 9.0 (və ya latest) edirik ki, Spring Boot-un tələbini ödəsin
FROM gradle:9.0-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle clean bootJar --no-daemon -x test

# 2-ci Mərhələ: Yalnız hazır JAR faylını yüngül Java mühitində işə salırıq
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Saat qurşağını Bakı vaxtına (Asia/Baku) tənzimləyirik
RUN apk add --no-cache tzdata
ENV TZ=Asia/Baku

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
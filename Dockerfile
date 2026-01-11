# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package


# ---------- RUN STAGE ----------
FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"

EXPOSE 8080
CMD ["-jar", "/app/app.jar"]

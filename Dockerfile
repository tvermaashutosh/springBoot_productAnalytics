# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom first -> allows dependency cache layer
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Now copy sources
COPY src ./src
RUN mvn -q -DskipTests package


# ---------- RUN STAGE ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

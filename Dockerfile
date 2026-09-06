# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Run stage ----
FROM tomcat:10.1-jdk17

RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    pip3 install --no-cache-dir --break-system-packages groq && \
    rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/insightline.war /usr/local/tomcat/webapps/ROOT.war

WORKDIR /usr/local/tomcat
COPY scripts ./scripts
COPY recordings ./recordings
RUN mkdir -p transcripts_tmp

EXPOSE 8080
CMD ["catalina.sh", "run"]
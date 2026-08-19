FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --create-home --shell /bin/bash switch \
  && chown -R switch:switch /app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER switch

EXPOSE 8090

# Pas d'actuator dans ce service (pom.xml) et toutes les routes /api/switch/*
# exigent une signature HMAC (X-Monetique-Signature) : impossible de sonder
# une route metier sans re-signer une requete. On verifie donc uniquement
# que le port applicatif accepte les connexions TCP.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD bash -c '</dev/tcp/localhost/8090' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

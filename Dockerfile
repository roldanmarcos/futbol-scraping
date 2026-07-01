# ============================================================
# STAGE 1: Build with Maven + JDK 21
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiar todo el proyecto
COPY . .

# Compilar y empaquetar (sin tests para acelerar en Docker)
RUN mvn clean package -DskipTests -B -q && mv target/*.jar app.jar

# ============================================================
# STAGE 2: Runtime liviano con JRE 21 + Chrome
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# ------------------------------------------------------------
# Instalar Google Chrome (Selenium 4 usa Selenium Manager
# para obtener ChromeDriver automáticamente)
# ------------------------------------------------------------
RUN apt-get update && apt-get install -y \
    wget \
    gnupg2 \
    curl \
    fonts-freefont-ttf \
    fonts-noto-color-emoji \
    --no-install-recommends \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub \
        | gpg --dearmor -o /usr/share/keyrings/google-chrome-keyring.gpg \
    && echo 'deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome-keyring.gpg] http://dl.google.com/linux/chrome/deb/ stable main' \
        > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update && apt-get install -y google-chrome-stable --no-install-recommends \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

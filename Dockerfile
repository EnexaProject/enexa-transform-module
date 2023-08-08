FROM maven:3.9-eclipse-temurin-17-alpine

WORKDIR /app

# Add enexa-utils
COPY --from=enexa-utils:1 / /.

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
COPY target/lib           /app/lib

# Add module script
COPY module /app/module

# Add the service itself
ARG JAR_FILE
COPY target/${JAR_FILE} /app/enexa-transform.jar

# Run
CMD ./module

FROM hub.cs.upb.de/enexa/images/enexa-utils:1 as enexa-utils
FROM maven:3.9-eclipse-temurin-17-alpine

WORKDIR /app

# Add enexa-utils
COPY --from=enexa-utils / /.

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
COPY target/lib           /app/lib

# Add module script
COPY module /app/module

# Add the service itself
ARG JAR_FILE
COPY target/${JAR_FILE} /app/enexa-transform.jar

# Run
CMD ./module

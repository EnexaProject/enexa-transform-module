FROM maven:3.9-eclipse-temurin-17-alpine
MAINTAINER Michael Röder <michael.roeder@uni-paderborn.de>

WORKDIR /app

# Add enexa-utils
COPY --from=enexa-utils:1 / /.

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD target/lib           /app/lib

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /app/enexa-transform.jar

# Run
CMD module

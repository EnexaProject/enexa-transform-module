TAG=enexa-example-module:latest

build:
	mvn clean package com.spotify:dockerfile-maven-plugin:build

test:
	docker run --rm \
	-v $(PWD)/test-shared-dir:/shared \
	-e ENEXA_SHARED_DIRECTORY=/shared \
	-e ENEXA_META_DATA_ENDPOINT=http://admin:admin@fuseki:3030/test \
	-e ENEXA_SERVICE_URL=http://enexa:36321/ \
	-e ENEXA_WRITEABLE_DIRECTORY=/shared/experiment1 \
	-e ENEXA_MODULE_INSTANCE_IRI=http://example.org/moduleinstance-$$(date +%s) \
	-e TEST_RUN=true \
	--network enexa-utils_default \
	$(TAG)

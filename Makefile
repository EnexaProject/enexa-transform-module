TAG=docker.io/library/dice-group/enexa-transform:0.0.1-SNAPSHOT
TEST_DIR=test-shared-dir

build:
	mvn clean package com.spotify:dockerfile-maven-plugin:build

test:
	[ -d $(TEST_DIR) ] || mkdir -p $(TEST_DIR)
	cp src/test/resources/org/dice_research/enexa/transform/expectedParameters.ttl $(TEST_DIR)/data1.ttl
	cp src/test/resources/org/dice_research/enexa/transform/testDataset.ttl $(TEST_DIR)/data2.ttl
	docker run --rm \
	-v $(PWD)/$(TEST_DIR):/shared \
	-e ENEXA_EXPERIMENT_IRI=http://example.org/experiment1 \
	-e ENEXA_META_DATA_ENDPOINT=http://admin:admin@fuseki:3030/test \
	-e ENEXA_META_DATA_GRAPH=http://example.org/meta-data \
	-e ENEXA_MODULE_INSTANCE_DIRECTORY=/shared/experiment1/module1 \
	-e ENEXA_MODULE_INSTANCE_IRI=http://example.org/moduleinstance-$$(date +%s) \
	-e ENEXA_SERVICE_URL=http://enexa:36321/ \
	-e ENEXA_SHARED_DIRECTORY=/shared \
	-e ENEXA_WRITEABLE_DIRECTORY=/shared/experiment1 \
	-e TEST_RUN=true \
	--network enexa-utils_default \
	$(TAG)

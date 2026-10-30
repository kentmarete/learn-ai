GRADLE ?= ./gradlew
COMPOSE ?= docker compose

.PHONY: build run test ask docker-build docker-run clean

# Local
build:
	$(GRADLE) bootJar

run:
	$(GRADLE) bootRun

test:
	$(GRADLE) test

ask:
	curl -s -X POST localhost:8000/ask -H 'Content-Type: application/json' \
		-d '{"query": "What is the weather in Nairobi?"}'

# Docker
docker-build:
	$(COMPOSE) build

docker-run: docker-build
	$(COMPOSE) up

clean:
	$(GRADLE) clean

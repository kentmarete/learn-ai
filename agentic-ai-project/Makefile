PYTHON ?= python
COMPOSE ?= docker compose

.PHONY: install run test ask all docker-build docker-run docker-test clean

# Local
install:
	$(PYTHON) -m pip install -r requirements.txt

run:
	$(PYTHON) main.py

test:
	$(PYTHON) -m pytest -q

ask:
	curl -s -X POST localhost:8000/ask -H 'Content-Type: application/json' \
		-d '{"query": "What is the weather in Nairobi?"}'

all: install test run

# Docker
docker-build:
	$(COMPOSE) build

docker-run: docker-build
	$(COMPOSE) up

docker-test: docker-build
	$(COMPOSE) run --rm app python -m pytest -q

clean:
	rm -rf .pytest_cache
	find . -name __pycache__ -type d -prune -exec rm -rf {} +

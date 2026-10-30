PYTHON ?= python
COMPOSE ?= docker compose

.PHONY: install train predict all docker-build docker-train docker-predict docker-all clean

# Local
install:
	$(PYTHON) -m pip install -r requirements.txt

train:
	$(PYTHON) train.py

predict:
	$(PYTHON) predict.py

all: install train predict

# Docker
docker-build:
	$(COMPOSE) build

docker-train: docker-build
	$(COMPOSE) run --rm app python train.py

docker-predict: docker-build
	$(COMPOSE) run --rm app python predict.py

docker-all: docker-train docker-predict

clean:
	rm -rf models __pycache__

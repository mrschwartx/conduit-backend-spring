COMPOSE = docker compose -f docker-compose.local.yml

.PHONY: docker/build docker/up docker/down docker/logs docker/ps docker/dev

docker/build:
	$(COMPOSE) build

docker/up:
	$(COMPOSE) up -d --build

docker/down:
	$(COMPOSE) down

docker/logs:
	$(COMPOSE) logs -f conduit-backend

docker/ps:
	$(COMPOSE) ps

docker/dev:
	docker compose -f docker-compose.dev.yml up -d conduit-postgres

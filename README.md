# learning-zio

A pile of code I'm using to learn [ZIO](https://zio.dev)

## Running this project

```bash
cp .env.example .env
docker-compose up
```

## Running the integration tests
```
cp .env.example .env
docker-compose up -d
docker-compose exec app sbt 'testOnly integration.*'
```

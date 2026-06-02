# Github Proxy

Lekka aplikacja Spring Boot działająca jako proxy do GitHub API. Zwraca listę repozytoriów (bez forków) dla podanego użytkownika wraz z listą gałęzi i SHA ostatnich commitów.

## Funkcje

- Zwraca tylko repozytoria, które nie są forkami
- Dla każdego repozytorium zwraca gałęzie z nazwą i SHA ostatniego commita
- Obsługa błędów: 404 gdy użytkownik nie istnieje, 406 dla nieobsługiwanego nagłówka Accept

## Endpoint

- GET /api/repositories/{username}
  - Nagłówek: `Accept: application/json`
  - Zwraca: JSON — lista obiektów `RepositoryResponse`
    - `repositoryName`: string
    - `ownerLogin`: string
    - `branches`: [{ `name`: string, `lastCommitSha`: string }]

## Przykład zapytania

```bash
curl -s -H "Accept: application/json" http://localhost:8080/api/repositories/test-user
```

## Przykładowa odpowiedź

```json
[
  {
    "repositoryName": "repo-1",
    "ownerLogin": "test-user",
    "branches": [
      { "name": "main", "lastCommitSha": "abcdef123456" }
    ]
  }
]
```

## Obsługa błędów

- 404 JSON: `{ "status": 404, "message": "User not found on GitHub: {username}" }` — gdy użytkownik nie istnieje
- 406 Not Acceptable — gdy `Accept` nie zawiera `application/json`

## Konfiguracja

- Property: `github.api.url` — adres GitHub API (domyślnie `https://api.github.com`)
  - Można nadpisać w `src/main/resources/application.properties` lub przez zmienne środowiskowe

## Budowanie i uruchamianie

- Zbudować: `./gradlew build`
- Uruchomić: `./gradlew bootRun` lub
- Uruchomić wygenerowany JAR: `java -jar build/libs/github-proxy-0.0.1-SNAPSHOT.jar`

> Uwaga: projekt korzysta z Gradle wrapper — używać `./gradlew` (Windows: `gradlew.bat`).

## Testy

- Testy uruchamiane przez Gradle (JUnit + WireMock)
- Integration testy używają WireMock do stubowania odpowiedzi GitHub
- Uruchom: `./gradlew test`

## Wymagania

- Java (konfiguracja toolchain w `build.gradle` ustawiona na Java 25)
- Gradle wrapper (dołączony)

## Uwagi implementacyjne

- `GithubClient` używa `RestClient.Builder` (bean zdefiniowany w `RestClientConfig`)
- `GithubService` filtruje forki i mapuje dane do DTO używanych przez API

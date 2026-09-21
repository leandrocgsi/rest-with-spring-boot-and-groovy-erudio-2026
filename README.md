# rest-with-spring-boot-and-groovy-erudio-2026

API REST com **Spring Boot 4.1.1**, **Groovy 5** e **Java 25**, portada do projeto [rest-with-spring-boot-and-java-erudio-2026](https://github.com/leandrocgsi/rest-with-spring-boot-and-java-erudio-2026) mantendo os mesmos frameworks, o mesmo contrato de API e a mesma organização de pacotes, mas escrita com as práticas do Groovy (`@CompileStatic`, propriedades, AST transformations, closures) e testada com **Spock**.

Gerencia pessoas e livros, com autenticação JWT, upload/download de arquivos, importação e exportação em CSV/XLSX/PDF e envio de e-mails.

## Como rodar

Pré-requisitos: JDK 25 e Docker.

```bash
./gradlew bootRun                         # sobe a API na 8080 (precisa de MySQL em localhost:3306)
./gradlew clean build                     # compila, roda os testes e gera build/libs/*.jar
./gradlew test                            # todos os testes (precisa do Docker, usa Testcontainers)
./gradlew test --tests '*PersonServiceSpec'
docker compose up -d --build              # API na 8080 e MySQL 9 na 3308 (rode ./gradlew clean bootJar antes)
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`. Login do seed: `leandro` / `admin123`.

A collection do Postman e as instruções para rodá-la com o newman estão em [`Collections/`](Collections/README.md).

O `CLAUDE.md` descreve a arquitetura, as decisões da portagem para Groovy e as armadilhas conhecidas.

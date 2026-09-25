# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 4.1.1 / Groovy 5.0.8 / Java 25 REST API (Gradle, base package `br.com.erudio`). It manages People and Books, with JWT auth, file upload/download, CSV/XLSX/PDF import/export and email sending. The code is idiomatic Groovy under `@CompileStatic` and is tested with Spock.

The course title strings in `OpenApiConfig` and in the Postman collection say "com Groovy" as a placeholder; there is no known Groovy course to name.

## Commands

```bash
./gradlew bootRun                              # run the app (needs MySQL, see below)
./gradlew clean build                          # compile, run every test, build build/libs/*.jar
./gradlew test                                 # everything (needs Docker)
./gradlew test --tests '*PersonServiceSpec'    # one spec
./gradlew test --tests 'br.com.erudio.unittests.*'   # unit specs only, no Docker needed
./gradlew flywayMigrate                        # apply migrations manually (build.gradle hardcodes localhost DB + root/admin123)
docker compose up -d --build                   # app on :8080 + MySQL 9 on :3308 (run "./gradlew clean bootJar" first, the image copies build/libs/*.jar)
```

Gradle 9.7.1 through the wrapper (`gradlew` must stay LF, see `.gitattributes`), JDK 25 through the toolchain. Versions of libraries the Boot BOM does not manage live in `gradle.properties`; plugin versions are resolved from the same file in `settings.gradle`. `openpdf`, which `jasperreports-pdf` needs, is not on Maven Central: Gradle does not read the `<repositories>` of a dependency pom, so `build.gradle` adds the Jaspersoft `third-party-ce-artifacts` repository for group `com.github.librepdf`. `org.objenesis:objenesis` is declared explicitly because Spock needs it to mock classes and nothing else brings it in.

## Language conventions

- `@CompileStatic` is applied to all main code through `gradle/compiler-config.groovy` (`groovyOptions.configurationScript` on `compileGroovy` only). Test code is dynamic Groovy. Parameter names are compiled in (`groovyOptions.parameters`).
- No comments in code (owner's rule), no semicolons, single quotes unless interpolating, `def`/inference for locals, explicit types on members and signatures.
- Properties instead of getters/setters, `@EqualsAndHashCode` instead of hand-written `equals`/`hashCode`, `@Slf4j` (the logger is `log`), `@TupleConstructor(includeFields = true, defaults = false)` for constructor injection with `private final` fields (`defaults = false` matters: with the default overloads Spring would pick the no-arg constructor and inject nothing). A class that also has fields Spring does not inject limits the constructor with `includes` (`JwtTokenProvider`, `PdfExporter`). Only `FileStorageService` writes its constructor by hand, because it creates the upload directory in it.
- Groovy truth and `?.`/`?:` handle blank checks (`!value?.trim()` is "blank"). Closures are used instead of streams, and the spread operator for projections (`permissions*.description`). The exporter and importer factories are a `Map` from media type / extension to bean class; constant maps are `.asImmutable()`.
- Closure parameters are not typed: `@CompileStatic` infers them from the receiver. Use `it` for a one-line body and a short name when the body is longer or closures are nested. The exception is a closure that is returned or coerced with `as` (`JacksonConfig`), which has no receiver to infer from and keeps its explicit type.
- Request/response DTOs that carry no behavior are Groovy `record`s (`ExceptionResponse`, `TokenDTO`, `AccountCredentialsDTO`, `UploadFileResponseDTO`, `EmailRequestDTO`); `PersonDTO`/`BookDTO` stay classes because they extend `RepresentationModel`. `EmailMessage` is an `@Immutable` class (named arguments, optional fields). Nothing here implements `Serializable`.
- `User` declares its four account flags as primitive `boolean` properties, so Groovy generates the `isAccountNonExpired()`/`isAccountNonLocked()`/`isCredentialsNonExpired()`/`isEnabled()` methods that `UserDetails` asks for. The columns are `DEFAULT NULL` in the migration, but the seed rows and `AuthService.create` always set them; a row with `NULL` would fail to load.
- `PersonDTO.name` (the `name` field of the JasperReports templates) is `[firstName, lastName].findAll().join(' ')`: blank parts are dropped, so there is no leading or trailing space. `PersonDTOSpec` covers the null and empty cases.
- The negotiated mappings are `@AnnotationCollector` aliases: `@ApiGet`, `@ApiPost`, `@ApiPut` and `@ApiPatch` in `controllers/mapping/ApiMappings.groovy` expand at compile time into `@GetMapping`/`@PostMapping`/`@PutMapping`/`@PatchMapping` with the JSON/XML/YAML `produces` (and `consumes` for POST/PUT), and take the path as their value (`@ApiGet('/{id}')`). The format list lives only in that file, so dropping a format is an edit there plus `WebConfig` and the Jackson dependency. The media types are fully qualified inside the aliases because a collector copies the expressions into the class that uses it, where the file's imports do not exist. `massCreation` and the export endpoints keep an explicit `@PostMapping`/`@GetMapping` because their `produces`/`consumes` differ.
- Exceptions use default constructor arguments instead of overloads (`BadRequestException(String message = '...')`).
- Nothing is injected with field `@Autowired`; the unit tests build services with `new PersonService(repository, importerFactory, exporterFactory, assembler)`.

## Groovy gotchas

- `Resource.file` in Groovy resolves to `isFile()` (a boolean). Use `resource.getFile()` (see `FileController`).
- `ObjectMapper` (the project one, not Jackson's) uses Spring's `BeanUtils.copyProperties` and must ignore `metaClass`: Groovy objects have a `metaClass` property with the same type on both sides, and without the exclusion the DTO gets the entity's `MetaClass` and fails with `object of type PersonDTO is not an instance of Person`. `ObjectMapperSpec` guards it.
- Jackson 3 orders properties alphabetically, except for records, which keep the declared order. `TokenDTO`, `AccountCredentialsDTO` and `UploadFileResponseDTO` are records for that reason: a Groovy class with `@TupleConstructor` generates several constructors and comes out alphabetical. A DTO whose wire order matters has to be a record.
- Groovy adds methods to every record, among them `size()` (the number of components). A component named `size` therefore returns that count instead of its value, and the API answered `"size":4` for a 40-byte upload. `UploadFileResponseDTO` names the component `fileSize` and keeps the wire name with `@JsonProperty('size')`. Watch for component names that clash with generated methods.
- Under `@CompileStatic`, named-argument construction of a record requires every component; use `@Immutable` when some fields are optional (`EmailMessage`).
- Hamcrest `equalTo(...)` does not equal a `GString` to a `String`. Build the expected value with `.toString()` when it interpolates.
- JAXB (used by RestAssured for XML request bodies) chokes on `getMetaClass()`. The test DTOs use `@XmlAccessorType(FIELD)`, and `PersonDTO.books` is `@XmlTransient` because JAXB would otherwise introspect the `Book` entity.
- A `final` property in a Spock subclass conflicts with an abstract `getX()` inherited from the base spec; override the getter (see `codec` in the `with*` specs).
- The `where:` block of a Spock feature cannot read instance fields; use `static`/`@Shared`.
- A closure returned straight from a method, or coerced with `as`, gets no inferred parameter type under `@CompileStatic` (`The variable [it] is undeclared`); type the parameter there.

## Tests

Spock 2.4 (`2.4-groovy-5.0`) on the JUnit 6 platform of the Boot BOM; `*Spec` classes under `src/test/groovy`.
- `unittests/**` use Spock mocks and need nothing external.
- `integrationtests/**` and `repository/PersonRepositorySpec` extend `AbstractIntegrationSpec`, which starts a **Testcontainers `mysql:9.1.0`** (with `withConfigurationOverride("mysql-default-conf")`, without it book ties change order and `find all books` fails with `98.73` instead of `97.21`) and **GreenMail** (SMTP on a dynamic port, account `sender@erudio.test` / `secret`), so Docker must be running. The app then boots on port **8888** (`src/test/resources/application.yml`, `TestConfigs.SERVER_PORT`) and RestAssured calls it. Spring contexts are cached across specs: in a full run the web app (Tomcat, Flyway) starts once, plus a small `@DataJpaTest` context for `PersonRepositorySpec`.
- The order-dependent specs use `@Stepwise` with `@Shared` state. Run the whole class; a single feature fails because the token was never obtained.
- The JSON, XML and YAML controller specs are three thin subclasses (`withjson`, `withxml`, `withyaml`) of `AuthControllerSpec`, `PersonControllerSpec` and `BookControllerSpec`. The per-format parts (media type, request body encoding, response parsing, the pagination wrapper) live in `RepresentationCodec` and `JsonCodec`/`XmlCodec`/`YamlCodec`; only the HATEOAS check of `PersonController` differs per format and stays in the subclass. A change to an endpoint usually means one edit in the base spec.
- New integration specs extend `AuthenticatedIntegrationSpec`, which signs in once and hands out `authenticated()` / `anonymous()` request specs.
- The service specs assert the HATEOAS links for real (`HateoasSupport.linksOf`).
- The test `file.upload-dir` is `build/test-uploads`.
- PDF specs are skipped (`@Requires` on `NetworkAssumptions.reportImagesAreReachable()`) when `raw.githubusercontent.com` is unreachable, because the templates download images.
- Specs that import people (`massCreation`) delete what they created in `cleanup()`; the other integration specs assume the seeded people are the only ones present.

## Runtime setup

- MySQL at `localhost:3306`, schema `rest_with_spring_boot_erudio`, `root`/`admin123` (in `application.yml`). Flyway (`src/main/resources/db/migration/V1..V18`) runs at startup, with `ddl-auto: none`. Schema and seed changes go in a new `V<n>__*.sql`; if an existing migration is edited, drop the schema (`docker compose down -v`) to avoid a Flyway checksum mismatch.
- SMTP credentials come from the env vars `EMAIL_USERNAME` / `EMAIL_PASSWORD` (Gmail SMTP). `email.subject` and `email.message` in `application.yml` are the fallback subject and message (`EmailDefaultsConfig`).
- `FileStorageService` creates `file.upload-dir` (`/Code/UploadDir`) in its constructor at startup. On Windows this resolves against the current drive root.
- Swagger UI is served at `/swagger-ui/index.html`. Seeded login: `leandro` / `admin123`.
- PDF export compiles the `.jrxml` templates at request time (once per template, cached in a `ConcurrentHashMap`), works from `java -jar` on the fat jar because `jasperreports-jdt` is on the classpath, and needs `src/main/resources/jasperreports.properties` (`net.sf.jasperreports.awt.ignore.missing.font=true`) because `people.jrxml` asks for `Arial`, which Linux does not have. Export failures reach the client as an empty `403`, not a `500`. The `books` sub-report reaches `person.jrxml` as the compiled `BOOK_SUB_REPORT` object.
- `Startup.generateHashedPassword()` prints PBKDF2 hashes for seeding users in migrations; nothing calls it, run it from a Groovy console or a scratch `main`. It uses `SecurityConfig.createPasswordEncoder()`, the same factory behind the `PasswordEncoder` bean that `AuthService` injects.

## Architecture

Standard layering: `controllers` -> `services` -> `repository` (Spring Data JPA) -> `model` entities, with these cross-cutting pieces:

- **Entity <-> DTO mapping**: services never expose entities; they convert with the static `ObjectMapper.parseObject(...)` (see the `metaClass` note above). DTOs extend HATEOAS `RepresentationModel`, and services add links in a private static `addHateoasLinks(dto)` using `linkTo(methodOn(XController).x(...))`. Adding or renaming a controller method means updating those links too. Paged results go through `PagedResourcesAssembler`.
- **OpenAPI docs live in interfaces**: each controller implements a `controllers/docs/*ControllerDocs` interface that carries the springdoc annotations.
- **Content negotiation**: JSON, XML and YAML (`WebConfig.configureContentNegotiation`, and the `@Api*` mappings above). Spring Boot configures only the JSON and XML mappers, so `config/JacksonConfig` builds the YAML converter and adds a mix-in that leaves out `EntityModel`'s own empty `links` (without it every YAML list item ends with a second `links: []`).
- **Import/export strategy factories**: `FileExporterFactory` picks `CsvExporter` / `XlsxExporter` / `PdfExporter` from the `Accept` header (case-insensitive, constants in `file/exporter/MediaTypes`); `FileImporterFactory` picks `CsvImporter` / `XlsxImporter` from the file extension. Both look the implementation up as a bean. To add a format, add a `@Component` and an entry in the factory map. `PersonController.exportPage` also maps the Accept type to the file extension.
- **E-mail**: `EmailController` -> `EmailService` -> `EmailSender`. `EmailService` builds an immutable `EmailMessage` (falling back to `email.subject`/`email.message` for blank fields) and `EmailSender.send(message)` is stateless, with `JavaMailSender` and `EmailConfig` injected, so it is safe for concurrent requests (`EmailSenderSpec` sends from 8 threads). The `emailRequest` part of `/withAttachment` is parsed with a Jackson 3 `JsonMapper` with `FAIL_ON_UNKNOWN_PROPERTIES`, so unknown fields are rejected.
- **Security** (`config/SecurityConfig`, `security/jwt/*`): stateless JWT (auth0 `java-jwt`), `JwtTokenFilter` before `UsernamePasswordAuthenticationFilter`. `/auth/signin`, `/auth/refresh/**`, `/auth/createUser`, `/swagger-ui/**` and `/v3/api-docs/**` are public; `/api/**` needs authentication; `/users` is denied. Passwords are PBKDF2. `JwtTokenFilter` never fails a request because of its token: an invalid one is ignored and the request goes on unauthenticated (empty `403` on `/api/**`). `/auth/refresh/**` verifies the token inside the controller, so an invalid one is answered as `403` with the `ExceptionResponse` body.
- **CORS** origins come from `cors.originPatterns` in `application.yml`, applied in `WebConfig`.
- **Error handling**: `exception/handler/CustomEntityResponseHandler` has one `@ExceptionHandler(Exception)` that takes the status from the `@ResponseStatus` on the exception class (`AnnotatedElementUtils.findMergedAnnotation`); an exception without it is a 500, and the body is always an `ExceptionResponse`. A new custom exception only needs its `@ResponseStatus`. It also sets `type: about:blank` on every `ProblemDetail`.

## Settings that must stay

- `src/test/resources/application.yml` is a full copy of the main one, not an overlay; change both.
- JSON, XML and YAML use the Jackson 3 defaults (alphabetical properties, `Z` dates, ISO YAML dates). `Book.launchDate` / `BookDTO.launchDate` are `LocalDate` (`yyyy-MM-dd`; offset date-times are rejected with 400).
- `spring.jpa.properties.hibernate.check_nullability: true` is required: springdoc 3 pulls in Bean Validation and Hibernate then turns its own not-null check off silently.
- `/v3/api-docs` is an OpenAPI 3.1 document; `springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled` are set to their default only to avoid a startup warning.
- Boot 4 split its auto-configuration into modules: keep `spring-boot-starter-flyway`, `spring-boot-starter-hateoas` and `spring-boot-starter-webmvc`.

## Docker and CI

- `Dockerfile` (`eclipse-temurin:25-jdk`) copies `build/libs/*.jar` (the plain jar is disabled, `jar { enabled = false }`, so there is exactly one). `.dockerignore` keeps only `build/libs/*.jar`, so run the Gradle build first.
- `docker-compose.yml`: `db` is `mysql:9` (host port 3308) with a healthcheck, and `app` waits for it. The image is `leandrocgsi/rest-with-spring-boot-groovy-erudio`.
- `.github/workflows/continuous-deployment.yml` runs on push to `main`: Docker Hub login, Java 25 (temurin) with the Gradle cache, `./gradlew clean build` (all tests, Docker needed), `docker compose build`/`up`, a wait on the Swagger URL, the Postman collection with `npx --yes newman@6.2.2`, logs on failure, `docker compose down -v`, and only then the image push (`latest` and the run id). It needs `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`. The `gradlew` file needs the executable bit in git (`git update-index --chmod=+x gradlew`).

## Postman

`Collections/` holds the collection, its environment (`SPRING_BOOT_GROOVY_ERUDIO`) and `files/`; `Collections/README.md` explains how to use and run them. It runs against this app with `npx newman run Collections/*.postman_collection.json -e Collections/SPRING_BOOT_GROOVY_ERUDIO.postman_environment.json --working-dir Collections`, both with `java -jar` and with `docker compose`. Keep it in sync with the API. Port 8080 is often taken on the developer's machine: publish another port and pass `--env-var baseUrl=http://localhost:<port>`.

## Notes

- The owner dislikes comments: do not add code comments (Groovy, YAML, Gradle, Dockerfile, ignore files); put explanations in this file or in the commit message.
- `spring-boot-devtools` is `developmentOnly`.
- `TestLogController` (`/api/test/v1`) is a demo endpoint for exercising log levels.

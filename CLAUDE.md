# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Kotlin/Spring Boot custody integration service for Fireblocks (crypto wallet infrastructure). MiCA-compliant euro stablecoin, regulated by Dutch Central Bank. Multi-module Gradle project with hexagonal architecture.

**Stack:** Kotlin 2.3.0, Spring Boot 4.0.6, JDK 25, Gradle Kotlin DSL, PostgreSQL, Kafka + Schema Registry, LocalStack (SecretsManager), Zitadel (OAuth2/OIDC).

## Working principles

- Avoid "not my department" thinking — if there are build failures you consider unrelated to current changes, still make an effort to fix them.
- Never add Claude (or any Anthropic identity) as a `Co-Authored-By` trailer on commit messages. Human co-authors are fine.

## Coding principles

- Correctness is top priority. Adhere to the "fail early" principle: validate inputs, check types, and throw on misuse rather than silently producing wrong results. Silent failures are never an option.
- When fixing bug reports, start with a failing test case. Then fix the bug and assert the test passes.
- Keep cyclomatic complexity low. Avoid fully-qualified class names in code — always add imports.
- Before writing new code, search for existing patterns in the same class/package (DRY). Extract repeated logic into helper methods rather than duplicating it.
- Be conservative with base class refactoring. Do not pull implementation details into abstract base classes unless the logic is truly identical across all subclasses with no foreseeable divergence. Shared helpers are better than shared template methods when subclasses may need different control flow.

## Build commands

```bash
./gradlew build                        # Full build: compile + all tests + ktlint + jacoco
./gradlew compileKotlin                # Compile only
./gradlew test                         # Unit tests + ArchUnit
./gradlew integrationTest              # Integration tests (needs Docker for Testcontainers)
./gradlew businessTest                 # End-to-end business tests (needs Docker)
./gradlew jacocoTestReport             # Generate coverage report (runs all test tasks first)
./gradlew jacocoTestCoverageVerification  # Enforce coverage gates (80% line, 70% branch)
./gradlew ktlintCheck                  # Lint check
./gradlew ktlintFormat                 # Auto-fix formatting
```

Run a single test class:
```bash
./gradlew test --tests "com.stablecoin.custody.fireblocks.SomeTest"
./gradlew integrationTest --tests "com.stablecoin.custody.fireblocks.SomeIntegrationTest"
```

## Module structure

| Module | Type | Purpose |
|--------|------|---------|
| `custody-fireblocks` | Spring Boot app | Main service: domain logic, REST API, DB, messaging |
| `custody-fireblocks-api` | java-library | Shared API contracts: request/response DTOs, event DTOs, validation |
| `custody-fireblocks-client` | java-library | REST client SDK for consumers of this service |

## Architecture rules (enforced by ArchUnit)

Hexagonal architecture under `com.stablecoin.custody.fireblocks`:

- **`domain/`** — Pure business logic, models, ports. MUST NOT import from `application` or `infrastructure`. No Spring annotations except `@Component`, `@Service`, `@Transactional` for DI/tx.
- **`application/`** — Inbound adapters (controllers, Kafka listeners, jobs). Depends on `domain`.
- **`infrastructure/`** — Outbound adapters (JPA, HTTP clients, Kafka publishers). Implements domain ports. Depends on `domain`.

Dependencies always point inward: `application` -> `domain` <- `infrastructure`.

## Test source sets

The `custody-fireblocks` module has four test source sets:

| Source set | Path | Purpose | Dependencies |
|------------|------|---------|-------------|
| `test` | `src/test/` | Unit tests (MockK, ArchUnit) | main + testFixtures |
| `testFixtures` | `src/testFixtures/` | Shared fixtures, stubs, base classes | main |
| `integrationTest` | `src/integrationTest/` | Testcontainers integration tests | main + testFixtures |
| `businessTest` | `src/businessTest/` | E2E business flow tests | main + testFixtures + client module |

## Testing conventions

- **MockK only** — Mockito is excluded from classpath. Use `every { }`, `verify { }`, `just runs`.
- **Single recursive comparison** — NEVER use multiple `assertThat` on individual fields. Build an expected object and use `assertThat(result).usingRecursiveComparison().ignoringFields(...).isEqualTo(expected)`.
- **Test structure** — Use `// given`, `// when`, `// then` comment markers.
- **Test naming** — Kotlin backtick names: `` fun `should create vault when valid command`() ``
- **Fixtures** — Place in `src/testFixtures/kotlin/.../test/fixtures/`. Use top-level functions like `aVault()`, `aTransaction()` returning domain objects with sensible defaults.
- **Integration test base** — Extend `AbstractIntegrationTest` which provides singleton Testcontainers (PostgreSQL, Kafka, Schema Registry, LocalStack).

## Kotlin conventions

- **No comments or Javadoc** — code must be self-explanatory through clear naming, small functions, and obvious structure. The only acceptable comments are `// given`, `// when`, `// then` markers in tests.
- Prefer `val` over `var` — immutability by default. Use `var` only when mutation is genuinely needed.
- Let the compiler infer types — only add explicit type annotations when the type isn't obvious from the right-hand side.
- Constructor injection via primary constructor (no `@Autowired`)
- `data class` for domain models with `copy()` for mutations
- `fun interface` (SAM) for single-method domain ports
- Return nullable types (`Vault?`) not `Optional`
- Logger as top-level `private val`: `private val log = logger<MyService>()`
- Extension functions for object mapping (no MapStruct)

## Local development

```bash
docker compose up -d    # PostgreSQL, Kafka, Schema Registry, LocalStack, Zitadel
./gradlew bootRun       # Run the app (port 8080, actuator on 8082)
```

## Key documentation

- `docs/KOTLIN_CODING_STANDARDS.md` — Full coding rules (architecture, domain models, testing, API design)
- `docs/TESTING_STANDARDS.md` — Test patterns (golden rule, fixtures, mocking, coverage)
- `docs/PROJECT_STRUCTURE.md` — Physical layout and file placement decision tree
- `docs/ADR.md` — Architecture decision records
- `docs/SPEC.md` — Full service specification

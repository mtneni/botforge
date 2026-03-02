# Bot Packages

This directory contains bot-specific implementations under `org.legendstack.bot.<botname>`.

Bot packages are **not** scanned by default. You must configure `botforge.bot-packages=org.legendstack.bot.<botname>` in your profile properties.

## Extension Mechanism

Each bot profile can customize BotForge along these axes:

1. **Properties** — override any `botforge.*` property (persona, objective, LLM, etc.)
2. **Templates** — provide custom Jinja persona, objective, and behaviour templates
3. **Domain Model** — define `NamedEntity` interfaces for typed entity extraction
4. **Relations** — define how entities connect in the knowledge graph
5. **Tools** — provide `@LlmTool`, `ToolishRag`, or `Subagent` beans

> **📖 For full documentation, see [`docs/EXTENDING.md`](../../../docs/EXTENDING.md) and [`docs/PERSONA_STUDIO.md`](../../../docs/PERSONA_STUDIO.md).**

## Current Profiles

### Architect

The `architect` profile is a reference implementation for enterprise software architecture guidance.

| Component | Purpose |
|-----------|---------|
| `ArchitectConfiguration` | Relations bean + DesignDocumentationTool |
| `SystemComponent` | Microservices, modules, packages |
| `ApiEndpoint` | REST endpoints, GraphQL, gRPC |
| `DataStore` | Databases, caches, message queues |
| `AIAgent` | LLM-powered agents |
| `LLMModel` | Specific LLM models used |
| `DeploymentTarget` | Cloud, on-prem, edge targets |

**Activate:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=architect
```

## Quick Start — New Bot

```bash
# 1. Create package
mkdir -p src/main/java/org/legendstack/bot/yourbot/domain

# 2. Add @Configuration + @Profile("yourbot")
# 3. Add NamedEntity interfaces in domain/
# 4. Add application-yourbot.properties
# 5. Add prompts/personas/yourbot.jinja + objectives/yourbot.jinja
# 6. Run:
mvn spring-boot:run -Dspring-boot.run.profiles=yourbot
```

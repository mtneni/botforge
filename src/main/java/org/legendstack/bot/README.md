# Bot Packages

This directory contains bot-specific implementations under `com.embabel.bot.<botname>`.

Bot packages are **not** scanned by default. You must configure `botforge.bot-packages=com.embabel.bot

## Extension Mechanism

Each bot profile can customize BotForge along three axes:

1. **Properties** — override any `botforge.*` property (persona, objective, LLM, etc.)
2. **Templates** — provide custom Jinja persona, objective, and behaviour templates
3. **Beans** — define tools, `LlmReference`s, or any Spring beans

### 1. Profile-Gated Configuration

Each bot **must** gate its `@Configuration` class with a Spring `@Profile` so that
its beans are only loaded when that profile is active:

```java
package com.embabel.bot.architect;

@Configuration
@Profile("architect")
public class ArchitectConfiguration {
    // Define tools, LlmReferences, or any beans
}
```

Activate via `spring.profiles.active`:

```yaml
spring:
  profiles:
    active: architect
```

Or multiple: `spring.profiles.active=architect,otherbot`

### 2. Property Overrides

Create `application-<profile>.properties` (or `.yml`) in `src/main/resources` to
override any `botforge.*` property when the profile is active. Spring Boot merges
profile-specific properties on top of the base `application.yml`.

Example `application-architect.properties`:

```properties
botforge.persona=architect
botforge.objective=architect
botforge.max-words=50
botforge.chat-llm.temperature=1.38
```

This changes the persona template, objective template, response length, and LLM
temperature — all without touching Java code.

Key `botforge.*` properties:

| Property | Description | Default |
|----------|-------------|---------|
| `botforge.persona` | Persona template name (resolves to `prompts/personas/<name>.jinja`) | `assistant` |
| `botforge.objective` | Objective template name (resolves to `prompts/objectives/<name>.jinja`) | `qa` |
| `botforge.behaviour` | Behaviour template name (resolves to `prompts/behaviours/<name>.jinja`) | `default` |
| `botforge.max-words` | Soft word limit for responses | `80` |
| `botforge.chat-llm.model` | LLM model ID | `gpt-4.1-mini` |
| `botforge.chat-llm.temperature` | LLM temperature | `0.0` |
| `botforge.memory.enabled` | Enable memory/proposition extraction | `true` |
| `botforge.bot-packages` | Packages to scan for bot components | _(none)_ |

### 3. Custom Templates

Add Jinja templates to `src/main/resources/prompts/` to define the bot's voice
and goals. The template names must match the property values.

For a bot named "architect" with `botforge.persona=architect` and `botforge.objective=architect`:

```
src/main/resources/
  prompts/
    personas/architect.jinja      # "Your voice" — personality, tone, style
    objectives/architect.jinja    # "Your objectives" — what the bot should do
    behaviours/architect.jinja    # "Your behaviour" — optional, use "default" to skip
```

Templates have access to `properties` (BotForgeProperties) and `user` (BotForgeUser)
via the Jinja context. They can also include shared elements:

```jinja
You are Architect, a warm and insightful astrologer.

{% include "elements/thorough_memory" %}
```

### 4. Domain Objects

Bots can define domain-specific `NamedEntity` subclasses in their package.
These are automatically picked up for proposition extraction and memory
when the bot package is listed in `botforge.memory.entity-packages`:

```properties
botforge.memory.entity-packages=com.embabel.bot.architect
```

Any `NamedEntity` classes found in that package are added to the data dictionary,
so DICE can extract, store, and resolve entities of those types from conversation.

### 5. Tools and References

Define any `Tool` or `LlmReference` beans in your profile configuration.
These are picked up by `ChatActions` when wired into the prompt runner.

```java
@Configuration
@Profile("architect")
public class ArchitectConfiguration {

    @Bean
    public AstrologyTools astrologyTools() {
        return new AstrologyTools();
    }
}
```

`LlmReference` beans are particularly powerful — they inject content into the
system prompt (via `contribution()`) and expose tools (via `tools()`). For example,
DICE's `Memory` class is an `LlmReference` that surfaces key memories in the prompt
and provides a search tool for additional retrieval.

## Complete Example: Architect

**File structure:**

```
src/main/
  java/com/embabel/bot/architect/
    ArchitectConfiguration.java
  resources/
    application-architect.properties
    prompts/
      personas/architect.jinja
      objectives/architect.jinja
```

**`ArchitectConfiguration.java`:**

```java
package com.embabel.bot.architect;

import architect.bot.org.legendstack.AstrologyTools;

@Configuration
@Profile("architect")
public class ArchitectConfiguration {

    @Bean
    public AstrologyTools astrologyTools() {
        return new AstrologyTools();
    }
}
```

**`application-architect.properties`:**

```properties
botforge.bot-packages=com.embabel.bot
botforge.persona=architect
botforge.objective=architect
botforge.max-words=50
botforge.chat-llm.temperature=1.38
```

**`prompts/personas/architect.jinja`:**

```jinja
You are Architect, a warm and insightful astrologer.
You speak with gentle authority about celestial matters.

{% include "dice/thorough_memory" %}
```

**`prompts/objectives/architect.jinja`:**

```jinja
Help the user explore astrological insights about themselves
and the world around them. Use their birth details from memory
to provide personalized readings.
```

**Activate:**

```yaml
spring:
  profiles:
    active: architect
```

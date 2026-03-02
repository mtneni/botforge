# Extending BotForge: Persona Studio & Custom Bots

BotForge is designed to be an extensible platform where you can forge specialized AI agents (personas) without touching the core infrastructure. This guide explains how to extend the system along several axes.

## 1. Persona Studio (No-Code Extension)

The **Persona Studio** (accessible via the Sidebar) is the easiest way to create new bot identities.

### Forging a New Persona
1. **Name & Identity**: Give your bot a display name and a unique ID.
2. **Objective**: Define the primary goal (e.g., "Review software architectures for scalability").
3. **Description**: Set the tone and persona (e.g., "You are a senior systems architect with 20 years of experience in distributed systems").
4. **Base Template**: Map your persona to a Jinja2 template in `src/main/resources/prompts/personas/`.

### Deployment
Once saved, the persona is persisted in the PostgreSQL database and appears in the sidebar for all users (or remains private if multi-tenancy is active).

---

## 2. Code-Based Extensions (The Architect Pattern)

For more complex bots that require custom logic, tools, or domain knowledge (like the default `Architect` bot), you should use the **Profile-based Extension Model**.

### Step 1: Create a Bot Package
Add a new package under `org.legendstack.bot.<yourbot>/`. All your custom code should live here.

### Step 2: Define Domain Interfaces
If you want the bot to have specialized "memory" (entity extraction), define Java interfaces extending `NamedEntity`.

```java
public interface SecurityAudit extends NamedEntity {
    @JsonPropertyDescription("The severity level of the audit finding")
    String getSeverity();
}
```

### Step 3: Implement Custom Tools
Create tools and annotate them with `@Service` and `@UnfoldingTools` to expose them to the LLM.

```java
@Service
@UnfoldingTools(name = "securityTools", description = "Tools for scanning security vulnerabilities")
public class SecurityScanTool {
    @LlmTool
    public String scanCode(String gitUrl) {
         // Logic to scan code
         return "Scan results...";
    }
}
```

### Step 4: Wire with high-performance Relations
Define how your domain entities relate to each other in a `@Configuration` class:

```java
@Bean
Relations securityRelations() {
    return Relations.empty()
            .withSemanticBetween("User", "SecurityAudit", "triggered", "user triggered an audit");
}
```

### Step 5: Activate via Profile
Create `application-<profile>.properties`:
```properties
botforge.chat.persona=<yourpersona>
botforge.bot-packages=org.legendstack.bot.<yourbot>
botforge.stylesheet=modern-dark
```

---

## 3. Power Tips for Persona Designers

*   **Matryoshka Tools**: Design tools that return summaries of large data, allowing the LLM to drill down only when needed.
*   **Scoped RAG**: Use `ToolishRag` beans to limit the document search space for specific personas.
*   **Hybrid Search**: Leverage the `HybridSearchService` in your custom tools to combine semantic and keyword search for high-precision retrieval.
*   **Analytics Integration**: Use the `AnalyticsService` to track how your custom persona is being used and what tools are most effective.

---

## 4. Summary of Extension Axes

| Axis | Mechanism |
|---|---|
| **Tone/Voice** | Jinja2 Persona Templates |
| **Logic** | Spring `@Service` + `@LlmTool` |
| **Data Scope** | `ToolishRag` Beans |
| **Memory Schema** | `NamedEntity` Interfaces |
| **UI Branding** | Custom CSS Themes |

For more details, see the core [README.md](../README.md#extensibility).

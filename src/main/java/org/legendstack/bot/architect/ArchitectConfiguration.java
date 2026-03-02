package org.legendstack.bot.architect;

import com.embabel.dice.common.Relations;
import org.legendstack.basebot.tools.ArchitectureReviewTool;
import org.legendstack.basebot.tools.DesignDocumentationTool;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for the Architect bot.
 * Enterprise-grade system design and technical governance assistant.
 *
 * <p>Provides rich domain relations across 12 entity types covering
 * components, APIs, data stores, security, quality, cloud services,
 * teams, integration patterns, deployment targets, and AI agents.</p>
 */
@Configuration
@Profile("architect")
public class ArchitectConfiguration {

    @Bean
    public DesignDocumentationTool architectureDesignTools(BotForgeUserService userService) {
        return new DesignDocumentationTool(userService);
    }

    @Bean
    public ArchitectureReviewTool architectureReviewTools(BotForgeUserService userService) {
        return new ArchitectureReviewTool(userService);
    }

    @Bean
    public Relations architecturalRelations() {
        return Relations.empty()
                // ── Component ↔ Component ──
                .withSemanticBetween("SystemComponent", "SystemComponent", "calls",
                        "component calls another component")
                .withSemanticBetween("SystemComponent", "SystemComponent", "depends_on",
                        "component depends on another component")

                // ── Component ↔ API ──
                .withSemanticBetween("SystemComponent", "ApiEndpoint", "exposes",
                        "component exposes an API endpoint")
                .withSemanticBetween("SystemComponent", "ApiEndpoint", "consumes",
                        "component consumes an API endpoint")

                // ── Component ↔ DataStore ──
                .withSemanticBetween("SystemComponent", "DataStore", "reads_from",
                        "component reads from a data store")
                .withSemanticBetween("SystemComponent", "DataStore", "writes_to",
                        "component writes to a data store")

                // ── Component ↔ Security ──
                .withSemanticBetween("SystemComponent", "SecurityRequirement", "requires",
                        "component requires a security control or compliance standard")
                .withSemanticBetween("ApiEndpoint", "SecurityRequirement", "protected_by",
                        "API endpoint is protected by a security requirement")

                // ── Component ↔ Quality ──
                .withSemanticBetween("SystemComponent", "QualityAttribute", "targets",
                        "component targets a quality attribute or SLA")
                .withSemanticBetween("ApiEndpoint", "QualityAttribute", "constrained_by",
                        "API endpoint is constrained by a quality attribute")

                // ── Component ↔ Cloud ──
                .withSemanticBetween("SystemComponent", "CloudService", "deployed_on",
                        "component is deployed on a cloud service")
                .withSemanticBetween("DataStore", "CloudService", "hosted_on",
                        "data store is hosted on a cloud service")
                .withSemanticBetween("CloudService", "CloudService", "integrates_with",
                        "cloud service integrates with another cloud service")

                // ── Component ↔ Deployment ──
                .withSemanticBetween("SystemComponent", "DeploymentTarget", "deployed_to",
                        "component is deployed to a target environment")
                .withSemanticBetween("DataStore", "DeploymentTarget", "provisioned_in",
                        "data store is provisioned in a deployment target")

                // ── Team ↔ Ownership ──
                .withSemanticBetween("Team", "SystemComponent", "owns",
                        "team owns and maintains a system component")
                .withSemanticBetween("Team", "DataStore", "maintains",
                        "team maintains a data store")
                .withSemanticBetween("Team", "ApiEndpoint", "responsible_for",
                        "team is responsible for an API endpoint")
                .withSemanticBetween("Team", "Team", "collaborates_with",
                        "team collaborates with another team")

                // ── Component ↔ Integration ──
                .withSemanticBetween("SystemComponent", "IntegrationPattern", "uses_pattern",
                        "component uses an integration pattern")
                .withSemanticBetween("ApiEndpoint", "IntegrationPattern", "implemented_with",
                        "API endpoint is implemented with an integration pattern")

                // ── AI Agent relationships ──
                .withSemanticBetween("AIAgent", "MCPTool", "uses_tool",
                        "agent uses an MCP tool")
                .withSemanticBetween("AIAgent", "LLMModel", "calls_model",
                        "agent calls a specific LLM model")
                .withSemanticBetween("AIAgent", "SystemComponent", "integrates_with",
                        "agent integrates with a system component")
                .withSemanticBetween("AIAgent", "AIAgent", "delegates_to",
                        "agent delegates a sub-task to another agent")
                .withSemanticBetween("AIAgent", "DataStore", "indexed_in",
                        "agent's knowledge is indexed in a vector store")
                .withSemanticBetween("AIAgent", "CloudService", "hosted_on",
                        "agent is hosted on a cloud service")

                // ── Cross-cutting ──
                .withSemanticBetween("SecurityRequirement", "QualityAttribute", "enables",
                        "security requirement enables a quality attribute")
                .withSemanticBetween("DeploymentTarget", "CloudService", "runs_on",
                        "deployment target runs on a cloud service")
                .withSemanticBetween("IntegrationPattern", "QualityAttribute", "supports",
                        "integration pattern supports a quality attribute");
    }
}

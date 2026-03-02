package org.legendstack.basebot.tools;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
import org.legendstack.basebot.user.BotForgeUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tool for the Architect Assistant to publish design documentation,
 * such as ADRs and Mermaid diagrams.
 */
@UnfoldingTools(name = "architectureDesignTools", description = "Tools for publishing architectural designs and ADRs")
@Service
public class DesignDocumentationTool {

    private static final Logger logger = LoggerFactory.getLogger(DesignDocumentationTool.class);
    private static final String DESIGNS_DIR = "workspace/designs";

    private final BotForgeUserService userService;

    public DesignDocumentationTool(BotForgeUserService userService) {
        this.userService = userService;
    }

    @LlmTool(description = """
            Publish an architectural design document, ADR, or Mermaid diagram to the workspace.
            Use this when you have finalized a design or ADR and want to save it for the developer.
            The title should be descriptive, and the content should be in Markdown format.
            """)
    public String publishDesignDoc(String docType, String title, String content) {
        try {
            var user = userService.getAuthenticatedUser();
            Path dirPath = Paths.get(DESIGNS_DIR, user.getTeamId(), docType.toLowerCase());
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String fileName = String.format("%s_%s.md", timestamp, sanitizedTitle);

            Path filePath = dirPath.resolve(fileName);
            Files.writeString(filePath, content);

            logger.info("Published design document for team {} to: {}", user.getTeamId(), filePath);
            return "Successfully published " + docType + " to: " + filePath.toAbsolutePath();
        } catch (IOException e) {
            logger.error("Failed to publish design doc", e);
            return "Error publishing document: " + e.getMessage();
        }
    }

    @LlmTool(description = """
            List all previously published design documents, ADRs, and diagrams for the current user's team.
            Use this to reference earlier work or check what architecture artifacts exist.
            """)
    public String listPublishedDocs() {
        try {
            var user = userService.getAuthenticatedUser();
            Path teamDir = Paths.get(DESIGNS_DIR, user.getTeamId());
            if (!Files.exists(teamDir)) {
                return "No design documents have been published yet for your team.";
            }

            StringBuilder sb = new StringBuilder("## Published Design Documents\n\n");
            try (var walk = Files.walk(teamDir, 3)) {
                var files = walk.filter(Files::isRegularFile)
                        .sorted()
                        .toList();
                if (files.isEmpty()) {
                    return "No design documents have been published yet for your team.";
                }
                for (Path file : files) {
                    String relative = teamDir.relativize(file).toString().replace('\\', '/');
                    long size = Files.size(file);
                    sb.append(String.format("- `%s` (%d bytes)\n", relative, size));
                }
            }
            return sb.toString();
        } catch (IOException e) {
            logger.error("Failed to list published docs", e);
            return "Error listing documents: " + e.getMessage();
        }
    }
}

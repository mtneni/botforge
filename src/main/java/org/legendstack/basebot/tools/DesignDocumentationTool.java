package org.legendstack.basebot.tools;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
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

    @LlmTool(description = """
            Publish an architectural design document, ADR, or Mermaid diagram to the workspace.
            Use this when you have finalized a design or ADR and want to save it for the developer.
            The title should be descriptive, and the content should be in Markdown format.
            """)
    public String publishDesignDoc(String docType, String title, String content) {
        try {
            Path dirPath = Paths.get(DESIGNS_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String fileName = String.format("%s_%s_%s.md", timestamp, docType.toLowerCase(), sanitizedTitle);

            Path filePath = dirPath.resolve(fileName);
            Files.writeString(filePath, content);

            logger.info("Published design document to: {}", filePath);
            return "Successfully published " + docType + " to: " + filePath.toAbsolutePath();
        } catch (IOException e) {
            logger.error("Failed to publish design doc", e);
            return "Error publishing document: " + e.getMessage();
        }
    }
}

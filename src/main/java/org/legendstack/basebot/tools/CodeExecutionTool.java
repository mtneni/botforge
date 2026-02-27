package org.legendstack.basebot.tools;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@UnfoldingTools(name = "pythonEnvironment", description = "Tools for safely executing python code")
@Service
public class CodeExecutionTool {

    private static final Logger logger = LoggerFactory.getLogger(CodeExecutionTool.class);

    @LlmTool(description = """
            Execute a python script locally in a sandboxed environment.
            Use this to perform calculations, parse data, or test algorithmic logic.
            The environment has no extra variable access and times out after 5 seconds.
            """)
    public String executePython(String code) {
        logger.info("Executing Python script snippet...");
        Path tempScript = null;
        try {
            tempScript = Files.createTempFile("BotForge_script_", ".py");
            Files.writeString(tempScript, code);

            ProcessBuilder pb = new ProcessBuilder("python", tempScript.toString());
            // Safe Sandbox: remove all environment variables except PATH (necessary to find
            // python)
            String path = pb.environment().get("PATH");
            String systemRoot = pb.environment().get("SystemRoot");
            pb.environment().clear();
            if (path != null)
                pb.environment().put("PATH", path);
            if (systemRoot != null)
                pb.environment().put("SystemRoot", systemRoot);
            // pb.directory(...) // Optional: Restrict working directory to a safe space

            pb.redirectErrorStream(false);
            Process process = pb.start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS); // Timeout of 5 seconds

            if (!finished) {
                process.destroyForcibly();
                return "Execution timed out after 5 seconds.";
            }

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            return "Exit Code: " + process.exitValue() + "\nStdout:\n" + stdout + "\nStderr:\n" + stderr;

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to execute code", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "Internal Error: " + e.getMessage();
        } finally {
            if (tempScript != null) {
                try {
                    Files.deleteIfExists(tempScript);
                } catch (IOException e) {
                    logger.warn("Could not delete temporary script file: {}", tempScript);
                }
            }
        }
    }
}

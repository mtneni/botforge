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
    private static final int MAX_OUTPUT_BYTES = 10_240; // 10 KB cap
    private static final int TIMEOUT_SECONDS = 5;

    @LlmTool(description = """
            Execute a python script locally in a sandboxed environment.
            Use this to perform calculations, parse data, or test algorithmic logic.
            The environment has no extra variable access and times out after 5 seconds.
            """)
    public String executePython(String code) {
        logger.info("Executing Python script snippet...");
        Path tempDir = null;
        Path tempScript = null;
        try {
            // Create an isolated temp working directory so the script cannot see the host
            // FS
            tempDir = Files.createTempDirectory("BotForge_sandbox_");
            tempScript = Files.createTempFile(tempDir, "BotForge_script_", ".py");
            Files.writeString(tempScript, code);

            // Execute inside a sandboxed Docker container
            // --rm: remove container when done
            // --network none: disable internet access
            // -m 100m: limit memory
            // -v: mount the script directory
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--network", "none",
                    "-m", "100m",
                    "--cpus", "0.5",
                    "-v", tempDir.toAbsolutePath() + ":/app",
                    "-w", "/app",
                    "python:3.11-slim",
                    "python", "-I", tempScript.getFileName().toString());

            pb.redirectErrorStream(false);

            Process process = pb.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS + 5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "Execution timed out after " + TIMEOUT_SECONDS + " seconds.";
            }

            String stdout = readCapped(process.getInputStream());
            String stderr = readCapped(process.getErrorStream());

            return "Exit Code: " + process.exitValue() + "\nStdout:\n" + stdout + "\nStderr:\n" + stderr;

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to execute code", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "Internal Error: " + e.getMessage();
        } finally {
            cleanUp(tempScript);
            cleanUp(tempDir);
        }
    }

    private String readCapped(java.io.InputStream is) throws IOException {
        byte[] buf = is.readNBytes(MAX_OUTPUT_BYTES);
        String result = new String(buf);
        if (buf.length == MAX_OUTPUT_BYTES) {
            result += "\n... [output truncated at " + MAX_OUTPUT_BYTES + " bytes]";
        }
        return result;
    }

    private void cleanUp(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                logger.warn("Could not delete temporary path: {}", path);
            }
        }
    }
}

package org.legendstack.basebot.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodeExecutionToolTest {

    private final CodeExecutionTool tool = new CodeExecutionTool();

    @Test
    public void testBasicPythonExecution() {
        String code = "print('hello world')";
        String result = tool.executePython(code);

        assertTrue(result.contains("hello world"), "Result should contain hello world");
        assertTrue(result.contains("Exit Code: 0"), "Exit code should be 0");
    }

    @Test
    public void testPythonCalculation() {
        String code = "print(10 + 20)";
        String result = tool.executePython(code);

        assertTrue(result.contains("30"), "Result should contain 30");
    }

    @Test
    public void testPythonTimeout() {
        // This script runs for 10 seconds, but tool has 5s timeout
        String code = "import time; time.sleep(10)";
        String result = tool.executePython(code);

        assertTrue(result.contains("timed out"), "Result should indicate timeout");
    }

    @Test
    public void testPythonError() {
        String code = "raise Exception('test error')";
        String result = tool.executePython(code);

        assertTrue(result.contains("Exception: test error"), "Result should contain error message");
    }
}

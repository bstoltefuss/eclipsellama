package com.eclipsellama.plugin.git;

import com.eclipsellama.plugin.core.OllamaClient;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Generates commit messages from git diff using AI.
 */
public class CommitMessageGenerator {

    private static final String COMMIT_PROMPT = "Generate a concise git commit message for these changes. " +
            "Use conventional commit format (type: description). " +
            "Types: feat, fix, docs, style, refactor, test, chore. " +
            "Keep it under 72 characters. Only output the commit message, nothing else.\n\n" +
            "Changes:\n%s";

    /**
     * Generate a commit message from the diff.
     */
    public static String generate(String diff) {
        if (diff == null || diff.trim().isEmpty()) {
            return "chore: update files";
        }

        // Truncate very long diffs
        String truncatedDiff = diff.length() > 4000
                ? diff.substring(0, 4000) + "\n... (truncated)"
                : diff;

        String prompt = String.format(COMMIT_PROMPT, truncatedDiff);

        try {
            String model = EclipseLlamaPreferences.getModel();
            String result = OllamaClient.generate(prompt, model);

            // Clean up the response - take first line only
            result = result.trim();
            int newline = result.indexOf('\n');
            if (newline > 0) {
                result = result.substring(0, newline).trim();
            }

            return result.isEmpty() ? "chore: update files" : result;

        } catch (Exception e) {
            System.err.println("EclipseLlama: Failed to generate commit message: " + e.getMessage());
            return "chore: update files";
        }
    }

    /**
     * Generate async and call the callback with result.
     */
    public static void generateAsync(String diff, java.util.function.Consumer<String> callback) {
        new Thread(() -> {
            String message = generate(diff);
            callback.accept(message);
        }, "EclipseLlama-CommitGen").start();
    }
}

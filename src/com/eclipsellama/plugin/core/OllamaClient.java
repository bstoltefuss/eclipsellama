package com.eclipsellama.plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;

import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * HTTP client for Ollama API.
 * Provides both blocking and async methods.
 */
public class OllamaClient {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 120000;

    private OllamaClient() {
        // Utility class
    }

    /**
     * Get the base endpoint URL.
     */
    private static String getEndpoint() {
        return EclipseLlamaPreferences.getOllamaEndpoint();
    }

    /**
     * Test if Ollama server is reachable.
     */
    public static boolean isServerReachable() {
        try {
            URL url = new URL(getEndpoint() + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try {
                conn.connect();
                return conn.getResponseCode() == 200;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetch available models from Ollama.
     */
    public static String[] getAvailableModels() {
        try {
            URL url = new URL(getEndpoint() + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            try {
                conn.connect();
                if (conn.getResponseCode() != 200) {
                    return new String[0];
                }
                String response = readResponse(conn.getInputStream());
                return JsonHelper.parseModelList(response);
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Blocking query using /api/generate endpoint.
     */
    public static String generate(String prompt, String model) {
        try {
            String payload = JsonHelper.buildGenerateRequest(model, prompt, false);
            String response = sendPost(getEndpoint() + "/api/generate", payload);
            return JsonHelper.parseGenerateResponse(response);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Async streaming chat using /api/chat endpoint.
     * 
     * @param messages   List of chat messages
     * @param model      Model name
     * @param onChunk    Called for each text chunk received
     * @param onComplete Called when streaming is done with full response
     * @param onError    Called on error
     */
    public static void streamChat(
            java.util.List<ChatMessage> messages,
            String model,
            Consumer<String> onChunk,
            Consumer<String> onComplete,
            Consumer<String> onError) {

        CompletableFuture.runAsync(() -> {
            StringBuilder fullResponse = new StringBuilder();
            HttpURLConnection conn = null;

            try {
                // Build messages array
                JSONArray jsonMessages = new JSONArray();
                for (ChatMessage msg : messages) {
                    jsonMessages.put(JsonHelper.buildChatMessage(msg.getRole(), msg.getContent()));
                }

                String payload = JsonHelper.buildChatRequest(model, jsonMessages, true);

                URL url = new URL(getEndpoint() + "/api/chat");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                InputStream stream = (conn.getResponseCode() >= 400)
                        ? conn.getErrorStream()
                        : conn.getInputStream();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String chunk = JsonHelper.parseChatChunk(line);
                        if (!chunk.isEmpty()) {
                            fullResponse.append(chunk);
                            // Update UI on SWT thread
                            final String c = chunk;
                            Display.getDefault().asyncExec(() -> onChunk.accept(c));
                        }
                    }
                }

                final String result = fullResponse.toString();
                Display.getDefault().asyncExec(() -> onComplete.accept(result));

            } catch (Exception e) {
                final String error = e.getMessage();
                Display.getDefault().asyncExec(() -> onError.accept(error));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Async completion for code.
     */
    public static CompletableFuture<String> generateAsync(String prompt, String model) {
        return CompletableFuture.supplyAsync(() -> generate(prompt, model));
    }

    /**
     * Send POST request and return response body.
     */
    private static String sendPost(String endpoint, String payload) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setDoOutput(true);

        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            InputStream stream = (conn.getResponseCode() >= 400)
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            return readResponse(stream);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Read full response from input stream.
     */
    private static String readResponse(InputStream stream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                response.append(buffer, 0, len);
            }
        }
        return response.toString();
    }
}

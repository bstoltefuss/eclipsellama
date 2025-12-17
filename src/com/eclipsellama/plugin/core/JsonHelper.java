package com.eclipsellama.plugin.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Centralized JSON utilities for EclipseLlama.
 * Uses org.json library for reliable parsing.
 */
public final class JsonHelper {

    private JsonHelper() {
        // Utility class
    }

    /**
     * Escape a string for safe JSON inclusion.
     */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Parse the "response" field from Ollama /api/generate response.
     */
    public static String parseGenerateResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("response", "");
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Parse the "content" field from Ollama /api/chat streaming response.
     */
    public static String parseChatChunk(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject message = obj.optJSONObject("message");
            if (message != null) {
                return message.optString("content", "");
            }
            return "";
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Check if the streaming response indicates completion.
     */
    public static boolean isDone(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optBoolean("done", false);
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Parse model names from /api/tags response.
     */
    public static String[] parseModelList(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray models = obj.optJSONArray("models");
            if (models == null) {
                return new String[0];
            }
            String[] result = new String[models.length()];
            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.getJSONObject(i);
                result[i] = model.optString("name", "unknown");
            }
            return result;
        } catch (JSONException e) {
            return new String[0];
        }
    }

    /**
     * Build a chat message JSON object.
     */
    public static JSONObject buildChatMessage(String role, String content) {
        JSONObject msg = new JSONObject();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    /**
     * Build the full chat request payload.
     */
    public static String buildChatRequest(String model, JSONArray messages, boolean stream) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("messages", messages);
        request.put("stream", stream);
        return request.toString();
    }

    /**
     * Build a generate request payload.
     */
    public static String buildGenerateRequest(String model, String prompt, boolean stream) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", stream);
        return request.toString();
    }
}

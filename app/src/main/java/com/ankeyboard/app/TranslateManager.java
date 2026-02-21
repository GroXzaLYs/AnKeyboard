/*
 * AnKeyboard - A smart learning keyboard for Android
 * Copyright (C) 2026 AnerysRynz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ankeyboard.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Simple Google Translate wrapper using free translation API
 * Note: This uses a free API alternative, for production use official Google Cloud Translation API
 */
public class TranslateManager {
    private static final String MYMEMORY_API = "https://api.mymemory.translated.net/get";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();
    
    /**
     * Translate text to target language
     * @param text Text to translate
     * @param targetLang Target language code (en, id, es, fr, de, zh-CN, ja)
     * @return Translated text
     */
    public static String translate(String text, String targetLang) {
        try {
            // Map common language codes
            String langCode = mapLanguageCode(targetLang);
            
            String url = MYMEMORY_API + "?q=" + 
                    urlEncode(text) + 
                    "&langpair=auto|" + langCode;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "AnKeyboard/1.0")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonObject.has("responseData")) {
                        JsonObject responseData = jsonObject.getAsJsonObject("responseData");
                        if (responseData.has("translatedText")) {
                            return responseData.get("translatedText").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Map language names to language codes
     */
    private static String mapLanguageCode(String language) {
        switch(language.toLowerCase()) {
            case "indonesian":
            case "bahasa indonesia":
                return "id";
            case "english":
            case "inggris":
                return "en";
            case "spanish":
            case "spanyol":
                return "es";
            case "french":
            case "prancis":
                return "fr";
            case "german":
            case "jerman":
                return "de";
            case "chinese":
            case "mandarin":
            case "cina":
                return "zh-CN";
            case "japanese":
            case "jepang":
                return "ja";
            case "portuguese":
            case "portugis":
                return "pt";
            case "russian":
            case "rusia":
                return "ru";
            case "korean":
            case "korea":
                return "ko";
            default:
                return "en";
        }
    }
    
    /**
     * Simple URL encoder (replacement for URLEncoder for null safety)
     */
    private static String urlEncode(String text) {
        if (text == null) return "";
        return text.replaceAll(" ", "%20")
                .replaceAll("\\n", "%0A")
                .replaceAll("\\r", "%0D");
    }
}

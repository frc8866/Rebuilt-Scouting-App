package com.example.scoutingapp.data.supabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Minimal synchronous REST client for Supabase's PostgREST HTTP API.
 * Replaces the Kotlin-only Supabase SDK (`.from(table).select()/insert()/...`).
 * Every method is a blocking network call — always invoke from a background thread.
 */
public final class PostgrestClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = SupabaseClientProvider.okHttpClient;
    private final String baseUrl = SupabaseClientProvider.SUPABASE_URL + "/rest/v1/";
    private final String apiKey = SupabaseClientProvider.SUPABASE_ANON_KEY;

    /** GET {table}?select=... with optional query-string filters, e.g. "eq.RED_1" values. */
    public JSONArray select(String table, String columns, Map<String, String> filters) throws IOException {
        StringBuilder url = new StringBuilder(baseUrl).append(table).append("?select=")
                .append(columns == null ? "*" : columns);
        if (filters != null) {
            for (Map.Entry<String, String> e : filters.entrySet()) {
                url.append('&').append(e.getKey()).append('=').append(e.getValue());
            }
        }
        Request request = new Request.Builder()
                .url(url.toString())
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) {
                throw new IOException("Supabase select failed (" + response.code() + "): " + body);
            }
            return new JSONArray(body);
        } catch (org.json.JSONException e) {
            throw new IOException("Failed to parse Supabase response", e);
        }
    }

    public JSONArray select(String table, String columns) throws IOException {
        return select(table, columns, null);
    }

    /** POST {table} with a JSON body (single object or array of objects). */
    public void insert(String table, JSONObject body) throws IOException {
        RequestBody requestBody = RequestBody.create(body.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(baseUrl + table)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("Supabase insert failed (" + response.code() + "): " + err);
            }
        }
    }

    /** PATCH {table}?{filters} with a JSON body of fields to update. */
    public void update(String table, JSONObject body, Map<String, String> filters) throws IOException {
        StringBuilder url = new StringBuilder(baseUrl).append(table).append('?');
        boolean first = true;
        for (Map.Entry<String, String> e : filters.entrySet()) {
            if (!first) url.append('&');
            url.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        RequestBody requestBody = RequestBody.create(body.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url.toString())
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("Supabase update failed (" + response.code() + "): " + err);
            }
        }
    }

    /** URL-encodes an equality filter value, e.g. eqFilter("RED_1") -> "eq.RED_1" */
    public static String eq(Object value) {
        return "eq." + value;
    }
}

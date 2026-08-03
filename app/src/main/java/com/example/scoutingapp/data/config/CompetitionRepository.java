package com.example.scoutingapp.data.config;

import android.util.Log;

import com.example.scoutingapp.data.supabase.PostgrestClient;
import com.example.scoutingapp.util.AppExecutors;
import com.example.scoutingapp.util.Callback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompetitionRepository {

    private final PostgrestClient client = new PostgrestClient();

    /** Equivalent of Kotlin's `suspend fun fetchCompetitions()`, delivered via callback on main thread. */
    public void fetchCompetitions(Callback<List<Competition>> callback) {
        AppExecutors.runBackground(() -> {
            try {
                JSONArray rows = client.select("competitions", "*");
                List<Competition> competitions = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    competitions.add(new Competition(row.getString("key"), row.getString("display_name")));
                }
                AppExecutors.runOnMain(() -> callback.onSuccess(competitions));
            } catch (Exception e) {
                Log.e("CompetitionRepo", "Failed to fetch competitions: " + e.getMessage(), e);
                List<Competition> fallback = Arrays.asList(
                        new Competition("dalton", "Error: Dalton"),
                        new Competition("albany", "Error: Albany"),
                        new Competition("dcmp", "Error: DCMP")
                );
                AppExecutors.runOnMain(() -> callback.onSuccess(fallback));
            }
        });
    }
}

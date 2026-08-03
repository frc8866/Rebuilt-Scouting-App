package com.example.scoutingapp.data.repository;

import android.util.Log;

import com.example.scoutingapp.data.supabase.PostgrestClient;
import com.example.scoutingapp.ui.data.DataModels;
import com.example.scoutingapp.ui.data.IDataRepository;
import com.example.scoutingapp.ui.data.PitScoutEntry;
import com.example.scoutingapp.ui.data.TeamAggregate;
import com.example.scoutingapp.util.AppExecutors;
import com.example.scoutingapp.util.Callback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DataRepository implements IDataRepository {

    private final String competition;
    private final PostgrestClient client = new PostgrestClient();

    public DataRepository(String competition) {
        this.competition = competition;
    }

    private String qualsTable() { return competition + "_quals"; }
    private String dataTable() { return competition + "_data"; }

    private static String positionString(DataModels.TeamPosition pos) {
        switch (pos) {
            case RED1: return "red1";
            case RED2: return "red2";
            case RED3: return "red3";
            case BLUE1: return "blue1";
            case BLUE2: return "blue2";
            case BLUE3: return "blue3";
            default: throw new IllegalStateException();
        }
    }

    private static QualMatch parseQualMatch(JSONObject row) throws Exception {
        QualMatch m = new QualMatch();
        m.id = row.getInt("id");
        m.red1_team = row.getInt("red1_team");
        m.red2_team = row.getInt("red2_team");
        m.red3_team = row.getInt("red3_team");
        m.blue1_team = row.getInt("blue1_team");
        m.blue2_team = row.getInt("blue2_team");
        m.blue3_team = row.getInt("blue3_team");
        return m;
    }

    // ── fetchAllMatches ───────────────────────────────────────────────────────

    @Override
    public void fetchAllMatches(String competition, String query, Callback<List<DataModels.MatchGroup>> callback) {
        AppExecutors.runBackground(() -> {
            try {
                // Fetch quals schedule in parallel with the 6 per-position scouted-ID queries,
                // same parallelism as the original's coroutineScope { async {...} }.
                CompletableFuture<List<QualMatch>> qualsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        JSONArray rows = client.select(qualsTable(), "*");
                        List<QualMatch> list = new ArrayList<>();
                        for (int i = 0; i < rows.length(); i++) list.add(parseQualMatch(rows.getJSONObject(i)));
                        list.sort((a, b) -> Integer.compare(a.id, b.id));
                        return list;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                Map<DataModels.TeamPosition, CompletableFuture<java.util.Set<Integer>>> scoutedFutures = new HashMap<>();
                for (DataModels.TeamPosition pos : DataModels.TeamPosition.values()) {
                    scoutedFutures.put(pos, CompletableFuture.supplyAsync(() -> {
                        try {
                            Map<String, String> filters = new HashMap<>();
                            filters.put("scouting_position", PostgrestClient.eq(positionString(pos)));
                            JSONArray rows = client.select(dataTable(), "match_number", filters);
                            java.util.Set<Integer> ids = new java.util.HashSet<>();
                            for (int i = 0; i < rows.length(); i++) ids.add(rows.getJSONObject(i).getInt("match_number"));
                            return ids;
                        } catch (Exception e) {
                            Log.w("DataRepository", "Failed scouted IDs for " + pos + ": " + e.getMessage());
                            return new java.util.HashSet<Integer>();
                        }
                    }));
                }

                List<QualMatch> quals = qualsFuture.join();
                Map<DataModels.TeamPosition, java.util.Set<Integer>> scoutedById = new HashMap<>();
                for (Map.Entry<DataModels.TeamPosition, CompletableFuture<java.util.Set<Integer>>> e : scoutedFutures.entrySet()) {
                    scoutedById.put(e.getKey(), e.getValue().join());
                }

                String q = query == null ? "" : query.trim().toLowerCase();
                List<DataModels.MatchGroup> result = new ArrayList<>();
                for (QualMatch m : quals) {
                    if (!q.isEmpty()) {
                        boolean matches = String.valueOf(m.id).contains(q)
                                || String.valueOf(m.red1_team).contains(q)
                                || String.valueOf(m.red2_team).contains(q)
                                || String.valueOf(m.red3_team).contains(q)
                                || String.valueOf(m.blue1_team).contains(q)
                                || String.valueOf(m.blue2_team).contains(q)
                                || String.valueOf(m.blue3_team).contains(q);
                        if (!matches) continue;
                    }
                    result.add(new DataModels.MatchGroup(
                            m.id,
                            java.util.Arrays.asList(
                                    new DataModels.TeamChip(m.id, m.red1_team, DataModels.TeamPosition.RED1,
                                            scouted(scoutedById, DataModels.TeamPosition.RED1, m.id)),
                                    new DataModels.TeamChip(m.id, m.red2_team, DataModels.TeamPosition.RED2,
                                            scouted(scoutedById, DataModels.TeamPosition.RED2, m.id)),
                                    new DataModels.TeamChip(m.id, m.red3_team, DataModels.TeamPosition.RED3,
                                            scouted(scoutedById, DataModels.TeamPosition.RED3, m.id))
                            ),
                            java.util.Arrays.asList(
                                    new DataModels.TeamChip(m.id, m.blue1_team, DataModels.TeamPosition.BLUE1,
                                            scouted(scoutedById, DataModels.TeamPosition.BLUE1, m.id)),
                                    new DataModels.TeamChip(m.id, m.blue2_team, DataModels.TeamPosition.BLUE2,
                                            scouted(scoutedById, DataModels.TeamPosition.BLUE2, m.id)),
                                    new DataModels.TeamChip(m.id, m.blue3_team, DataModels.TeamPosition.BLUE3,
                                            scouted(scoutedById, DataModels.TeamPosition.BLUE3, m.id))
                            )
                    ));
                }
                AppExecutors.runOnMain(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e("DataRepository", "fetchAllMatches failed: " + e.getMessage(), e);
                AppExecutors.runOnMain(() -> callback.onSuccess(new ArrayList<>()));
            }
        });
    }

    private static boolean scouted(Map<DataModels.TeamPosition, java.util.Set<Integer>> scoutedById,
                                    DataModels.TeamPosition pos, int matchId) {
        java.util.Set<Integer> ids = scoutedById.get(pos);
        return ids != null && ids.contains(matchId);
    }

    // ── fetchTeamAggregate ────────────────────────────────────────────────────

    @Override
    public void fetchTeamAggregate(String competition, int teamNumber, Callback<TeamAggregate> callback) {
        AppExecutors.runBackground(() -> {
            try {
                JSONArray qualsRows = client.select(qualsTable(), "*");
                boolean appearsInSchedule = false;
                for (int i = 0; i < qualsRows.length() && !appearsInSchedule; i++) {
                    JSONObject row = qualsRows.getJSONObject(i);
                    appearsInSchedule = row.getInt("red1_team") == teamNumber || row.getInt("red2_team") == teamNumber
                            || row.getInt("red3_team") == teamNumber || row.getInt("blue1_team") == teamNumber
                            || row.getInt("blue2_team") == teamNumber || row.getInt("blue3_team") == teamNumber;
                }

                if (!appearsInSchedule) {
                    Log.w("DataRepository", "Team " + teamNumber + " not found in quals schedule");
                    AppExecutors.runOnMain(() -> callback.onSuccess(null));
                    return;
                }

                List<JSONObject> allRows = new ArrayList<>();
                try {
                    Map<String, String> filters = new HashMap<>();
                    filters.put("team_number", PostgrestClient.eq(teamNumber));
                    JSONArray rows = client.select(dataTable(), "*", filters);
                    for (int i = 0; i < rows.length(); i++) allRows.add(rows.getJSONObject(i));
                } catch (Exception e) {
                    Log.w("DataRepository", "Failed fetching rows for team " + teamNumber + ": " + e.getMessage());
                }

                if (allRows.isEmpty()) {
                    TeamAggregate empty = new TeamAggregate(teamNumber, 0, 0.0, 0.0, 0.0, 0.0,
                            false, false, false, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, new ArrayList<>());
                    AppExecutors.runOnMain(() -> callback.onSuccess(empty));
                    return;
                }

                TeamAggregate agg = aggregate(teamNumber, allRows);
                AppExecutors.runOnMain(() -> callback.onSuccess(agg));
            } catch (Exception e) {
                Log.e("DataRepository", "fetchTeamAggregate failed: " + e.getMessage(), e);
                AppExecutors.runOnMain(() -> callback.onSuccess(null));
            }
        });
    }

    // ── fetchAllPitScoutEntries ───────────────────────────────────────────────

    @Override
    public void fetchAllPitScoutEntries(String competition, int teamNumber, Callback<List<PitScoutEntry>> callback) {
        Log.d("DataRepository", "fetchPitScoutEntries: competition=" + competition + ", teamNumber=" + teamNumber);
        AppExecutors.runBackground(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("competition", PostgrestClient.eq(competition));
                filters.put("team_number", PostgrestClient.eq(teamNumber));
                JSONArray rows = client.select("pit_scouting_data", "*", filters);
                Log.d("DataRepository", "pit rows returned: " + rows.length());

                List<PitScoutEntry> entries = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    entries.add(new PitScoutEntry(
                            row.optInt("id", 0),
                            row.optString("scouter_name", ""),
                            row.optString("competition", ""),
                            row.optString("team_number", ""),
                            row.optString("drive_train", ""),
                            row.optString("drive_train_other", ""),
                            row.optString("shooter", ""),
                            row.optString("shooter_other", ""),
                            row.optString("camera", ""),
                            row.optInt("hopper_capacity", 0),
                            row.optString("indexer", ""),
                            row.optString("indexer_other", ""),
                            row.optString("notes", "")
                    ));
                }
                AppExecutors.runOnMain(() -> callback.onSuccess(entries));
            } catch (Exception e) {
                Log.e("DataRepository", "fetchAllPitScoutEntries failed: " + e.getMessage(), e);
                AppExecutors.runOnMain(() -> callback.onSuccess(new ArrayList<>()));
            }
        });
    }

    // ── Aggregation logic ─────────────────────────────────────────────────────

    private static Double optDouble(JSONObject row, String key) {
        if (!row.has(key) || row.isNull(key)) return null;
        Object v = row.opt(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }

    private static Boolean optBool(JSONObject row, String key) {
        if (!row.has(key) || row.isNull(key)) return null;
        Object v = row.opt(key);
        if (v instanceof Boolean) return (Boolean) v;
        return null;
    }

    private static String optStr(JSONObject row, String key) {
        if (!row.has(key) || row.isNull(key)) return null;
        Object v = row.opt(key);
        return v == null ? null : String.valueOf(v);
    }

    private static double round1(double d) {
        return ((long) (d * 10)) / 10.0;
    }

    private TeamAggregate aggregate(int teamNumber, List<JSONObject> rows) {
        int n = rows.size(); // includes off-field entries for matchesScoutedCount

        List<JSONObject> onField = new ArrayList<>();
        for (JSONObject r : rows) {
            Boolean b = optBool(r, "on_field");
            if (b != null && b) onField.add(r);
        }
        int nOnField = onField.size();

        java.util.function.Function<String, Double> pct = key -> {
            if (nOnField == 0) return 0.0;
            int count = 0;
            for (JSONObject r : onField) {
                Boolean b = optBool(r, key);
                if (b != null && b) count++;
            }
            return count / (double) nOnField * 100.0;
        };

        java.util.function.Function<String, Boolean> majority = key -> {
            int trues = 0;
            for (JSONObject r : onField) {
                Boolean b = optBool(r, key);
                if (b != null && b) trues++;
            }
            return trues >= (nOnField - trues);
        };

        java.util.function.Function<String, Double> avg = key -> {
            List<Double> vals = new ArrayList<>();
            for (JSONObject r : onField) {
                Double d = optDouble(r, key);
                if (d != null) vals.add(d);
            }
            if (vals.isEmpty()) return 0.0;
            double sum = 0;
            for (double v : vals) sum += v;
            return sum / vals.size();
        };

        java.util.function.Function<String, Double> avgNonZero = key -> {
            List<Double> vals = new ArrayList<>();
            for (JSONObject r : onField) {
                Double d = optDouble(r, key);
                if (d != null && d > 0.0) vals.add(d);
            }
            if (vals.isEmpty()) return 0.0;
            double sum = 0;
            for (double v : vals) sum += v;
            return sum / vals.size();
        };

        List<Double> fuelPerMatchVals = new ArrayList<>();
        for (JSONObject row : onField) {
            Double autoF = optDouble(row, "alliance_auto_fuel_score");
            Double teleopF = optDouble(row, "alliance_teleop_fuel_score");
            Double score;
            if ((autoF == null || autoF == 0.0) && (teleopF == null || teleopF == 0.0) && autoF == null) {
                score = null;
            } else {
                score = (autoF == null ? 0.0 : autoF) + (teleopF == null ? 0.0 : teleopF);
            }
            Double pctVal = optDouble(row, "fuel_percent");
            if (score != null && pctVal != null) {
                fuelPerMatchVals.add(score * pctVal / 100.0);
            }
        }
        double avgFuelPerMatch = 0.0;
        if (!fuelPerMatchVals.isEmpty()) {
            double sum = 0;
            for (double v : fuelPerMatchVals) sum += v;
            avgFuelPerMatch = sum / fuelPerMatchVals.size();
        }

        List<String> notes = new ArrayList<>();
        for (JSONObject r : rows) {
            String note = optStr(r, "notes");
            if (note != null && !note.trim().isEmpty()) notes.add(note);
        }

        return new TeamAggregate(
                teamNumber,
                n,
                nOnField == 0 ? 0.0 : round1(countTrue(onField, "preload") / (double) nOnField * 100.0),
                n == 0 ? 0.0 : round1(nOnField / (double) n * 100.0),
                round1(pct.apply("won_auto")),
                round1(pct.apply("won_match")),
                majority.apply("bump"),
                majority.apply("trench"),
                majority.apply("ground_intake"),
                majority.apply("station"),
                round1(avgNonZero.apply("avg_intake")),
                round1(avgNonZero.apply("avg_shoot")),
                round1(avgNonZero.apply("avg_defend")),
                round1(avg.apply("fuel_percent")),
                round1(avg.apply("driver_skill")),
                round1(avgFuelPerMatch),
                notes
        );
    }

    private static int countTrue(List<JSONObject> rows, String key) {
        int count = 0;
        for (JSONObject r : rows) {
            Boolean b = optBool(r, key);
            if (b != null && b) count++;
        }
        return count;
    }
}

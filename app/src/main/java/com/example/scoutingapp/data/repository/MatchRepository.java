package com.example.scoutingapp.data.repository;

import android.util.Log;

import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.data.scout.PendingUploadStore;
import com.example.scoutingapp.data.supabase.PostgrestClient;
import com.example.scoutingapp.domain.TableResolver;
import com.example.scoutingapp.util.AppExecutors;
import com.example.scoutingapp.util.Callback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MatchRepository {

    private final PostgrestClient client = new PostgrestClient();

    // ── JSON parsing helper ───────────────────────────────────────────────────

    private static QualMatch parseQualMatch(JSONObject row) throws Exception {
        QualMatch m = new QualMatch();
        m.id = row.getInt("id");
        m.red1_team = row.getInt("red1_team");
        m.red2_team = row.getInt("red2_team");
        m.red3_team = row.getInt("red3_team");
        m.blue1_team = row.getInt("blue1_team");
        m.blue2_team = row.getInt("blue2_team");
        m.blue3_team = row.getInt("blue3_team");
        m.red1_scouter = row.optString("red1_scouter", null);
        m.red2_scouter = row.optString("red2_scouter", null);
        m.red3_scouter = row.optString("red3_scouter", null);
        m.blue1_scouter = row.optString("blue1_scouter", null);
        m.blue2_scouter = row.optString("blue2_scouter", null);
        m.blue3_scouter = row.optString("blue3_scouter", null);
        return m;
    }

    // ── Schedule fetch (cached) ───────────────────────────────────────────────

    private List<QualMatch> getSchedule(Competition competition) throws Exception {
        List<QualMatch> cached = ScheduleCache.getSchedule(competition.getKey());
        if (cached != null) return cached;

        try {
            JSONArray rows = client.select(TableResolver.quals(competition), "*");
            List<QualMatch> fetched = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                fetched.add(parseQualMatch(rows.getJSONObject(i)));
            }
            fetched.sort((a, b) -> Integer.compare(a.id, b.id));

            ScheduleCache.putSchedule(competition.getKey(), fetched);
            return fetched;
        } catch (Exception e) {
            // Offline-first fallback: use the schedule downloaded earlier in the event, if any,
            // instead of failing outright just because we can't reach the network right now.
            List<QualMatch> persisted = ScheduleCache.getPersistedSchedule(competition.getKey());
            if (persisted != null) {
                Log.w("MatchRepository", "Schedule fetch failed, using offline copy: " + e.getMessage());
                ScheduleCache.putSchedule(competition.getKey(), persisted);
                return persisted;
            }
            throw e;
        }
    }

    /**
     * Downloads (or re-downloads) the full qual schedule for this competition and persists it
     * to disk, so the rest of the event can run without a network connection. Used both
     * automatically when a competition is selected and from the manual "Re-sync Schedule"
     * action. On failure, the previously downloaded/cached schedule (if any) is left untouched.
     */
    public void prefetchSchedule(Competition competition, Callback<Void> callback) {
        AppExecutors.runBackground(() -> {
            try {
                JSONArray rows = client.select(TableResolver.quals(competition), "*");
                List<QualMatch> fetched = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    fetched.add(parseQualMatch(rows.getJSONObject(i)));
                }
                fetched.sort((a, b) -> Integer.compare(a.id, b.id));
                ScheduleCache.putSchedule(competition.getKey(), fetched);
                AppExecutors.runOnMain(() -> callback.onSuccess(null));
            } catch (Exception e) {
                AppExecutors.runOnMain(() -> callback.onError(e));
            }
        });
    }

    // ── Scouted IDs fetch (cached) ────────────────────────────────────────────

    private Set<Integer> getScoutedIds(Competition competition, ScoutPosition position) {
        Set<Integer> cached = ScheduleCache.getScoutedIds(competition.getKey(), position);
        Set<Integer> base;
        if (cached != null) {
            base = cached;
        } else {
            base = fetchScoutedIdsFromNetwork(competition, position);
            ScheduleCache.putScoutedIds(competition.getKey(), position, base);
        }

        // Offline-first: also treat matches queued locally (submitted but not yet confirmed
        // uploaded) as scouted, so "next match" / "already scouted" checks stay correct even
        // before those submissions reach the server.
        Set<Integer> merged = new HashSet<>(base);
        merged.addAll(PendingUploadStore.getPendingMatchNumbers(
                competition.getKey(), TableResolver.positionString(position)));
        return merged;
    }

    private Set<Integer> fetchScoutedIdsFromNetwork(Competition competition, ScoutPosition position) {
        try {
            String posStr = TableResolver.positionString(position);
            Map<String, String> filters = new HashMap<>();
            filters.put("scouting_position", PostgrestClient.eq(posStr));
            JSONArray rows = client.select(TableResolver.data(competition), "match_number", filters);

            Set<Integer> ids = new HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                ids.add(rows.getJSONObject(i).getInt("match_number"));
            }
            return ids;
        } catch (Exception e) {
            Log.e("MatchRepository", "fetchScoutedIds failed: " + e.getMessage());
            return new HashSet<>();
        }
    }

    // ── Public API (all network work runs on a background thread; callback fires on main) ──

    public void findNextMatch(Competition competition, ScoutPosition position, Callback<NextMatchInfo> callback) {
        AppExecutors.runBackground(() -> {
            try {
                List<QualMatch> allMatches = getSchedule(competition);
                Set<Integer> scoutedIds = getScoutedIds(competition, position);

                QualMatch next = null;
                for (QualMatch m : allMatches) {
                    if (!scoutedIds.contains(m.id)) { next = m; break; }
                }
                if (next == null) {
                    Exception err = new Exception("All matches have been scouted for this position");
                    AppExecutors.runOnMain(() -> callback.onError(err));
                    return;
                }
                QualMatch finalNext = next;
                NextMatchInfo info = new NextMatchInfo(
                        finalNext.id, finalNext.getTeamNumber(position), finalNext.getScouterName(position));
                AppExecutors.runOnMain(() -> callback.onSuccess(info));
            } catch (Exception e) {
                AppExecutors.runOnMain(() -> callback.onError(e));
            }
        });
    }

    public void findNextUnscoutedFrom(Competition competition, ScoutPosition position, int currentMatchId,
                                       int direction, Callback<NextMatchInfo> callback) {
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction must be 1 or -1");
        }
        AppExecutors.runBackground(() -> {
            try {
                List<QualMatch> allMatches = getSchedule(competition);
                Set<Integer> scoutedIds = getScoutedIds(competition, position);

                List<QualMatch> candidates = new ArrayList<>();
                if (direction > 0) {
                    for (QualMatch m : allMatches) {
                        if (m.id > currentMatchId && !scoutedIds.contains(m.id)) candidates.add(m);
                    }
                } else {
                    for (int i = allMatches.size() - 1; i >= 0; i--) {
                        QualMatch m = allMatches.get(i);
                        if (m.id < currentMatchId && !scoutedIds.contains(m.id)) candidates.add(m);
                    }
                }

                if (candidates.isEmpty()) {
                    String msg = direction > 0
                            ? "No more unscouted matches after Qual " + currentMatchId
                            : "No unscouted matches before Qual " + currentMatchId;
                    Exception err = new Exception(msg);
                    AppExecutors.runOnMain(() -> callback.onError(err));
                    return;
                }

                QualMatch next = candidates.get(0);
                NextMatchInfo info = new NextMatchInfo(
                        next.id, next.getTeamNumber(position), next.getScouterName(position));
                AppExecutors.runOnMain(() -> callback.onSuccess(info));
            } catch (Exception e) {
                AppExecutors.runOnMain(() -> callback.onError(e));
            }
        });
    }

    public void isMatchScouted(Competition competition, ScoutPosition position, int matchId, Callback<Boolean> callback) {
        AppExecutors.runBackground(() -> {
            boolean result;
            try {
                result = getScoutedIds(competition, position).contains(matchId);
            } catch (Exception e) {
                result = false;
            }
            boolean finalResult = result;
            AppExecutors.runOnMain(() -> callback.onSuccess(finalResult));
        });
    }

    public void getMatchDetails(Competition competition, int matchId, Callback<QualMatch> callback) {
        AppExecutors.runBackground(() -> {
            try {
                List<QualMatch> cachedList = ScheduleCache.getSchedule(competition.getKey());
                QualMatch cached = null;
                if (cachedList != null) {
                    for (QualMatch m : cachedList) {
                        if (m.id == matchId) { cached = m; break; }
                    }
                }
                if (cached != null) {
                    QualMatch finalCached = cached;
                    AppExecutors.runOnMain(() -> callback.onSuccess(finalCached));
                    return;
                }

                Map<String, String> filters = new HashMap<>();
                filters.put("id", PostgrestClient.eq(matchId));
                JSONArray rows = client.select(TableResolver.quals(competition), "*", filters);

                if (rows.length() == 0) {
                    Exception err = new Exception("Match " + matchId + " not found");
                    AppExecutors.runOnMain(() -> callback.onError(err));
                    return;
                }
                QualMatch match = parseQualMatch(rows.getJSONObject(0));
                AppExecutors.runOnMain(() -> callback.onSuccess(match));
            } catch (Exception e) {
                AppExecutors.runOnMain(() -> callback.onError(e));
            }
        });
    }

    /** After a successful submit, next findNextMatch re-fetches scouted IDs from the network for this position. */
    public void invalidateScoutedIds(Competition competition, ScoutPosition position) {
        ScheduleCache.invalidateScoutedIds(competition.getKey(), position);
    }

    /** When competition changes, schedule is re-fetched from network. */
    public void invalidateSchedule(Competition competition) {
        ScheduleCache.invalidateSchedule(competition.getKey());
    }
}

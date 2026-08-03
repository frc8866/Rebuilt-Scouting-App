package com.example.scoutingapp.ui.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Collections;
import java.util.List;

public class DataViewModel extends ViewModel {

    /** Equivalent of Kotlin's `data class Ui` (immutable UI-state snapshot). */
    public static class Ui {
        public final String query;
        public final boolean loading;
        public final List<DataModels.MatchGroup> matches;
        public final DataModels.TeamSelection selection;
        public final boolean aggregateLoading;
        public final TeamAggregate aggregate;
        public final List<PitScoutEntry> pitEntries;
        public final boolean pitLoading;

        public Ui(String query, boolean loading, List<DataModels.MatchGroup> matches,
                  DataModels.TeamSelection selection, boolean aggregateLoading, TeamAggregate aggregate,
                  List<PitScoutEntry> pitEntries, boolean pitLoading) {
            this.query = query;
            this.loading = loading;
            this.matches = matches;
            this.selection = selection;
            this.aggregateLoading = aggregateLoading;
            this.aggregate = aggregate;
            this.pitEntries = pitEntries;
            this.pitLoading = pitLoading;
        }

        public static Ui initial() {
            return new Ui("", false, Collections.emptyList(), null, false, null, Collections.emptyList(), false);
        }

        // "copy" helpers, equivalent to Kotlin data class .copy()
        public Ui withQuery(String q) {
            return new Ui(q, loading, matches, selection, aggregateLoading, aggregate, pitEntries, pitLoading);
        }
        public Ui withLoading(boolean l) {
            return new Ui(query, l, matches, selection, aggregateLoading, aggregate, pitEntries, pitLoading);
        }
        public Ui withMatches(boolean l, List<DataModels.MatchGroup> m) {
            return new Ui(query, l, m, selection, aggregateLoading, aggregate, pitEntries, pitLoading);
        }
        public Ui withSelection(DataModels.TeamSelection s, boolean aggLoading, TeamAggregate agg) {
            return new Ui(query, loading, matches, s, aggLoading, agg, pitEntries, pitLoading);
        }
        public Ui withAggregate(boolean aggLoading, TeamAggregate agg) {
            return new Ui(query, loading, matches, selection, aggLoading, agg, pitEntries, pitLoading);
        }
        public Ui withPit(boolean pLoading, List<PitScoutEntry> entries) {
            return new Ui(query, loading, matches, selection, aggregateLoading, aggregate, entries, pLoading);
        }
    }

    private final IDataRepository repository;
    private final String competition;

    private final MutableLiveData<Ui> _ui = new MutableLiveData<>(Ui.initial());
    public final LiveData<Ui> ui = _ui;

    public DataViewModel(IDataRepository repository, String competition) {
        this.repository = repository;
        this.competition = competition;
    }

    private Ui current() {
        Ui value = _ui.getValue();
        return value != null ? value : Ui.initial();
    }

    public void onQueryChange(String q) {
        _ui.setValue(current().withQuery(q));
        load();
    }

    public void onTeamSelected(DataModels.TeamSelection sel) {
        Ui cur = current();
        boolean sameTeam = cur.selection != null && cur.selection.teamNumber == sel.teamNumber;
        _ui.setValue(cur.withSelection(sel, !sameTeam, sameTeam ? cur.aggregate : null));
        if (!sameTeam) reloadAggregate(sel.teamNumber);
        if (!sameTeam) reloadPit(sel.teamNumber);
    }

    public void load() {
        Log.d("DataViewModel", "load() called, competition=" + competition);
        _ui.setValue(current().withLoading(true));
        repository.fetchAllMatches(competition, current().query, new com.example.scoutingapp.util.Callback<List<DataModels.MatchGroup>>() {
            @Override
            public void onSuccess(List<DataModels.MatchGroup> result) {
                Log.d("DataViewModel", "load() got " + result.size() + " matches");
                _ui.setValue(current().withMatches(false, result));
            }

            @Override
            public void onError(Exception e) {
                _ui.setValue(current().withMatches(false, Collections.emptyList()));
            }
        });
    }

    public void reloadAggregate() { reloadAggregate(null); }

    public void reloadAggregate(Integer teamNumber) {
        Integer team = teamNumber != null ? teamNumber
                : (current().selection != null ? current().selection.teamNumber : null);
        if (team == null) return;
        _ui.setValue(current().withAggregate(true, current().aggregate));
        repository.fetchTeamAggregate(competition, team, new com.example.scoutingapp.util.Callback<TeamAggregate>() {
            @Override
            public void onSuccess(TeamAggregate result) {
                _ui.setValue(current().withAggregate(false, result));
            }

            @Override
            public void onError(Exception e) {
                _ui.setValue(current().withAggregate(false, null));
            }
        });
    }

    public void reloadPit() { reloadPit(null); }

    public void reloadPit(Integer teamNumber) {
        Integer team = teamNumber != null ? teamNumber
                : (current().selection != null ? current().selection.teamNumber : null);
        if (team == null) return;
        _ui.setValue(current().withPit(true, current().pitEntries));
        Log.d("DataViewModel", "pit filter: competition=" + competition + ", team_number=" + team);
        repository.fetchAllPitScoutEntries(competition, team, new com.example.scoutingapp.util.Callback<List<PitScoutEntry>>() {
            @Override
            public void onSuccess(List<PitScoutEntry> result) {
                _ui.setValue(current().withPit(false, result));
            }

            @Override
            public void onError(Exception e) {
                _ui.setValue(current().withPit(false, Collections.emptyList()));
            }
        });
    }

    public static ViewModelProvider.Factory factory(IDataRepository repo, String competition) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new DataViewModel(repo, competition);
            }
        };
    }
}

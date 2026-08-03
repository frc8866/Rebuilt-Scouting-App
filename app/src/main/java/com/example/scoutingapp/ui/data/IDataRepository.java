package com.example.scoutingapp.ui.data;

import com.example.scoutingapp.util.Callback;

import java.util.List;

public interface IDataRepository {
    void fetchAllMatches(String competition, String query, Callback<List<DataModels.MatchGroup>> callback);

    /** aggregate may be null in onSuccess, matching the original nullable TeamAggregate? return. */
    void fetchTeamAggregate(String competition, int teamNumber, Callback<TeamAggregate> callback);

    void fetchAllPitScoutEntries(String competition, int teamNumber, Callback<List<PitScoutEntry>> callback);
}

package com.example.scoutingapp.data.repository;

public class NextMatchInfo {
    public final int matchId;
    public final int teamNumber;
    public final String scouterName;

    public NextMatchInfo(int matchId, int teamNumber, String scouterName) {
        this.matchId = matchId;
        this.teamNumber = teamNumber;
        this.scouterName = scouterName;
    }
}

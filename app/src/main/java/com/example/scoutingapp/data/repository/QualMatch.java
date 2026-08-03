package com.example.scoutingapp.data.repository;

import com.example.scoutingapp.data.config.ScoutPosition;

/** Equivalent of Kotlin @Serializable data class QualMatch */
public class QualMatch {
    public int id;
    public int red1_team;
    public int red2_team;
    public int red3_team;
    public int blue1_team;
    public int blue2_team;
    public int blue3_team;
    public String red1_scouter;
    public String red2_scouter;
    public String red3_scouter;
    public String blue1_scouter;
    public String blue2_scouter;
    public String blue3_scouter;

    public int getTeamNumber(ScoutPosition position) {
        switch (position) {
            case RED_1: return red1_team;
            case RED_2: return red2_team;
            case RED_3: return red3_team;
            case BLUE_1: return blue1_team;
            case BLUE_2: return blue2_team;
            case BLUE_3: return blue3_team;
            default: throw new IllegalStateException();
        }
    }

    public String getScouterName(ScoutPosition position) {
        String v;
        switch (position) {
            case RED_1: v = red1_scouter; break;
            case RED_2: v = red2_scouter; break;
            case RED_3: v = red3_scouter; break;
            case BLUE_1: v = blue1_scouter; break;
            case BLUE_2: v = blue2_scouter; break;
            case BLUE_3: v = blue3_scouter; break;
            default: throw new IllegalStateException();
        }
        return v == null ? "" : v;
    }
}

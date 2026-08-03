package com.example.scoutingapp.domain;

import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.ScoutPosition;

public class TableResolver {

    public static String quals(Competition competition) {
        return competition.getKey() + "_quals";
    }

    public static String data(Competition competition) {
        return competition.getKey() + "_data";
    }

    public static String positionString(ScoutPosition position) {
        switch (position) {
            case RED_1: return "red1";
            case RED_2: return "red2";
            case RED_3: return "red3";
            case BLUE_1: return "blue1";
            case BLUE_2: return "blue2";
            case BLUE_3: return "blue3";
            default: throw new IllegalStateException("Unknown position: " + position);
        }
    }
}

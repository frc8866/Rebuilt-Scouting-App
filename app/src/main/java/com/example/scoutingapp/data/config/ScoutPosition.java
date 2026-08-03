package com.example.scoutingapp.data.config;

public enum ScoutPosition {
    RED_1, RED_2, RED_3,
    BLUE_1, BLUE_2, BLUE_3;

    /** Equivalent of the Kotlin extension function ScoutPosition.toLabel() */
    public String toLabel() {
        switch (this) {
            case RED_1: return "Red 1";
            case RED_2: return "Red 2";
            case RED_3: return "Red 3";
            case BLUE_1: return "Blue 1";
            case BLUE_2: return "Blue 2";
            case BLUE_3: return "Blue 3";
            default: throw new IllegalStateException("Unknown position: " + this);
        }
    }
}

package com.example.scoutingapp.data.config;

import java.util.Objects;

/** Equivalent of Kotlin: data class DeviceConfig(competition, scoutPosition, setupComplete=false, scoutName) */
public class DeviceConfig {
    private final Competition competition;
    private final ScoutPosition scoutPosition;
    private final boolean setupComplete;
    private final String scoutName;

    public DeviceConfig(Competition competition, ScoutPosition scoutPosition, boolean setupComplete, String scoutName) {
        this.competition = competition;
        this.scoutPosition = scoutPosition;
        this.setupComplete = setupComplete;
        this.scoutName = scoutName;
    }

    // Overload matching Kotlin's default setupComplete = false
    public DeviceConfig(Competition competition, ScoutPosition scoutPosition, String scoutName) {
        this(competition, scoutPosition, false, scoutName);
    }

    public Competition getCompetition() { return competition; }
    public ScoutPosition getScoutPosition() { return scoutPosition; }
    public boolean isSetupComplete() { return setupComplete; }
    public String getScoutName() { return scoutName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceConfig)) return false;
        DeviceConfig that = (DeviceConfig) o;
        return setupComplete == that.setupComplete
                && Objects.equals(competition, that.competition)
                && scoutPosition == that.scoutPosition
                && Objects.equals(scoutName, that.scoutName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(competition, scoutPosition, setupComplete, scoutName);
    }

    @Override
    public String toString() {
        return "DeviceConfig{competition=" + competition + ", scoutPosition=" + scoutPosition
                + ", setupComplete=" + setupComplete + ", scoutName='" + scoutName + "'}";
    }
}

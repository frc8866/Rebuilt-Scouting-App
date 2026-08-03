package com.example.scoutingapp.ui.data;

import java.util.List;

/**
 * Aggregate stats for a team across all their scouted matches.
 * Percentage fields (0.0-100.0): preloadPct, onFieldPct, wonAutoPct, wonMatchPct
 * Majority-vote booleans: bump, trench, groundIntake, station
 * Numeric averages: avgIntake, avgShoot, avgDefend, avgFuelPercent, avgDriverSkill, avgFuelPerMatch
 * notes: one entry per match, blank entries filtered out.
 */
public class TeamAggregate {
    public final int teamNumber;
    public final int matchesScoutedCount;

    public final double preloadPct;
    public final double onFieldPct;
    public final double wonAutoPct;
    public final double wonMatchPct;

    public final boolean bump;
    public final boolean trench;
    public final boolean groundIntake;
    public final boolean station;

    public final double avgIntake;
    public final double avgShoot;
    public final double avgDefend;
    public final double avgFuelPercent;
    public final double avgDriverSkill;
    public final double avgFuelPerMatch;

    public final List<String> notes;

    public TeamAggregate(int teamNumber, int matchesScoutedCount, double preloadPct, double onFieldPct,
                          double wonAutoPct, double wonMatchPct, boolean bump, boolean trench,
                          boolean groundIntake, boolean station, double avgIntake, double avgShoot,
                          double avgDefend, double avgFuelPercent, double avgDriverSkill,
                          double avgFuelPerMatch, List<String> notes) {
        this.teamNumber = teamNumber;
        this.matchesScoutedCount = matchesScoutedCount;
        this.preloadPct = preloadPct;
        this.onFieldPct = onFieldPct;
        this.wonAutoPct = wonAutoPct;
        this.wonMatchPct = wonMatchPct;
        this.bump = bump;
        this.trench = trench;
        this.groundIntake = groundIntake;
        this.station = station;
        this.avgIntake = avgIntake;
        this.avgShoot = avgShoot;
        this.avgDefend = avgDefend;
        this.avgFuelPercent = avgFuelPercent;
        this.avgDriverSkill = avgDriverSkill;
        this.avgFuelPerMatch = avgFuelPerMatch;
        this.notes = notes;
    }
}

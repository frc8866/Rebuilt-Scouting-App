package com.example.scoutingapp.ui.scout;

import org.json.JSONException;
import org.json.JSONObject;

/** Equivalent of Kotlin @Serializable data class ScoutDataPayload, with snake_case @SerialName fields. */
public class ScoutDataPayload {
    public final int matchNumber;
    public final int teamNumber;
    public final String scoutingPosition;
    public final String scouterName;
    public final boolean preload;
    public final boolean onField;
    public final boolean wonAuto;
    public final boolean bump;
    public final boolean trench;
    public final boolean groundIntake;
    public final boolean station;
    public final int driverSkill;
    public final int fuelPercent;
    public final int allianceAutoFuelScore;
    public final int allianceTeleopFuelScore;
    public final boolean wonMatch;
    public final double avgIntake;
    public final double avgShoot;
    public final double totalShoot;
    public final double avgDefend;

    public ScoutDataPayload(int matchNumber, int teamNumber, String scoutingPosition, String scouterName,
                             boolean preload, boolean onField, boolean wonAuto, boolean bump, boolean trench,
                             boolean groundIntake, boolean station, int driverSkill, int fuelPercent,
                             int allianceAutoFuelScore, int allianceTeleopFuelScore, boolean wonMatch,
                             double avgIntake, double avgShoot, double totalShoot, double avgDefend) {
        this.matchNumber = matchNumber;
        this.teamNumber = teamNumber;
        this.scoutingPosition = scoutingPosition;
        this.scouterName = scouterName;
        this.preload = preload;
        this.onField = onField;
        this.wonAuto = wonAuto;
        this.bump = bump;
        this.trench = trench;
        this.groundIntake = groundIntake;
        this.station = station;
        this.driverSkill = driverSkill;
        this.fuelPercent = fuelPercent;
        this.allianceAutoFuelScore = allianceAutoFuelScore;
        this.allianceTeleopFuelScore = allianceTeleopFuelScore;
        this.wonMatch = wonMatch;
        this.avgIntake = avgIntake;
        this.avgShoot = avgShoot;
        this.totalShoot = totalShoot;
        this.avgDefend = avgDefend;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("match_number", matchNumber);
        o.put("team_number", teamNumber);
        o.put("scouting_position", scoutingPosition);
        o.put("scouter_name", scouterName);
        o.put("preload", preload);
        o.put("on_field", onField);
        o.put("won_auto", wonAuto);
        o.put("bump", bump);
        o.put("trench", trench);
        o.put("ground_intake", groundIntake);
        o.put("station", station);
        o.put("driver_skill", driverSkill);
        o.put("fuel_percent", fuelPercent);
        o.put("alliance_auto_fuel_score", allianceAutoFuelScore);
        o.put("alliance_teleop_fuel_score", allianceTeleopFuelScore);
        o.put("won_match", wonMatch);
        o.put("avg_intake", avgIntake);
        o.put("avg_shoot", avgShoot);
        o.put("total_shoot", totalShoot);
        o.put("avg_defend", avgDefend);
        return o;
    }
}

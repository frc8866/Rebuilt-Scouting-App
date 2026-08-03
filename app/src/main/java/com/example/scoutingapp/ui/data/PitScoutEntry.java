package com.example.scoutingapp.ui.data;

public class PitScoutEntry {
    public final int id;
    public final String scouterName;
    public final String competition;
    public final String teamNumber;
    public final String driveTrain;
    public final String driveTrainOther;
    public final String shooter;
    public final String shooterOther;
    public final String camera;
    public final int hopperCapacity;
    public final String indexer;
    public final String indexerOther;
    public final String notes;

    public PitScoutEntry(int id, String scouterName, String competition, String teamNumber,
                          String driveTrain, String driveTrainOther, String shooter, String shooterOther,
                          String camera, int hopperCapacity, String indexer, String indexerOther, String notes) {
        this.id = id;
        this.scouterName = scouterName;
        this.competition = competition;
        this.teamNumber = teamNumber;
        this.driveTrain = driveTrain;
        this.driveTrainOther = driveTrainOther;
        this.shooter = shooter;
        this.shooterOther = shooterOther;
        this.camera = camera;
        this.hopperCapacity = hopperCapacity;
        this.indexer = indexer;
        this.indexerOther = indexerOther;
        this.notes = notes;
    }
}

package com.example.scoutingapp.ui.scout;

public class PhaseWindow {
    public final ScoutStage stage;
    public final int startSec;
    public final int endSec;

    public PhaseWindow(ScoutStage stage, int startSec, int endSec) {
        this.stage = stage;
        this.startSec = startSec;
        this.endSec = endSec;
    }
}

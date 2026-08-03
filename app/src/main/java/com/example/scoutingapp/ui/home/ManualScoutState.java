package com.example.scoutingapp.ui.home;

import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.ScoutPosition;

public class ManualScoutState {
    public String matchId = "";
    public String teamNumber = "";
    public String scouterName = "";
    public ScoutPosition selectedPosition = null;
    public ScoutPosition defaultPosition = ScoutPosition.RED_1;
    public Competition competition = null;
    public boolean isValidating = false;
    public boolean isAlreadyScouted = false;
    public boolean canSubmit = false;

    public ScoutPosition effectivePosition() {
        return selectedPosition != null ? selectedPosition : defaultPosition;
    }
}

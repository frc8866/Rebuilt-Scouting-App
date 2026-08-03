package com.example.scoutingapp.ui.home;

/** Equivalent of Kotlin's (matchId: Int, teamNumber: Int, scouterName: String) -> Unit lambda type. */
public interface NavigateCallback {
    void navigate(int matchId, int teamNumber, String scouterName);
}

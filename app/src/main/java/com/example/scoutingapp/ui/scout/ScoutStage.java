package com.example.scoutingapp.ui.scout;

/** Java equivalent of the Kotlin sealed class ScoutStage (a plain enum, since no subtype carries data). */
public enum ScoutStage {
    Start,            // Start menu with preload + on_field + Play
    Auto,             // Autonomous (0-20s)
    AutoWinnerScreen, // pauses the timer and asks who won auto
    Transition,       // 20-30s
    Shift1,           // 30-55s (loser active)
    Shift2,           // 55-80s (winner active)
    Shift3,           // 80-105s (loser active)
    Shift4,           // 105-130s (winner active)
    Endgame,          // 130-150s (both active)
    Summary           // End of match
}

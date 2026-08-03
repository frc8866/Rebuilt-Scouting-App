package com.example.scoutingapp.ui.scout;

public enum Alliance {
    RED, BLUE;

    public static Alliance fromLabel(String positionLabel) {
        return positionLabel != null && positionLabel.toLowerCase().startsWith("red") ? RED : BLUE;
    }
}

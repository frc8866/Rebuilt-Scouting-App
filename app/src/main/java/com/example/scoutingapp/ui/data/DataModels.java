package com.example.scoutingapp.ui.data;

import java.util.List;

public class DataModels {

    private DataModels() {}

    /** The six scouting positions. Equivalent of Kotlin `enum class TeamPosition`. */
    public enum TeamPosition { RED1, RED2, RED3, BLUE1, BLUE2, BLUE3 }

    /** One team chip inside a match row (left pane). */
    public static class TeamChip {
        public final int matchId;
        public final int teamNumber;
        public final TeamPosition position;
        public final boolean scouted;

        public TeamChip(int matchId, int teamNumber, TeamPosition position, boolean scouted) {
            this.matchId = matchId;
            this.teamNumber = teamNumber;
            this.position = position;
            this.scouted = scouted;
        }
    }

    /** A group for one Quals match (left pane). */
    public static class MatchGroup {
        public final int matchId;
        public final List<TeamChip> red;  // size 3
        public final List<TeamChip> blue; // size 3

        public MatchGroup(int matchId, List<TeamChip> red, List<TeamChip> blue) {
            this.matchId = matchId;
            this.red = red;
            this.blue = blue;
        }
    }

    /** Selecting one team chip to view their aggregate breakdown. */
    public static class TeamSelection {
        public final int matchId; // used only to identify which chip is highlighted
        public final int teamNumber;
        public final TeamPosition position;

        public TeamSelection(int matchId, int teamNumber, TeamPosition position) {
            this.matchId = matchId;
            this.teamNumber = teamNumber;
            this.position = position;
        }
    }
}

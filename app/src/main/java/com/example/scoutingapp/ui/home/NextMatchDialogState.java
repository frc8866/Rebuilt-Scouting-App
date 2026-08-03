package com.example.scoutingapp.ui.home;

import com.example.scoutingapp.data.repository.NextMatchInfo;

/**
 * Java equivalent of the Kotlin sealed class NextMatchDialogState.
 * Sealed classes have no Java equivalent, so this is modeled as an abstract base class
 * with a `Kind` enum for switch-friendly dispatch, plus static factory/subclasses.
 */
public abstract class NextMatchDialogState {

    public enum Kind { HIDDEN, LOADING, SHIFTING, READY, ERROR }

    public abstract Kind kind();

    public static final NextMatchDialogState HIDDEN = new NextMatchDialogState() {
        @Override public Kind kind() { return Kind.HIDDEN; }
    };

    public static final NextMatchDialogState LOADING = new NextMatchDialogState() {
        @Override public Kind kind() { return Kind.LOADING; }
    };

    public static final class Shifting extends NextMatchDialogState {
        public final NextMatchInfo info;
        public Shifting(NextMatchInfo info) { this.info = info; }
        @Override public Kind kind() { return Kind.SHIFTING; }
    }

    public static final class Ready extends NextMatchDialogState {
        public final NextMatchInfo info;
        public Ready(NextMatchInfo info) { this.info = info; }
        @Override public Kind kind() { return Kind.READY; }
    }

    public static final class Error extends NextMatchDialogState {
        public final String message;
        public Error(String message) { this.message = message; }
        @Override public Kind kind() { return Kind.ERROR; }
    }
}

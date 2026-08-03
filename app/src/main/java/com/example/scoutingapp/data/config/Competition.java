package com.example.scoutingapp.data.config;

import java.util.Objects;

/** Equivalent of Kotlin: @Serializable data class Competition(val key, val displayName) */
public class Competition {
    private final String key;
    private final String displayName;

    public Competition(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Competition)) return false;
        Competition that = (Competition) o;
        return Objects.equals(key, that.key) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, displayName);
    }

    @Override
    public String toString() {
        return "Competition{key='" + key + "', displayName='" + displayName + "'}";
    }
}

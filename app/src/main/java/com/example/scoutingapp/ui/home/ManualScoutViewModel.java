package com.example.scoutingapp.ui.home;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.data.config.DeviceConfig;
import com.example.scoutingapp.data.config.DeviceConfigStore;
import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.data.repository.MatchRepository;
import com.example.scoutingapp.util.Callback;

public class ManualScoutViewModel extends ViewModel {

    private final MatchRepository matchRepository;

    private final MutableLiveData<ManualScoutState> _state = new MutableLiveData<>(new ManualScoutState());
    public final LiveData<ManualScoutState> state = _state;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingValidation;

    public ManualScoutViewModel(DeviceConfigStore configStore) {
        this(configStore, new MatchRepository());
    }

    public ManualScoutViewModel(DeviceConfigStore configStore, MatchRepository matchRepository) {
        this.matchRepository = matchRepository;

        configStore.getConfigLiveData().observeForever(this::onConfigChanged);
    }

    private void onConfigChanged(DeviceConfig config) {
        if (config == null) return;
        ManualScoutState s = current();
        s.defaultPosition = config.getScoutPosition();
        s.competition = config.getCompetition();
        s.scouterName = config.getScoutName();
        _state.setValue(s);
    }

    private ManualScoutState current() {
        ManualScoutState v = _state.getValue();
        return v != null ? v : new ManualScoutState();
    }

    public void onMatchIdChange(String value) {
        ManualScoutState s = current();
        s.matchId = value;
        _state.setValue(s);
        scheduleValidation();
    }

    public void onTeamNumberChange(String value) {
        ManualScoutState s = current();
        s.teamNumber = value;
        _state.setValue(s);
        scheduleValidation();
    }

    public void onScouterNameChange(String value) {
        ManualScoutState s = current();
        s.scouterName = value;
        _state.setValue(s);
        scheduleValidation();
    }

    public void onPositionChange(ScoutPosition position) {
        ManualScoutState s = current();
        s.selectedPosition = position;
        _state.setValue(s);
        scheduleValidation();
    }

    /**
     * Cancels any in-flight validation and schedules a new one after 500ms.
     * This means a network call only fires once the user pauses typing/selecting -
     * equivalent of the original's Job.cancel() + delay(500).
     */
    private void scheduleValidation() {
        if (pendingValidation != null) debounceHandler.removeCallbacks(pendingValidation);
        pendingValidation = this::validateForm;
        debounceHandler.postDelayed(pendingValidation, 500);
    }

    private void validateForm() {
        ManualScoutState s = current();
        Integer matchId = parseIntOrNull(s.matchId);
        com.example.scoutingapp.data.config.Competition competition = s.competition;
        ScoutPosition position = s.effectivePosition();

        if (matchId == null || s.scouterName == null || s.scouterName.trim().isEmpty() || competition == null) {
            ManualScoutState updated = current();
            updated.canSubmit = false;
            updated.isAlreadyScouted = false;
            _state.setValue(updated);
            return;
        }

        ManualScoutState validating = current();
        validating.isValidating = true;
        _state.setValue(validating);

        matchRepository.isMatchScouted(competition, position, matchId, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isAlreadyScouted) {
                ManualScoutState updated = current();
                updated.isValidating = false;
                updated.isAlreadyScouted = isAlreadyScouted;
                updated.canSubmit = !isAlreadyScouted;
                _state.setValue(updated);
            }

            @Override
            public void onError(Exception e) {
                ManualScoutState updated = current();
                updated.isValidating = false;
                updated.isAlreadyScouted = false;
                updated.canSubmit = false;
                _state.setValue(updated);
            }
        });
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static ViewModelProvider.Factory factory(DeviceConfigStore configStore) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ManualScoutViewModel(configStore);
            }
        };
    }
}

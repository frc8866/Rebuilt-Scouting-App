package com.example.scoutingapp.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.CompetitionRepository;
import com.example.scoutingapp.data.config.DeviceConfig;
import com.example.scoutingapp.data.config.DeviceConfigStore;
import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.data.repository.MatchRepository;
import com.example.scoutingapp.data.repository.NextMatchInfo;
import com.example.scoutingapp.data.repository.ScheduleCache;
import com.example.scoutingapp.data.scout.MatchStateStore;
import com.example.scoutingapp.data.scout.PendingUploadStore;
import com.example.scoutingapp.data.scout.SavedMatchState;
import com.example.scoutingapp.sync.SyncManager;
import com.example.scoutingapp.util.Callback;

import java.util.Collections;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final DeviceConfigStore configStore;
    private final MatchStateStore matchStateStore;
    private final CompetitionRepository competitionRepo;
    private final MatchRepository matchRepository;

    private final MutableLiveData<List<Competition>> _availableCompetitions = new MutableLiveData<>(Collections.emptyList());
    public final LiveData<List<Competition>> availableCompetitions = _availableCompetitions;

    private final MutableLiveData<Boolean> _competitionsLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> competitionsLoading = _competitionsLoading;

    private final MutableLiveData<String> _nextMatchError = new MutableLiveData<>(null);
    public final LiveData<String> nextMatchError = _nextMatchError;

    private final MutableLiveData<Boolean> _nextMatchLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> nextMatchLoading = _nextMatchLoading;

    private final MutableLiveData<NextMatchDialogState> _dialogState = new MutableLiveData<>(NextMatchDialogState.HIDDEN);
    public final LiveData<NextMatchDialogState> dialogState = _dialogState;

    private NavigateCallback pendingNavigate;

    // ── Offline-first schedule sync ──────────────────────────────────────────

    private final MutableLiveData<Boolean> _scheduleDownloading = new MutableLiveData<>(false);
    public final LiveData<Boolean> scheduleDownloading = _scheduleDownloading;

    private final MutableLiveData<String> _scheduleSyncMessage = new MutableLiveData<>(null);
    public final LiveData<String> scheduleSyncMessage = _scheduleSyncMessage;

    /** Count of scouted matches saved locally but not yet confirmed uploaded to Supabase. */
    public final LiveData<Integer> pendingUploadCount = PendingUploadStore.getCountLiveData();

    // ── Resume ────────────────────────────────────────────────────────────────

    /** Equivalent of Kotlin's pendingResume: StateFlow<SavedMatchState?> */
    public final LiveData<SavedMatchState> pendingResume;

    public void discardResume() {
        matchStateStore.clear();
    }

    // ── Device config ─────────────────────────────────────────────────────────

    /** Equivalent of Kotlin's deviceConfig: StateFlow<DeviceConfig> */
    public final LiveData<DeviceConfig> deviceConfig;

    public HomeViewModel(DeviceConfigStore configStore, MatchStateStore matchStateStore) {
        this(configStore, matchStateStore, new CompetitionRepository(), new MatchRepository());
    }

    public HomeViewModel(DeviceConfigStore configStore, MatchStateStore matchStateStore,
                          CompetitionRepository competitionRepo, MatchRepository matchRepository) {
        this.configStore = configStore;
        this.matchStateStore = matchStateStore;
        this.competitionRepo = competitionRepo;
        this.matchRepository = matchRepository;

        this.pendingResume = matchStateStore.getSavedStateLiveData();
        this.deviceConfig = configStore.getConfigLiveData();

        loadCompetitions();
    }

    public void loadCompetitions() {
        _competitionsLoading.setValue(true);
        competitionRepo.fetchCompetitions(new Callback<List<Competition>>() {
            @Override
            public void onSuccess(List<Competition> result) {
                _availableCompetitions.setValue(result);
                _competitionsLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                _competitionsLoading.setValue(false);
            }
        });
    }

    public void changeCompetition(Competition newComp) {
        DeviceConfig old = configStore.getConfigLiveData().getValue();
        // Invalidate all cached data for the old competition before switching
        ScheduleCache.invalidateAll();
        if (old != null) {
            matchRepository.invalidateSchedule(old.getCompetition());
        }
        configStore.updateCompetition(newComp);
        // Offline-first: download the new competition's schedule right away so the rest of the
        // event can run without a network connection.
        downloadSchedule(newComp);
    }

    /** Manual "Re-sync Schedule" action - re-downloads the current competition's schedule. */
    public void resyncSchedule() {
        DeviceConfig config = configStore.getConfigLiveData().getValue();
        if (config == null) return;
        downloadSchedule(config.getCompetition());
    }

    private void downloadSchedule(Competition competition) {
        _scheduleDownloading.setValue(true);
        matchRepository.prefetchSchedule(competition, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _scheduleDownloading.setValue(false);
                _scheduleSyncMessage.setValue("Schedule downloaded for " + competition.getDisplayName());
            }

            @Override
            public void onError(Exception e) {
                _scheduleDownloading.setValue(false);
                _scheduleSyncMessage.setValue("Couldn't download schedule (offline?). Using cached copy if available.");
            }
        });
    }

    public void clearScheduleSyncMessage() {
        _scheduleSyncMessage.setValue(null);
    }

    /** Manual "Sync Now" action - uploads any locally-queued matches, over any connection type. */
    public void syncNow(Callback<SyncManager.SyncResult> callback) {
        SyncManager.flushNow(callback);
    }

    public void changeScoutPosition(ScoutPosition newPosition) {
        configStore.updateScoutPosition(newPosition);
    }

    public void clearNextMatchError() {
        _nextMatchError.setValue(null);
    }

    public void scoutNextMatch(NavigateCallback onNavigate) {
        _nextMatchLoading.setValue(true);
        _nextMatchError.setValue(null);
        DeviceConfig config = configStore.getConfigLiveData().getValue();
        if (config == null) return;
        matchRepository.findNextMatch(config.getCompetition(), config.getScoutPosition(), new Callback<NextMatchInfo>() {
            @Override
            public void onSuccess(NextMatchInfo info) {
                _nextMatchLoading.setValue(false);
                onNavigate.navigate(info.matchId, info.teamNumber, info.scouterName);
            }

            @Override
            public void onError(Exception e) {
                _nextMatchLoading.setValue(false);
                _nextMatchError.setValue(e.getMessage() != null ? e.getMessage() : "Failed to find next match");
            }
        });
    }

    public void openScoutNextDialog(NavigateCallback onNavigate) {
        pendingNavigate = onNavigate;
        _dialogState.setValue(NextMatchDialogState.LOADING);
        DeviceConfig config = configStore.getConfigLiveData().getValue();
        if (config == null) return;
        matchRepository.findNextMatch(config.getCompetition(), config.getScoutPosition(), new Callback<NextMatchInfo>() {
            @Override
            public void onSuccess(NextMatchInfo info) {
                _dialogState.setValue(new NextMatchDialogState.Ready(info));
            }

            @Override
            public void onError(Exception e) {
                _dialogState.setValue(new NextMatchDialogState.Error(
                        e.getMessage() != null ? e.getMessage() : "Failed to find next match"));
            }
        });
    }

    public void shiftPendingMatch(int direction) {
        NextMatchDialogState state = _dialogState.getValue();
        NextMatchInfo currentInfo;
        if (state instanceof NextMatchDialogState.Ready) {
            currentInfo = ((NextMatchDialogState.Ready) state).info;
        } else if (state instanceof NextMatchDialogState.Shifting) {
            currentInfo = ((NextMatchDialogState.Shifting) state).info;
        } else {
            return;
        }

        _dialogState.setValue(new NextMatchDialogState.Shifting(currentInfo));
        DeviceConfig config = configStore.getConfigLiveData().getValue();
        if (config == null) return;
        matchRepository.findNextUnscoutedFrom(config.getCompetition(), config.getScoutPosition(),
                currentInfo.matchId, direction, new Callback<NextMatchInfo>() {
                    @Override
                    public void onSuccess(NextMatchInfo info) {
                        _dialogState.setValue(new NextMatchDialogState.Ready(info));
                    }

                    @Override
                    public void onError(Exception e) {
                        _dialogState.setValue(new NextMatchDialogState.Ready(currentInfo));
                    }
                });
    }

    public void confirmPendingMatch() {
        NextMatchDialogState state = _dialogState.getValue();
        if (!(state instanceof NextMatchDialogState.Ready)) return;
        NextMatchInfo info = ((NextMatchDialogState.Ready) state).info;
        _dialogState.setValue(NextMatchDialogState.HIDDEN);
        if (pendingNavigate != null) {
            pendingNavigate.navigate(info.matchId, info.teamNumber, info.scouterName);
        }
        pendingNavigate = null;
    }

    public void dismissScoutNextDialog() {
        _dialogState.setValue(NextMatchDialogState.HIDDEN);
        pendingNavigate = null;
    }

    public static ViewModelProvider.Factory factory(DeviceConfigStore configStore, MatchStateStore matchStateStore) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new HomeViewModel(configStore, matchStateStore);
            }
        };
    }
}

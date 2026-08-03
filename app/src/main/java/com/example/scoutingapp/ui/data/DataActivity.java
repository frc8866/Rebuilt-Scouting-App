package com.example.scoutingapp.ui.data;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.R;
import com.example.scoutingapp.data.config.DeviceConfigStore;
import com.example.scoutingapp.data.repository.DataRepository;
import com.example.scoutingapp.ui.data.components.MatchListPaneView;
import com.example.scoutingapp.ui.data.components.TeamBreakdownPaneView;

public class DataActivity extends AppCompatActivity {

    private DataViewModel viewModel;
    private MatchListPaneView matchListPane;
    private TeamBreakdownPaneView teamBreakdownPane;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        DeviceConfigStore configStore = new DeviceConfigStore(this);
        String competition = configStore.getConfigLiveData().getValue() != null
                ? configStore.getConfigLiveData().getValue().getCompetition().getKey()
                : "dalton";

        IDataRepository repository = new DataRepository(competition);
        viewModel = new ViewModelProvider(this, DataViewModel.factory(repository, competition))
                .get(DataViewModel.class);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.drawer_item_scout).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            finish();
        });
        findViewById(R.id.drawer_item_data).setOnClickListener(v -> drawerLayout.closeDrawers());

        FrameLayout matchListContainer = findViewById(R.id.match_list_container);
        FrameLayout teamBreakdownContainer = findViewById(R.id.team_breakdown_container);

        matchListPane = new MatchListPaneView(this, matchListContainer,
                viewModel::onQueryChange, viewModel::onTeamSelected);
        matchListContainer.addView(matchListPane.root);

        teamBreakdownPane = new TeamBreakdownPaneView(this, teamBreakdownContainer);
        teamBreakdownContainer.addView(teamBreakdownPane.root);

        viewModel.ui.observe(this, this::render);
        viewModel.load();
    }

    private void render(DataViewModel.Ui ui) {
        if (ui == null) return;
        matchListPane.render(ui);
        teamBreakdownPane.render(ui.selection, ui.aggregate, ui.aggregateLoading,
                ui.pitEntries, ui.pitLoading, viewModel::reloadAggregate);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}

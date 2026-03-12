package com.example.playmusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class favorites extends AppCompatActivity {
    private ListView favoritesListView;
    private ArrayList<String> favoritesList;
    private ArrayList<String> songPathsList;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ArrayAdapter<String> adapter;
    private static final String PREFS_NAME = "favorites_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoritesListView = findViewById(R.id.favoritesListView);
        favoritesList = new ArrayList<>();
        songPathsList = new ArrayList<>();

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        loadFavoriteSongs();

        favoritesListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSongName = favoritesList.get(position);
            String selectedPath = songPathsList.get(position);

            Intent intent = new Intent(favorites.this, PlayerActivity.class);
            intent.putExtra("songPath", selectedPath);
            intent.putExtra("songName", selectedSongName);
            intent.putStringArrayListExtra("songPaths", songPathsList);
            intent.putExtra("position", position);
            startActivity(intent);
        });
    }

    private void loadFavoriteSongs() {
        executorService.execute(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String path = entry.getKey();
                boolean isFavorite = (Boolean) entry.getValue();

                if (isFavorite) {
                    File file = new File(path);
                    if (file.exists()) {
                        songPathsList.add(path);
                        favoritesList.add(file.getName().replace(".mp3", ""));
                    }
                }
            }

            mainHandler.post(this::updateListView);
        });
    }
    private void updateListView() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoritesList);
        favoritesListView.setAdapter(adapter);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

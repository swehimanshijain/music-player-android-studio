package com.example.playmusic;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ListView songsListView;
    private EditText searchEditText;
    private Button favoritesButton;

    private ArrayList<String> songNamesList;
    private ArrayList<File> songFilesList;

    private ExecutorService executorService;
    private Handler mainHandler;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songsListView = findViewById(R.id.SonglistView);
        searchEditText = findViewById(R.id.SearchEditText);
        favoritesButton = findViewById(R.id.favoritesButton);

        songNamesList = new ArrayList<>();
        songFilesList = new ArrayList<>();

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        favoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, favorites.class);
                startActivity(intent);
            }
        });

        setupSearchFunctionality();

        requestStoragePermission();
    }
    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    private void filterSongs(String query) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final ArrayList<String> filteredList = new ArrayList<>();
                for (String song : songNamesList) {
                    if (song.toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(song);
                    }
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_list_item_1,
                                filteredList);
                        songsListView.setAdapter(adapter);
                    }
                });
            }
        });
    }
    private void requestStoragePermission() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        fetchAllSongs();
                    }
                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this,
                                "Permission needed to access songs",
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void fetchAllSongs() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                songNamesList.clear();
                songFilesList.clear();

                File[] possibleDirs = {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        new File(Environment.getExternalStorageDirectory(), "Music"),
                        new File(Environment.getExternalStorageDirectory(), "Download"),
                        new File(Environment.getExternalStorageDirectory(), "Documents")
                };

                for (File dir : possibleDirs) {
                    ArrayList<File> songs = findSongs(dir);
                    for (File song : songs) {
                        String songName = song.getName().replace(".mp3", "_");
                        if (!songNamesList.contains(songName)) {
                            songNamesList.add(songName);
                            songFilesList.add(song);
                        }
                    }
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (songNamesList.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "No songs found. Check storage permissions.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            adapter = new ArrayAdapter<>(MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    songNamesList);
                            songsListView.setAdapter(adapter);

        songsListView.setOnItemClickListener(
                 new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                           int position, long id) {
                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                        intent.putExtra("songPath", songFilesList.get(position).getAbsolutePath());
                        intent.putExtra("songName", songNamesList.get(position));
                        intent.putExtra("position", position);
                        intent.putExtra("songPaths", getSongPaths(songFilesList));
                        startActivity(intent);

                    }
                 });
                        }
                    }
                });
            }
        });
    }
    private ArrayList<String> getSongPaths(ArrayList<File> files) {
        ArrayList<String> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.getAbsolutePath());
        }
        return paths;
    }

    private ArrayList<File> findSongs(File directory) {
        ArrayList<File> songs = new ArrayList<>();
        if (directory == null || !directory.exists()) {
            return songs;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (file.isDirectory() && !file.isHidden()) {
                        songs.addAll(findSongs(file));
                    } else if (file.getName().toLowerCase().endsWith(".mp3")) {
                        songs.add(file);
                    }
                } catch (Exception e) {
                }
            }
        }
        return songs;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

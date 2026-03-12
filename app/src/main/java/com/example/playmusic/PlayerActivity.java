package com.example.playmusic;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private ImageButton playBtn, prevBtn, nextBtn, likeBtn;
    private SeekBar seekBar;
    private TextView songNameTv;
    private boolean isPlaying = false;
    private String songPath, songName;
    private ArrayList<String> favoritesList;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isFavorite = false;
    private ArrayList<String> songPaths;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getWindow().setStatusBarColor(getResources().getColor(R.color.grey));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.grey));

        Intent intent = getIntent();
        songPath = intent.getStringExtra("songPath");
        songName = intent.getStringExtra("songName");
        position = intent.getIntExtra("position", 0);
        songPaths = intent.getStringArrayListExtra("songPaths");

        playBtn = findViewById(R.id.playButton);
        prevBtn = findViewById(R.id.previousButton);
        nextBtn = findViewById(R.id.nextButton);
        likeBtn = findViewById(R.id.favoriteButton);
        seekBar = findViewById(R.id.seekBar);
        songNameTv = findViewById(R.id.SongTitle);

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        favoritesList = new ArrayList<>();
        loadFavorites();

        songNameTv.setText(songName);
        checkIfFavorite();
        initializeMediaPlayer();

        playBtn.setOnClickListener(v -> togglePlayPause());
        prevBtn.setOnClickListener(v -> playPreviousSong());
        nextBtn.setOnClickListener(v -> playNextSong());
        likeBtn.setOnClickListener(v -> toggleFavorite());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        updateSeekBar();
    }
    private void initializeMediaPlayer() {
        executorService.execute(() -> {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(songPath);
                mediaPlayer.prepare();
                mainHandler.post(() -> {
                    seekBar.setMax(mediaPlayer.getDuration());
                    playBtn.setImageResource(R.drawable.pause);
                    isPlaying = true;
                    mediaPlayer.start();
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(PlayerActivity.this,
                        "Error playing song", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
                playBtn.setImageResource(R.drawable.play);
            } else {
                mediaPlayer.start();
                playBtn.setImageResource(R.drawable.pause);
            }
            isPlaying = !isPlaying;
        }
    }
    private void playSongAtPosition() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
        songPath = songPaths.get(position);
        songName = new File(songPath).getName().replace(".mp3", "");
        songNameTv.setText(songName);
        initializeMediaPlayer();
        checkIfFavorite();
    }
    private void playPreviousSong() {
        Toast.makeText(this, "Playing previous song", Toast.LENGTH_SHORT).show();
        if (position > 0) {
            position--;
        } else {
            position = songPaths.size() - 1;
        }
        playSongAtPosition();
    }

    private void playNextSong() {
        Toast.makeText(this, "Playing next song", Toast.LENGTH_SHORT).show();
        if (position < songPaths.size() - 1) {
            position++;
        } else {
            position = 0;
        }
        playSongAtPosition();
    }

    private static final String PREFS_NAME = "favorites_prefs";

    private void toggleFavorite() {
        executorService.execute(() -> {
            isFavorite = !isFavorite;
            mainHandler.post(() -> {
                likeBtn.setImageResource(isFavorite ? R.drawable.favorites1 : R.drawable.favorites);
                saveFavoriteState(songPath, isFavorite);
            });
        });
    }
    private void checkIfFavorite() {
        executorService.execute(() -> {
            isFavorite = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getBoolean(songPath, false);
            mainHandler.post(() -> {
                likeBtn.setImageResource(isFavorite ? R.drawable.favorites1 : R.drawable.favorites);
            });
        });
    }
    private void saveFavoriteState(String path, boolean favorite) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(path, favorite)
                .apply();
    }
    private void loadFavorites() {
    }
    private void updateSeekBar() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                }
                mainHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.popup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuButton) {
            showPopupMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.menuButton));
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_delete) {
                deleteCurrentSong();
                return true;
            } else if (id == R.id.action_rename) {
                renameCurrentSong();
                return true;
            } else if (id == R.id.action_favorite) {
                toggleFavorite();
                return true;
            }
            return false;
        });
        popup.show();
    }
    private void deleteCurrentSong() {
        executorService.execute(() -> {
            File file = new File(songPath);
            boolean deleted = file.delete();

            mainHandler.post(() -> {
                if (deleted) {
                    Toast.makeText(PlayerActivity.this,
                            "Song deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PlayerActivity.this,
                            "Failed to delete song", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private void renameCurrentSong() {
        Toast.makeText(this, "Rename functionality", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        executorService.shutdown();
    }
}

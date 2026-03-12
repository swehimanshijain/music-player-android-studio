package com.example.playmusic;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String path;
    private String title;
    private boolean isFavorite;

    public Song(String path, String title) {
        this.path = path;
        this.title = title;
        this.isFavorite = false;
    }

    protected Song(Parcel in) {
        path = in.readString();
        title = in.readString();
        isFavorite = in.readByte() != 0;
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getPath() {
        return path;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(title);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
    }
}
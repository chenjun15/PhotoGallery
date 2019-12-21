package com.cj.photogallery;

import androidx.annotation.NonNull;

public class GalleryItem {
    private String mCation;
    private String mId;
    private String mUrl;

    @NonNull
    @Override
    public String toString() {
        return mCation;
    }

    public String getCation() {
        return mCation;
    }

    public void setCation(String cation) {
        mCation = cation;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}

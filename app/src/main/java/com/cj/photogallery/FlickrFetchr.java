package com.cj.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "c5419f7a649aa47f43d5a82e945f9eb9";

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
            return out.toByteArray();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems() {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            Log.d(TAG, "fetchItems: url=" + url);
            String jsonString = getUrlString(url);
            Log.i(TAG, "fetchItems: Received JSON: " + jsonString);
            gsonParseItems(items, jsonString);
        } catch (IOException ioe) {
            Log.e(TAG, "fetchItems: Failed to fetch items", ioe);
        }

        return items;
    }

    private void gsonParseItems(List<GalleryItem> items, final String jsonString) {
        GeneralInfo info = new Gson().fromJson(jsonString, GeneralInfo.class);
        for (Photo photo : info.photos.photo) {
            GalleryItem item = new GalleryItem();
            item.setId(photo.id);
            item.setCaption(photo.title);
            if (photo.url_s == null)
                continue;
            item.setUrl(photo.url_s);
            items.add(item);
        }
    }

    public class GeneralInfo {
        Photos photos;
        String stat;
    }

    public class Photos {
        int page;
        int pages;
        int perpage;
        int total;
        List<Photo> photo;
    }

    public class Photo {
        String id;
        String owner;
        String secret;
        String server;
        int farm;
        String title;
        int ispublic;
        int isfriend;
        int isfamily;
        String url_s;
        int height_s;
        int width_s;
    }
}

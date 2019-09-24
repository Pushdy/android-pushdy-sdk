package com.pushdy.handlers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class PDYDownloadImageHandler extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public PDYDownloadImageHandler(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap result = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            result = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Pushdy", "PDYDownloadImageHandler error: " +e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}
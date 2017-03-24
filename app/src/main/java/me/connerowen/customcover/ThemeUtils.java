package me.connerowen.customcover;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v7.graphics.Palette;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.robv.android.xposed.XposedBridge;

public class ThemeUtils {
    MyCallBack mCallBack = null;

    public void saveAlbumArt(Bitmap art, String album) {

        new AsyncCaller().execute(art, album);

    }

    public Bitmap drawMultipleBitmapsOnImageView(Bitmap art, Bitmap bg, Bitmap overlay, Bitmap mask, int left, int top, DisplayMetrics displayMetrics, int DPI, String album) {
        Bitmap drawnBitmap = null;

        new AsyncCaller().execute(art, album);

        try {
            switch (displayMetrics.densityDpi) {
                case DisplayMetrics.DENSITY_560:
                    // phones like nexus 6p & note 4 (2560x1440 most of the time)
                    drawnBitmap = Bitmap.createBitmap(1440, 1253, Bitmap.Config.ARGB_8888); //width x height
                    break;
                case DisplayMetrics.DENSITY_XXHIGH:
                    //phones like galaxy s5 (1920x1080 most of the time) //65 top 74 left
                    drawnBitmap = Bitmap.createBitmap(1080, 940, Bitmap.Config.ARGB_8888); //width x height
                    break;
                case DisplayMetrics.DENSITY_XHIGH:
                    // phones like galaxy s3 (1280x720 most of the time)
                    drawnBitmap = Bitmap.createBitmap(720, 627, Bitmap.Config.ARGB_8888); //width x height
                    break;
            }


            Canvas canvas = new Canvas(drawnBitmap);
            /*
            TextPaint songInfo = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            songInfo.setColor(textColor); // Text Color
            songInfo.setStrokeWidth(30); // Text Size
            songInfo.setTextSize(50);
            songInfo.setTextAlign(Paint.Align.CENTER);
            songInfo.setTypeface(Typeface.DEFAULT);
            songInfo.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER)); // Text Overlapping Pattern

            String currentTime = DateFormat.getTimeInstance().format(new Date(0));
            TextPaint clock = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            clock.setColor(textColor);
            clock.setTextSize(170);
            //clock.setShadowLayer(5, 2, 2, Color.BLACK);
            clock.setTextAlign(Paint.Align.CENTER);
            clock.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER)); // Text Overlapping Pattern
            clock.setTypeface(Typeface.DEFAULT_BOLD);
            */

            //left, top
            if (bg != null)
            {
                canvas.drawBitmap(bg, 0, 0, null); // vinyl bg
            }
            canvas.drawBitmap(art, left, top, null); // album art (100, 72 for vinyl)
            if (mask != null)
            {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                canvas.drawBitmap(mask, left, top, paint);
            }
            if (overlay != null)
            {
                canvas.drawBitmap(overlay, 0, 0, null); // overlay
            }

            //x, y
            //canvas.drawText(song, 700, 700, songInfo);
            //canvas.drawText(artist, 700, 770, songInfo);
            //canvas.drawText(album, 700, 840, songInfo);

            //canvas.drawText(currentTime, 700, 300, clock);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return drawnBitmap;
    } //end draw

    private class AsyncCaller extends AsyncTask<Object, Void, Boolean>
    {
        @Override
        protected void onPreExecute() {}

        @Override
        protected Boolean doInBackground(Object... params) {

            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            Bitmap art = (Bitmap) params[0];
            String album = (String) params[1];
            storeImage(art, album);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("CustomCover", "task finished, result was:" + result.toString());
        }

    }

    private static void storeImage(Bitmap image, String album) {
        //String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        //File file = new File(extStorageDirectory + "/cover.png");
        //Log.d("CustomCover", "store: " + file.getAbsolutePath());
        /*if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, "cover.png");
            Log.d("CustomCover", "file exists");
        }
        if (file == null) {
            Log.d("CustomCover",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        Log.d("CustomCover", "we made it this far"); */

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/me.connerowen.customcover/Files");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            XposedBridge.log("mkdir success: " +  mediaStorageDir.mkdirs());
        }

        File mediaFile;
        String mImageName = "cover_" + album + ".png";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        XposedBridge.log("file name: " + mediaFile.getName());

        if (mediaFile.exists()) {
            XposedBridge.log("file " + mediaFile.getName() + " already exists, not saving");
            return;
        }
        XposedBridge.log("file does not exist so we're in a new album, saving and deleting any old files");

        File[] listOfFiles = mediaStorageDir.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                if (mediaFile.getName().equals(listOfFiles[i].getName())) {
                    XposedBridge.log("current file is mediafile, do not delete");
                    break;
                }
                else {
                    XposedBridge.log("deleted file success: " + listOfFiles[i].delete());
                }
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(mediaFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("CustomCover", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("CustomCover", "Error accessing file: " + e.getMessage());
        }
    }

    public Palette generatePaletteFromAlbum() {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/Android/data/me.connerowen.customcover/Files";
        String fileThatYouWantToFilter;
        File artFileDir = new File(extStorageDirectory);
        File[] listOfFiles = artFileDir.listFiles();
        File artFile = null;
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {

                if (listOfFiles[i].isFile()) {
                    fileThatYouWantToFilter = listOfFiles[i].getName();
                    if (fileThatYouWantToFilter.startsWith("cover_") && fileThatYouWantToFilter.endsWith(".png")) {
                        XposedBridge.log("found" + " " + fileThatYouWantToFilter);
                        artFile = listOfFiles[i];
                    }
                }
            }
        }
        if (artFile != null) {
            Bitmap artwork = BitmapFactory.decodeFile(artFile.getAbsolutePath());
            Palette p = Palette.from(artwork).generate();
            return p;
        }
        return null;
    }

    public interface MyCallBack
    {
        // Declaration of the template function for the interface
        public Boolean saveAlbumArt(Bitmap art);
        //public void updateAlbumBG(int bg);
    }

    public void update(Bitmap art, Palette p) {
        Log.d("CustomCover", "We're in update");
        //Boolean updated = this.mCallBack.updateAlbumArt(art);
        MainActivity obj = new MainActivity();
        ImageView coverArt = obj.getImageView();
        if (coverArt != null) {
            Log.d("CustomCover", "Update test was successful");
            coverArt.setImageBitmap(art);
            coverArt.setBackgroundColor(p.getMutedColor(0xFF000000));
            return;
        }
        Log.d("CustomCover", "Update test failed");
    }

}

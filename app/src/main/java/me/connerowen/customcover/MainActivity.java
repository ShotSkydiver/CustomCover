package me.connerowen.customcover;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Theme {
    public String themename;
    public String themebg;
    public Boolean hasoverlay = Boolean.FALSE;
    public String themeoverlay;
    public Boolean hasmask = Boolean.FALSE;
    public String thememask;

    public String albumartsize;
    public String leftpadding;
    public String toppadding;
}
//public class MainActivity extends PreferenceActivity {
public class MainActivity extends AppCompatActivity {
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static TextView vibrantText = null;
    public static TextView vibrantDarkText = null;
    public static TextView vibrantLightText = null;
    public static TextView mutedText = null;
    public static TextView mutedDarkText = null;
    public static TextView mutedLightText = null;

    public static ImageView coverPreview = null;


    private static ArrayList<Theme> themes = null;
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        //addPreferencesFromResource(R.xml.preferences);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);

        vibrantText = (TextView) findViewById(R.id.vibrant);
        vibrantDarkText = (TextView) findViewById(R.id.vibrantDark);
        vibrantLightText = (TextView) findViewById(R.id.vibrantLight);
        mutedText = (TextView) findViewById(R.id.muted);
        mutedDarkText = (TextView) findViewById(R.id.mutedDark);
        mutedLightText = (TextView) findViewById(R.id.mutedLight);

        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(vibrantDarkText.getLayoutParams());
        LinearLayout swatchesLayout = (LinearLayout) findViewById(R.id.swatches);

        coverPreview = (ImageView) findViewById(R.id.coverArt);

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
                        artFile = listOfFiles[i];
                    }
                }
            }
        }
        Bitmap artwork = BitmapFactory.decodeFile(artFile.getAbsolutePath());
        Palette palette = Palette.from(artwork).generate();

        if (artwork != null) {
            coverPreview.setImageBitmap(artwork);
            coverPreview.setBackgroundColor(palette.getMutedColor(0xFF000000));
            Log.d("CustomCover", "artwork not null");
        }

        if (palette != null)
        {
            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            List<Palette.Swatch> swatches = palette.getSwatches();

        if (vibrantSwatch == null) {
            vibrantText.setText("Vibrant swatch is null");
        } else {
            vibrantText.setTextColor(vibrantSwatch.getTitleTextColor());
            vibrantText.setText("This is vibrant swatch, RGB: " + vibrantSwatch.getRgb());
            vibrantText.setBackgroundColor(palette.getVibrantColor(0xFF000000));
        }

        Palette.Swatch vibrantDarkSwatch = palette.getDarkVibrantSwatch();
        if (vibrantDarkSwatch == null) {
            vibrantDarkText.setText("Vibrant dark swatch is null");
        } else {
            vibrantDarkText.setTextColor(vibrantDarkSwatch.getTitleTextColor());
            vibrantDarkText.setText("This is vibrant dark swatch, RGB: " + vibrantDarkSwatch.getRgb());
            vibrantDarkText.setBackgroundColor(palette.getDarkVibrantColor(0xFF000000));
        }

        Palette.Swatch vibrantLightSwatch = palette.getLightVibrantSwatch();
        if (vibrantLightSwatch == null) {
            vibrantLightText.setText("Vibrant light swatch is null");
        } else {
            vibrantLightText.setTextColor(vibrantLightSwatch.getTitleTextColor());
            vibrantLightText.setBackgroundColor(palette.getLightVibrantColor(0xFF000000));
        }

        Palette.Swatch mutedSwatch = palette.getMutedSwatch();
        if (mutedSwatch == null) {
            mutedText.setText("Muted swatch is null");
        } else {
            mutedText.setTextColor(mutedSwatch.getTitleTextColor());
            mutedText.setBackgroundColor(palette.getMutedColor(0xFF000000));
        }

        Palette.Swatch mutedDarkSwatch = palette.getDarkMutedSwatch();
        if (mutedDarkSwatch == null) {
            mutedDarkText.setText("Muted dark swatch is null");
        } else {
            mutedDarkText.setTextColor(mutedDarkSwatch.getTitleTextColor());
            mutedDarkText.setBackgroundColor(palette.getDarkMutedColor(0xFF000000));
        }

        Palette.Swatch mutedLightSwatch = palette.getLightMutedSwatch();
        if (mutedLightSwatch == null) {
            mutedLightText.setText("Muted light swatch is null");
        } else {
            mutedLightText.setTextColor(mutedLightSwatch.getTitleTextColor());
            mutedLightText.setBackgroundColor(palette.getLightMutedColor(0xFF000000));
        }

            int[] rgbs = new int[swatches.size()];

            for (int i=0; i<swatches.size(); i++) {
                Palette.Swatch thisSwatch = swatches.get(i);
                TextView thisSwatchText = new TextView(this);
                float[] hsl = thisSwatch.getHsl();
                float hue = hsl[0];
                StringBuilder sb = new StringBuilder();
                int rgb = thisSwatch.getRgb();
                int r = (rgb >> 16) & 0xFF;
                sb.append(r);
                int g = (rgb >> 8) & 0xFF;
                sb.append(g);
                int b = rgb & 0xFF;
                sb.append(b);
                String colorHex = sb.toString();
                rgbs[i] = Integer.parseInt(colorHex);
                thisSwatchText.setText("Swatch #" + i + ", Population: " + thisSwatch.getPopulation() + ", RGB: #" + colorHex + ", hue: " + hue);
                thisSwatchText.setTextColor(thisSwatch.getTitleTextColor());
                thisSwatchText.setBackgroundColor(thisSwatch.getRgb());
                thisSwatchText.setLayoutParams(colorParams);
                thisSwatchText.setPadding(10,10,10,10);
                swatchesLayout.addView(thisSwatchText);
            }
            Arrays.sort(rgbs);
            int lowestColor = rgbs[0];
            int highestColor = rgbs[rgbs.length - 1];
            int diff = highestColor - lowestColor;
            vibrantText.setText("highest: " + highestColor + " lowest: " + lowestColor + " diff: " + diff);
        }



        LinearLayout fragContainer = (LinearLayout) findViewById(R.id.llFragmentContainer);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);


        ll.setId(View.generateViewId());



        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .add(ll.getId(), new MainActivityFragment())
                .commit();

        fragContainer.addView(ll);



        //coverPreview = (ImageView) findViewById(R.id.coverPreview);
    } //end oncreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            Intent intent = getIntent();
            //overridePendingTransition(0, 0);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MainActivityFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            verifyStoragePermissions(getActivity());


            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.preferences);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

            //ImageView ivSubmarine = (ImageView)view.findViewById(R.id.iv_submarine);

            final ListPreference listPreference = (ListPreference) findPreference("theme");

            Log.d("CustomCover", "root: " + Environment.getExternalStorageDirectory().getAbsolutePath()); // /storage/emulated/0

            String path = Environment.getExternalStorageDirectory().toString()+"/Themes";

            Log.d("CustomCover", "path dir: " + path);
            File f = new File(path);
            final File files[] = f.listFiles();
            //Log.d("CustomCover", "Size: " + files.length);

            //setListPreferenceData(listPreference, files);



            /*File coverArt = new File("/sdcard/Themes/cover.png");
            if (coverArt.exists()) {
                Bitmap cover = BitmapFactory.decodeFile(coverArt.getAbsolutePath());
                coverPreview.setImageBitmap(cover);
            }*/

            /*listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    setListPreferenceData(listPreference, files);
                    return false;
                }
            }); */
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newTheme = (String) newValue;
                    Log.d("CustomCover", "New pref value: " + newTheme);
                    //updatePreferences(listPreference, newTheme, getActivity());
                    Toast.makeText(getActivity(), ("Theme successfully changed to " + newTheme), Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        }
        /*@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View v =  inflater.inflate(R.layout.fragment, container, false);

            ((TextView) v.findViewById(R.id.tvFragText)).setText(getArguments().getString("text"));
            return v;
        } */
    }

    public ImageView getImageView() {
        if (coverPreview != null) {
            ImageView imgView = (ImageView) findViewById(R.id.coverArt);
            Log.d("CustomCover", "coverPreview not null");
            return imgView;
        }
        else {
            Log.d("CustomCover", "coverPreview is null, app hasn't been launched yet");
            return null;
        }
    }

    /*@Override
    public Boolean updateAlbumArt(Bitmap art) {
        if (coverPreview != null) {
            ImageView imgView = (ImageView) findViewById(R.id.coverArt);
            imgView.setImageBitmap(art);
            Log.d("CustomCover", "coverPreview not null");
            return true;
        }
        else {
            Log.d("CustomCover", "coverPreview is null, app hasn't been launched yet");
            return false;
        }
    }

    @Override
    public void updateAlbumBG(int bg) {
        ImageView imgView = (ImageView) findViewById(R.id.coverArt);
        imgView.setBackgroundColor(bg);
    }*/

    public static String updateColors(Bitmap artwork2, Palette p) {
        if (artwork2 == null) {
            return "Artwork is null";
        }
        if (p == null) {
            return "Palette is null";
        }
        //coverPreview.setImageBitmap(artwork2);
        //coverPreview.setBackgroundColor(p.getMutedColor(0xFF000000));

        //artwork = artwork2;
        //bgColor = p.getMutedColor(0xFF000000);

        //p2 = p;

        /*
        Palette.Swatch vibrantSwatch = p.getVibrantSwatch();
        if (vibrantSwatch == null) {
            vibrantText.setText("Vibrant swatch is null");
        }
        else {
            vibrantText.setTextColor(vibrantSwatch.getTitleTextColor());
            vibrantText.setBackgroundColor(p.getVibrantColor(0xFF000000));
        }

        Palette.Swatch vibrantDarkSwatch = p.getDarkVibrantSwatch();
        if (vibrantDarkSwatch == null)
        {
            vibrantDarkText.setText("Vibrant dark swatch is null");
        }
        else {
            vibrantDarkText.setTextColor(vibrantDarkSwatch.getTitleTextColor());
            vibrantDarkText.setBackgroundColor(p.getDarkVibrantColor(0xFF000000));
        }

        Palette.Swatch vibrantLightSwatch = p.getLightVibrantSwatch();
        if (vibrantLightSwatch == null) {
            vibrantLightText.setText("Vibrant light swatch is null");
        }
        else {
            vibrantLightText.setTextColor(vibrantLightSwatch.getTitleTextColor());
            vibrantLightText.setBackgroundColor(p.getLightVibrantColor(0xFF000000));
        }

        Palette.Swatch mutedSwatch = p.getMutedSwatch();
        if (mutedSwatch == null) {
            mutedText.setText("Muted swatch is null");
        } else {
            mutedText.setTextColor(mutedSwatch.getTitleTextColor());
            mutedText.setBackgroundColor(p.getMutedColor(0xFF000000));
        }

        Palette.Swatch mutedDarkSwatch = p.getDarkMutedSwatch();
        if (mutedDarkSwatch == null) {
            mutedDarkText.setText("Muted dark swatch is null");
        }
        else {
            mutedDarkText.setTextColor(mutedDarkSwatch.getTitleTextColor());
            mutedDarkText.setBackgroundColor(p.getDarkMutedColor(0xFF000000));
        }

        Palette.Swatch mutedLightSwatch = p.getLightMutedSwatch();
        if (mutedLightSwatch == null) {
            mutedLightText.setText("Muted light swatch is null");
        }
        else {
            mutedLightText.setTextColor(mutedLightSwatch.getTitleTextColor());
            mutedLightText.setBackgroundColor(p.getLightMutedColor(0xFF000000));
        }
        */



        return "Success";

    }

    protected static void setListPreferenceData(ListPreference lp, File[] files) {
        List<String> listItems = new ArrayList<String>();
        themes = getXMLs(files);

        for (int i = 0; i < themes.size(); i++) {
            listItems.add(themes.get(i).themename);
        }

        final CharSequence[] charSequenceItems = listItems.toArray(new CharSequence[listItems.size()]);
        Log.d("CustomCover", "listItems size: " + listItems.size());
        lp.setEntries(charSequenceItems);
        lp.setDefaultValue("Simple");
        lp.setEntryValues(charSequenceItems);
    }

    private static void updatePreferences(ListPreference lp, String newTheme, Context context) {
        //String themeName = lp.getValue();
        lp.setValue(newTheme);
        Theme currentTheme = null;
        for (int i = 0; i < themes.size(); i++) {
            if (newTheme.equals(themes.get(i).themename)) {
                currentTheme = themes.get(i);
            }
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("theme", currentTheme.themename).commit();
        editor.putString("theme_bg", currentTheme.themebg).commit();

        if (currentTheme.hasoverlay) {
            editor.putString("theme_overlay", currentTheme.themeoverlay).commit();
            editor.putString("hasoverlay", "TRUE").commit();
        }
        else {
            editor.putString("theme_overlay", "null").commit();
        }
        if (currentTheme.hasmask) {
            editor.putString("theme_mask", currentTheme.thememask).commit();
            editor.putString("hasmask", "TRUE").commit();
        }
        else {
            editor.putString("theme_mask", "null").commit();
        }
        editor.putString("artsize", currentTheme.albumartsize).commit();
        editor.putString("left_padding", currentTheme.leftpadding).commit();
        editor.putString("top_padding", currentTheme.toppadding).commit();


    }


    private static ArrayList<Theme> getXMLs(File[] files) {
        //File themesXML = new File("/sdcard/Themes/themes.xml");
        String themeXML = (Environment.getExternalStorageDirectory().toString() + "/Themes/" + files[1].getName() + "/theme.xml");
        Log.d("CustomCover", "themeXML: " + themeXML);
        Log.d("CustomCover", "absolute path: " + files[1].getAbsolutePath());

        //mergeFiles(files, themesXML);

        ArrayList<Theme> themes = new ArrayList();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                File temp = new File(files[i].getAbsolutePath() + "/theme.xml");
                Log.d("CustomCover", "temp File Path: " + temp.getAbsolutePath() + "array path: " + files[i].getAbsolutePath());
                themes.add(readFromXML(temp));
                Log.d("CustomCover", "Theme added: " + files[i].getName());
            } else if (!files[i].isDirectory()) {
                Log.d("CustomCover", "This is not a folder: " + files[i].getName());
            }
        }
        //ArrayList<Theme> themes = readFromXML(themesXML);
        return themes;
    }


    private static Theme readFromXML(File xmlFile) {
        //ArrayList<Theme> themes = new ArrayList();
        Theme currentTheme = null;
        try {
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            xppf.setNamespaceAware(true);
            XmlPullParser xpp = xppf.newPullParser();

            FileInputStream input = new FileInputStream(xmlFile);
            xpp.setInput(input, null);

            //Theme currentTheme = null;

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("theme")) {
                        //items.add(xpp.getAttributeValue(0));
                        currentTheme = new Theme();

                        eventType = xpp.next();
                    }
                    else if (currentTheme != null) {
                        if (xpp.getName().equals("themename")) {

                            eventType = xpp.next(); //advance to text
                            currentTheme.themename = xpp.getText();
                            eventType = xpp.next(); //advance to end tag
                        }
                        else if (xpp.getName().equals("themebackground")) {
                            eventType = xpp.next(); //advance to text
                            currentTheme.themebg = xpp.getText();
                            eventType = xpp.next(); //advance to end tag
                        }
                        else if (xpp.getName().equals("themeoverlay")) {
                            eventType = xpp.next(); //advance to text
                            currentTheme.themeoverlay = xpp.getText();
                            currentTheme.hasoverlay = Boolean.TRUE;
                            eventType = xpp.next(); //advance to end tag
                        }
                        else if (xpp.getName().equals("thememask")) {
                            eventType = xpp.next(); //advance to text
                            currentTheme.thememask = xpp.getText();
                            currentTheme.hasmask = Boolean.TRUE;
                            eventType = xpp.next(); //advance to end tag
                        }
                        else if (xpp.getName().equals("albumartsize")) {
                            eventType = xpp.next(); //advance to text
                            currentTheme.albumartsize = xpp.getText();
                            eventType = xpp.next(); //advance to end tag
                        }
                        else if (xpp.getName().equals("leftpadding")) {
                            eventType = xpp.next(); //advance to text
                            currentTheme.leftpadding = xpp.getText();
                            eventType = xpp.next(); //advance to end tag
                        }
                        else if (xpp.getName().equals("toppadding")) {
                            eventType = xpp.next(); //advance to text
                            currentTheme.toppadding = xpp.getText();
                            eventType = xpp.next(); //advance to end tag
                        }
                        if (xpp.getName().equals("theme")) {
                            //this should be the end of the theme
                            return currentTheme;
                        }
                        eventType = xpp.next();
                    }
                }
                else {
                    eventType = xpp.next();
                }
            } // end while

        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        return currentTheme;
    }



} //end class
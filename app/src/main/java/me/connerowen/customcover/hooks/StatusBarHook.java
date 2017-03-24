package me.connerowen.customcover.hooks;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.io.File;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Target;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;



import fr.arnaudguyon.smartfontslib.FontManager;
import me.connerowen.customcover.CustomCover;
import me.connerowen.customcover.R;
import me.connerowen.customcover.ThemeUtils;
import me.connerowen.customcover.Utils;

public class StatusBarHook {
    private CustomCover mInstance;
    public int defaultColor = 0xFF000000;
    public int vibrant = 0xFFFFFFFF;
    public int vibrantRegular = 0xFFFFFFFF;
    public int muted = 0xFF000000;
    public int vibrantText = 0xFFFFFFFF;
    public int mutedText = 0xFFFFFFFF;
    public ThemeUtils themeUtil = new ThemeUtils();
    public static XSharedPreferences prefs;
    private Context context2;
    private TextView artist_text;
    //public TextView currentTime;
    //private GradientLinearLayout gradientLayout;
    public static final String INTENT_SPOTIFY_PLAYBACK_STATE_CHANGED = "com.spotify.music.playbackstatechanged";
    public static final String INTENT_SPOTIFY_METADATA_CHANGED = "com.spotify.music.metadatachanged";

    private MediaController mController;

    private TextView timeText;

    public static int deviceDPI = 0;

    public StatusBarHook(CustomCover instance, ClassLoader classLoader) {
        mInstance = instance;
        prefs = mInstance.getPrefs();

        final Class<?> PhoneStatusBar = findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
        XposedBridge.log("phonestatusbar class found");
        XposedBridge.hookAllConstructors(PhoneStatusBar, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {

            }
        });

        try {
            findAndHookMethod(PhoneStatusBar, "makeStatusBarView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    XposedBridge.log("after makestatusbarview");
                    //LinearLayout mStatusIcons = (LinearLayout) getObjectField(param.thisObject, "mStatusIcons");
                    //mInstance.setStatusIcons(mStatusIcons);
                    //mstatusbarwindow for stock ROMs
                    FrameLayout statusBarWindow = (FrameLayout) getObjectField(param.thisObject, "mStatusBarWindow");
                    XposedBridge.log("mStatusBarWindow children before change: " + statusBarWindow.getChildCount());
                    int children = statusBarWindow.getChildCount();
                    if ((children == 0) || (Utils.romIsCM13)) {
                        XposedBridge.log("mstatusbarwindow has 0 children, must be a non-stock ROM");
                        statusBarWindow = (FrameLayout) getObjectField(param.thisObject, "mStatusBarWindowContent");
                    }
                    XposedBridge.log("mStatusBarWindow children after change: " + statusBarWindow.getChildCount()); // has 2 children on CM ROM
                    for (int i = 0; i < statusBarWindow.getChildCount(); i++) {
                        View childView = statusBarWindow.getChildAt(i);
                        XposedBridge.log("found mStatusBarWindow child #" + i + ": " + statusBarWindow.getResources().getResourceName(childView.getId()));
                    }

                    FrameLayout keyguardBottomAreaView = (FrameLayout) getObjectField(param.thisObject, "mKeyguardBottomArea");
                    RelativeLayout keyguardStatusBarView = (RelativeLayout) getObjectField(param.thisObject, "mKeyguardStatusBar");
                    XposedBridge.log("mKeyguardStatusBar children: " + keyguardStatusBarView.getChildCount());
                    for (int i = 0; i < keyguardStatusBarView.getChildCount(); i++) {
                        View childView = keyguardStatusBarView.getChildAt(i);
                        XposedBridge.log("found mKeyguardStatusBar child #" + i + ": " + keyguardStatusBarView.getResources().getResourceName(childView.getId()));
                    }

                    XposedBridge.log("attempting keyguardstatusbar resize");
                    keyguardStatusBarView.setScaleY(0.9f);
                    keyguardStatusBarView.setScaleX(0.9f);
                    keyguardStatusBarView.setBottom(120);
                    //RelativeLayout.LayoutParams keyguardstatusParams = new RelativeLayout.LayoutParams(keyguardStatusBarView.getLayoutParams());
                    //keyguardstatusParams.height = 80;
                    //keyguardStatusBarView.setLayoutParams(keyguardstatusParams);
                    //XposedBridge.log("keyguardstatusbar resized");

                    RelativeLayout status_bar_expanded_header = (RelativeLayout) getObjectField(param.thisObject, "mHeader");
                    XposedBridge.log("status_bar_expanded_header children: " + status_bar_expanded_header.getChildCount());
                    for (int i = 0; i < status_bar_expanded_header.getChildCount(); i++) {
                        View childView = status_bar_expanded_header.getChildAt(i);
                        XposedBridge.log("found status_bar_expanded_header child: " + status_bar_expanded_header.getResources().getResourceName(childView.getId()));
                    }

                    LinearLayout icons = (LinearLayout) status_bar_expanded_header.getChildAt(2);
                    FrameLayout dateGroup = (FrameLayout) status_bar_expanded_header.getChildAt(4);
                    // this (v) is for stock ROM
                    //LinearLayout clockGroup = (LinearLayout) status_bar_expanded_header.getChildAt(5);
                    try {
                        LinearLayout clockGroup = (LinearLayout) status_bar_expanded_header.getChildAt(5);
                        RelativeLayout.LayoutParams clockParams = new RelativeLayout.LayoutParams(clockGroup.getLayoutParams());
                        clockParams.removeRule(RelativeLayout.ABOVE);
                        clockParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        clockParams.setMargins(0, 20, 0, 0);
                        clockParams.setMarginStart(60);
                        clockGroup.setLayoutParams(clockParams);
                        RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(dateGroup.getLayoutParams());
                        dateParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        dateParams.addRule(RelativeLayout.BELOW, clockGroup.getId());
                        dateGroup.setLayoutParams(dateParams);
                    } catch (ClassCastException exc) {
                        //XposedBridge.log("cannot cast framelayout to linearlayout, we're not on stock ROM");
                        FrameLayout clockGroup = (FrameLayout) status_bar_expanded_header.getChildAt(5);
                        RelativeLayout.LayoutParams clockParams = new RelativeLayout.LayoutParams(clockGroup.getLayoutParams());
                        clockParams.removeRule(RelativeLayout.ABOVE);
                        clockParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        clockParams.setMargins(0, 20, 0, 0);
                        clockParams.setMarginStart(60);
                        clockGroup.setLayoutParams(clockParams);
                        RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(dateGroup.getLayoutParams());
                        dateParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        dateParams.addRule(RelativeLayout.BELOW, clockGroup.getId());
                        dateGroup.setLayoutParams(dateParams);

                    }

                    FrameLayout panel_holder = null;
                    if (!Utils.romIsCM13) {
                        XposedBridge.log("ROM is stock");
                        panel_holder = (FrameLayout) statusBarWindow.getChildAt(5);
                    } else if (Utils.romIsCM13) {
                        XposedBridge.log("ROM is CM13");
                        panel_holder = (FrameLayout) statusBarWindow.getChildAt(4); // THIS IS 5 ON STOCK ROM AND 4 ON CM13
                    }

                    FrameLayout extended_status_bar = (FrameLayout) panel_holder.getChildAt(0);

                    GridLayout keyguard_status_view = (GridLayout) extended_status_bar.getChildAt(0);

                    LinearLayout keyguard_clock_container = (LinearLayout) keyguard_status_view.getChildAt(0);

                    TextView clock_view = (TextView) keyguard_clock_container.getChildAt(0);
                    mInstance.addTextLabel(clock_view);
                    LinearLayout keyguard_status_area = (LinearLayout) keyguard_clock_container.getChildAt(1);
                    TextView date_view = (TextView) keyguard_status_area.getChildAt(0);
                    mInstance.addTextLabel(date_view);

                    keyguard_clock_container.setClickable(true);



                    context2 = keyguard_status_view.getContext();

                    Context appContext = context2.createPackageContext("me.connerowen.customcover", Context.CONTEXT_IGNORE_SECURITY);
                    Resources appRes = appContext.getResources();
                    AssetManager appAsset = appContext.getAssets();

                    DisplayMetrics displayMetrics = context2.getResources().getDisplayMetrics();
                    deviceDPI = displayMetrics.densityDpi;


                    /* TODO:*************************************************** KEYGUARD CONTROLS AND THINGS *************************************** */

                    final Bitmap play2_image = Utils.getBitmapFromAsset(appAsset, "play.png");
                    final Bitmap next2_image = Utils.getBitmapFromAsset(appAsset, "next.png");
                    final Bitmap previous2_image = Utils.getBitmapFromAsset(appAsset, "previous.png");

                    Bitmap play_imageScaled = Bitmap.createScaledBitmap(play2_image, 108, 108, true);
                    Bitmap next_imageScaled = Bitmap.createScaledBitmap(next2_image, 108, 108, true);
                    Bitmap previous_imageScaled = Bitmap.createScaledBitmap(previous2_image, 108, 108, true);

                    ImageButton play = new ImageButton(context2);
                    ImageButton next = new ImageButton(context2);
                    ImageButton previous = new ImageButton(context2);

                    play.setImageBitmap(play_imageScaled);
                    next.setImageBitmap(next_imageScaled);
                    previous.setImageBitmap(previous_imageScaled);

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 86); // ORIGINAL WAS 200
                    LinearLayout.LayoutParams layoutParamsPlay = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); // ORIGINAL WAS 200
                    layoutParamsPlay.leftMargin = 145; // ORIGINALLY 120
                    layoutParamsPlay.rightMargin = 145;
                    layoutParamsPlay.gravity = Gravity.CENTER;
                    layoutParams.gravity = Gravity.CENTER;
                    play.setLayoutParams(layoutParamsPlay);
                    next.setLayoutParams(layoutParams);
                    previous.setLayoutParams(layoutParams);

                    play.setBackgroundColor(Color.TRANSPARENT);
                    next.setBackgroundColor(Color.TRANSPARENT);
                    previous.setBackgroundColor(Color.TRANSPARENT);

                    LinearLayout buttons = new LinearLayout(context2);
                    LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); // ORIGINAL WAS 170
                    buttonsParams.topMargin = 15;
                    buttonsParams.gravity = Gravity.CENTER_HORIZONTAL;
                    buttons.setLayoutParams(buttonsParams);
                    buttons.setOrientation(LinearLayout.HORIZONTAL);

                    buttons.addView(previous); //0
                    buttons.addView(play); //1
                    buttons.addView(next); //2

                    FrameLayout backdrop = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mBackdrop");

                    LinearLayout keyguardText = new LinearLayout(backdrop.getContext());
                    LinearLayout.LayoutParams text2Params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    //text2Params.topMargin = 700;
                    //text2Params.leftMargin = 500;
                    keyguardText.setLayoutParams(text2Params);
                    keyguardText.setOrientation(LinearLayout.VERTICAL);

                    artist_text = new TextView(backdrop.getContext());
                    TextView song_text = new TextView(backdrop.getContext());

                    song_text.setTextSize(17.5f); //ORIGINAL 18f
                    song_text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    song_text.setGravity(Gravity.BOTTOM);

                    song_text.setPadding(0,0,0,0); // ORIGINAL WAS 0,0,0,5
                    keyguardText.addView(song_text, 0);

                    //FrameLayout.LayoutParams artistparams = new FrameLayout.LayoutParams(1440, 2560);
                    //artist_text.setLayoutParams(artistparams);

                    artist_text.setTextSize(17.5f); //ORIGINAL 15f
                    artist_text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    artist_text.setGravity(Gravity.TOP);
                    keyguardText.addView(artist_text, 1);

                    LinearLayout durationControls = new LinearLayout(context2);
                    LinearLayout.LayoutParams durationControlParams = new LinearLayout.LayoutParams(1150, ViewGroup.LayoutParams.WRAP_CONTENT); // ORIGINAL WAS 1150, 100 //860 on s5 screen
                    durationControlParams.gravity = Gravity.CENTER_HORIZONTAL;
                    durationControls.setLayoutParams(durationControlParams);
                    durationControls.setOrientation(LinearLayout.VERTICAL); // ORIGINAL WAS HORIZONTAL

                    LinearLayout timeLayout = new LinearLayout(context2);
                    LinearLayout.LayoutParams timeLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    timeLayout.setLayoutParams(timeLayoutParams);
                    timeLayout.setOrientation(LinearLayout.HORIZONTAL);

                    LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(575, LinearLayout.LayoutParams.WRAP_CONTENT);
                    //timeParams.gravity = Gravity.CENTER_VERTICAL;

                    TextView elapsedTime = new TextView(context2);
                    elapsedTime.setTextSize(10f); // ORIGINAL WAS 12.5F
                    elapsedTime.setLayoutParams(timeParams);
                    elapsedTime.setGravity(Gravity.START);
                    elapsedTime.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    elapsedTime.setPadding(2,0,0,0);
                    timeLayout.addView(elapsedTime);

                    TextView remainingTime = new TextView(context2);
                    remainingTime.setTextSize(10f); // ORIGINAL WAS 12.5F
                    remainingTime.setLayoutParams(timeParams);
                    remainingTime.setGravity(Gravity.END);
                    remainingTime.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                    timeLayout.addView(remainingTime);

                    SeekBar positionChanger = new SeekBar(context2);
                    LinearLayout.LayoutParams positionParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); //700 width on s5 screen
                    //RelativeLayout.LayoutParams positionParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    //positionParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    positionChanger.setLayoutParams(positionParams);
                    //positionChanger.getThumb().mutate().setAlpha(0);
                    positionChanger.setPadding(5,0,0,0);
                    positionChanger.setThumb(appRes.getDrawable(R.drawable.timeseekbar_thumb));
                    //positionChanger.setProgressDrawable(appRes.getDrawable(R.drawable.rounded_seekbar));

                    durationControls.addView(positionChanger, 0);
                    durationControls.addView(timeLayout, 1);

                    LinearLayout volumeControls = new LinearLayout(context2);
                    LinearLayout.LayoutParams volControlParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 80); // ORIGINAL WAS 1155, 60 //890 on s5 screen
                    volControlParams.topMargin = 40; // originally 25
                    volControlParams.gravity = Gravity.CENTER_HORIZONTAL;
                    volumeControls.setLayoutParams(volControlParams);
                    volumeControls.setOrientation(LinearLayout.HORIZONTAL);

                    //Drawable volumeDown = appRes.getDrawable(R.drawable.ic_volume_mute_white_36dp);
                    //Drawable volumeUp = appRes.getDrawable(R.drawable.ic_volume_up_white_36dp);
                    final Bitmap volumeDown_Image = Utils.getBitmapFromAsset(appAsset, "vol_down.png");
                    final Bitmap volumeUp_Image = Utils.getBitmapFromAsset(appAsset, "vol_up.png");

                    Bitmap volumeDown_ImageScaled = Bitmap.createScaledBitmap(volumeDown_Image, 50, 43, true);
                    Bitmap volumeUp_ImageScaled = Bitmap.createScaledBitmap(volumeUp_Image, 44, 39, true);

                    ImageView volDown = new ImageView(context2);
                    volDown.setImageBitmap(volumeDown_ImageScaled);
                    ImageView volUp = new ImageView(context2);
                    volUp.setImageBitmap(volumeUp_ImageScaled);

                    LinearLayout.LayoutParams volIconsParams = new LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.MATCH_PARENT);
                    volIconsParams.gravity = Gravity.CENTER;
                    volDown.setLayoutParams(volIconsParams);
                    volUp.setLayoutParams(volIconsParams);

                    SeekBar volumeChanger = new SeekBar(context2);
                    LinearLayout.LayoutParams volumeParams = new LinearLayout.LayoutParams(870, 60); //680 on s5 screen
                    volumeParams.gravity = Gravity.CENTER;
                    volumeChanger.setLayoutParams(volumeParams);
                    //volumeChanger.getThumb().mutate().setAlpha(0);
                    volumeChanger.setThumb(appRes.getDrawable(R.drawable.sliderthumb_style3));
                    //volumeChanger.setProgressDrawable(appRes.getDrawable(R.drawable.rounded_seekbar));
                    volumeChanger.setPadding(15,0,15,0);


                    volumeControls.addView(volDown, 0);
                    volumeControls.addView(volumeChanger, 1);
                    volumeControls.addView(volUp, 2);

                    LinearLayout keyguardControls = new LinearLayout(context2);
                    LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    controlParams.topMargin = 100; // ORIGINAL WAS 150
                    controlParams.bottomMargin = 20;
                    controlParams.gravity = Gravity.CENTER_HORIZONTAL;
                    keyguardControls.setLayoutParams(controlParams);
                    keyguardControls.setOrientation(LinearLayout.VERTICAL);

                    keyguardControls.addView(durationControls, 0);
                    keyguardControls.addView(keyguardText, 1);
                    keyguardControls.addView(buttons, 2);
                    keyguardControls.addView(volumeControls, 3);


                    keyguard_clock_container.addView(keyguardControls);
                    keyguardControls.setVisibility(View.GONE);
                    /* TODO:*************************************************** END OF KEYGUARD THINGS *************************************** */

                    //currentTime = new TextView(context2);
                    //gradientLayout = new GradientLinearLayout()
                    //backdrop.addView(gradientLayout, 0);
                    //backdropGradient = new GradientLinearLayout(context2);


                    /* TODO:*************************************************** EXPANDED STATUS BAR CONTROLS AND THINGS *************************************** */
                    FrameLayout notification_panel = (FrameLayout) getObjectField(param.thisObject, "mNotificationPanel");
                    FrameLayout notification_container_parent = (FrameLayout) notification_panel.getChildAt(1);
                    ScrollView scroll_view = (ScrollView) notification_container_parent.getChildAt(0);
                    LinearLayout layoutToModify = (LinearLayout) scroll_view.getChildAt(0);
                    FrameLayout qs_panel = (FrameLayout) layoutToModify.getChildAt(0);

                    Context header_context = status_bar_expanded_header.getContext();

                    ImageButton play_expanded = new ImageButton(header_context);
                    ImageButton next_expanded = new ImageButton(header_context);
                    ImageButton previous_expanded = new ImageButton(header_context);

                    play_expanded.setImageBitmap(play2_image);
                    next_expanded.setImageBitmap(next2_image);
                    previous_expanded.setImageBitmap(previous2_image);

                    LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 64); // ORIGINALLY 120
                    LinearLayout.LayoutParams layoutParamsPlay2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); // ORIGINALLY 120
                    layoutParamsPlay2.leftMargin = 75;
                    layoutParamsPlay2.rightMargin = 75;
                    layoutParamsPlay2.gravity = Gravity.TOP;
                    layoutParams2.gravity = Gravity.CENTER;

                    play_expanded.setLayoutParams(layoutParamsPlay2);
                    next_expanded.setLayoutParams(layoutParams2);
                    previous_expanded.setLayoutParams(layoutParams2);

                    play_expanded.setBackgroundColor(Color.TRANSPARENT);
                    next_expanded.setBackgroundColor(Color.TRANSPARENT);
                    previous_expanded.setBackgroundColor(Color.TRANSPARENT);

                    play_expanded.setColorFilter(0xFFEBEBEB, PorterDuff.Mode.SRC_ATOP);
                    next_expanded.setColorFilter(0xFFEBEBEB, PorterDuff.Mode.SRC_ATOP);
                    previous_expanded.setColorFilter(0xFFEBEBEB, PorterDuff.Mode.SRC_ATOP);

                    TextView songTitle = new TextView(header_context);
                    //songTitle.setId(appRes.getIdentifier("song_title", "id", appContext.getPackageName()));
                    songTitle.setText("Song Title");
                    songTitle.setTextColor(0xFFEBEBEB);
                    songTitle.setTextSize(16.5f); //original 17f
                    //songTitle.setShadowLayer();
                    songTitle.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                    songTitle.setPadding(20,0,0,5);
                    //songTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    //songparams.gravity = Gravity.CENTER_HORIZONTAL;
                    songTitle.setLayoutParams(textParams);

                    TextView artistName = new TextView(header_context);
                    artistName.setText("Artist Name");
                    artistName.setTextColor(0xFFEBEBEB);
                    artistName.setTextSize(14f);//original 15f
                    artistName.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                    artistName.setPadding(20,0,0,0);
                    //artistName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    artistName.setLayoutParams(textParams);

                    TextView albumName = new TextView(header_context);
                    albumName.setText("Album Name");
                    albumName.setTextColor(0xFFEBEBEB);
                    albumName.setTextSize(14f);//original 15f
                    albumName.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                    albumName.setPadding(20,0,0,0);
                    //albumName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    albumName.setLayoutParams(textParams);

                    RelativeLayout.LayoutParams buttonsExpParams = null;
                    LinearLayout.LayoutParams wrapperParams = null;
                    RelativeLayout.LayoutParams controlsParams = null;
                    RelativeLayout.LayoutParams albumParams = null;
                    switch (displayMetrics.densityDpi) {
                        case DisplayMetrics.DENSITY_560:
                            XposedBridge.log("560 dpi test");
                            buttonsExpParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 155); // ORIGINAL WAS 380
                            wrapperParams = new LinearLayout.LayoutParams(900, 700);
                            controlsParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 780);
                            albumParams = new RelativeLayout.LayoutParams(680, 680);
                            break;
                        case DisplayMetrics.DENSITY_XXHIGH:
                            XposedBridge.log("xxhigh dpi test");
                            buttonsExpParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                            wrapperParams = new LinearLayout.LayoutParams(470, 360);
                            controlsParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 400);
                            albumParams = new RelativeLayout.LayoutParams(360, 360);
                            break;
                        case DisplayMetrics.DENSITY_XHIGH:
                            XposedBridge.log("xhigh dpi test");
                            buttonsExpParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 380);
                            wrapperParams = new LinearLayout.LayoutParams(650, 700);
                            controlsParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 780);
                            albumParams = new RelativeLayout.LayoutParams(680, 680);
                            break;
                    }

                    LinearLayout buttons_expanded = new LinearLayout(header_context);
                    //buttons_expanded.setId(appRes.getIdentifier("buttons_expanded", "id", appContext.getPackageName()));

                    //buttonsExpParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    //buttonsExpParams.setMargins(0,30,0,0);
                    buttons_expanded.setPadding(20,0,0,0); // ORIGINALLY 20,20,0,0
                    buttons_expanded.setLayoutParams(buttonsExpParams);
                    buttons_expanded.setOrientation(LinearLayout.HORIZONTAL);

                    buttons_expanded.addView(previous_expanded); //0
                    buttons_expanded.addView(play_expanded); //1
                    buttons_expanded.addView(next_expanded); //2

                    LinearLayout controlsAndTextWrapper = new LinearLayout(header_context);

                    controlsAndTextWrapper.setLayoutParams(wrapperParams);
                    controlsAndTextWrapper.setOrientation(LinearLayout.VERTICAL);

                    controlsAndTextWrapper.addView(songTitle, 0);
                    controlsAndTextWrapper.addView(artistName, 1);
                    controlsAndTextWrapper.addView(albumName, 2);
                    controlsAndTextWrapper.addView(buttons_expanded, 3);


                    LinearLayout controls_header = new LinearLayout(header_context);

                    controlsParams.addRule(RelativeLayout.BELOW, icons.getId());
                    controls_header.setLayoutParams(controlsParams);
                    controls_header.setElevation(16f);
                    controls_header.setOrientation(LinearLayout.HORIZONTAL);
                    controls_header.setPadding(15,20,0,5);
                    controls_header.setBackgroundColor(0xFF263238);


                    ImageView albumPreview = new ImageView(header_context);
                    //albumPreview.setId(appRes.getIdentifier("album_preview", "id", appContext.getPackageName()));
                    //Bitmap album_image_temp = BitmapFactory.decodeFile(albumArt2.getAbsolutePath());


                    albumPreview.setLayoutParams(albumParams);

                    controls_header.addView(controlsAndTextWrapper, 0);
                    controls_header.addView(albumPreview, 1);

                    controls_header.setClickable(true);
                    status_bar_expanded_header.addView(controls_header, 4);
                    /* TODO:*************************************************** END EXPANDED STATUSBAR THINGS *************************************** */

                    Context thisContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    //Context context = phoneStatusBar.getContext();
                    IntentFilter spotifyFilter = new IntentFilter();
                    spotifyFilter.addAction(INTENT_SPOTIFY_PLAYBACK_STATE_CHANGED);
                    spotifyFilter.addAction(INTENT_SPOTIFY_METADATA_CHANGED);
                    thisContext.registerReceiver(new BroadcastReceiver() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            XposedBridge.log("spotify intent received");
                            //MediaSessionManager mediaSessionManager = (MediaSessionManager) XposedHelpers.getObjectField(param.thisObject, "mMediaSessionManager");
                            //MediaController mediaController = (MediaController) XposedHelpers.getObjectField(param.thisObject, "mMediaController");
                            //MediaController.Callback mediaListener = (MediaController.Callback) XposedHelpers.getObjectField(param.thisObject, "mMediaListener");
                            MediaMetadata metadata = (MediaMetadata) XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata");
                            if (metadata != null) {
                                XposedBridge.log("metadata not null, calling updatemediamatadata method");
                                //XposedHelpers.callMethod(param.thisObject, "updateMediaMetaData", true);
                            }
                            else {
                                XposedBridge.log("metadata is null");
                            }

                            if(intent.getAction().equals(INTENT_SPOTIFY_METADATA_CHANGED)) {
                                XposedBridge.log("spotify metadata changed");

                            }

                            if(intent.getAction().equals(INTENT_SPOTIFY_PLAYBACK_STATE_CHANGED)) {
                                XposedBridge.log("spotify playback state changed");

                            }

                            if (Utils.isKeyguardLocked(context)) {
                                // change back to white
                            }
                        }
                    }, spotifyFilter);


                    try {
                        TextView mBatteryText = (TextView) getObjectField(param.thisObject, "mBatteryText");
                        mInstance.addTextLabel(mBatteryText);
                    } catch (NoSuchFieldError e) {}

                    try {
                        TextView mOperatorTextView = (TextView) getObjectField(param.thisObject, "mOperatorTextView");
                        mInstance.addTextLabel(mOperatorTextView);
                    } catch (NoSuchFieldError e) {}
                }
            });
        } catch (NoSuchMethodError e) {}

        findAndHookMethod(PhoneStatusBar, "updateMediaMetaData", "boolean", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("We're replacing updateMediaMetaData");
                final FrameLayout mBackdrop = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mBackdrop");
                final ImageView mBackdropBack = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBackdropBack");
                final ImageView mBackdropFront = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBackdropFront");
                final MediaMetadata mMediaMetadata = (MediaMetadata) XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata");
                boolean mLaunchTransitionFadingAway = (boolean) XposedHelpers.getObjectField(param.thisObject, "mLaunchTransitionFadingAway");
                boolean mScrimSrcModeEnabled = (boolean) XposedHelpers.getObjectField(param.thisObject, "mScrimSrcModeEnabled");
                PorterDuffXfermode mSrcOverXferMode = (PorterDuffXfermode) XposedHelpers.getObjectField(param.thisObject, "mSrcOverXferMode");
                final int mState = (int) XposedHelpers.getObjectField(param.thisObject, "mState");
                boolean mKeyguardFadingAway = (boolean) XposedHelpers.getObjectField(param.thisObject, "mKeyguardFadingAway");
                long mKeyguardFadingAwayDuration = (long) XposedHelpers.getObjectField(param.thisObject, "mKeyguardFadingAwayDuration");
                long mKeyguardFadingAwayDelay = (long) XposedHelpers.getObjectField(param.thisObject, "mKeyguardFadingAwayDelay");
                Interpolator mLinearInterpolator = (Interpolator) XposedHelpers.getObjectField(param.thisObject, "mLinearInterpolator");
                Interpolator mBackdropInterpolator = (Interpolator) XposedHelpers.getObjectField(param.thisObject, "mBackdropInterpolator");
                final Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                boolean metaDataChanged = (boolean) param.args[0];

                Bitmap bg = null;
                Bitmap overlay = null;
                Bitmap mask = null;


                if (mBackdrop == null) return null; // called too early
                XposedBridge.log("mBackdrop not null");
                if (mLaunchTransitionFadingAway) {
                    mBackdrop.setVisibility(View.INVISIBLE);
                    return null;
                }

                DisplayMetrics displayMetrics = mBackdrop.getResources().getDisplayMetrics();
                final PlaybackState state;



                //mstatusbarwindow for stock ROMs
                //FrameLayout statusBarWindowStock = (FrameLayout) getObjectField(param.thisObject, "mStatusBarWindow");
                //XposedBridge.log("mStatusBarWindow in method children: " + statusBarWindowStock.getChildCount()); // has 2 children on CM ROM
                //for (int i = 0; i < statusBarWindowStock.getChildCount(); i++) {
                //    View childView = statusBarWindowStock.getChildAt(i);
                //    XposedBridge.log("found mStatusBarWindow in method child #" + i + ": " + statusBarWindowStock.getResources().getResourceName(childView.getId()));
                //}


                //mstatusbarwindow for stock ROMs
                FrameLayout statusBarWindow = (FrameLayout) getObjectField(param.thisObject, "mStatusBarWindow");
                XposedBridge.log("mStatusBarWindow children before change: " + statusBarWindow.getChildCount());
                int children = statusBarWindow.getChildCount();
                if ((children == 0) || (Utils.romIsCM13)) {
                    XposedBridge.log("non-stock ROM");
                    statusBarWindow = (FrameLayout) getObjectField(param.thisObject, "mStatusBarWindowContent");
                    XposedBridge.log("mStatusBarWindow children after change: " + statusBarWindow.getChildCount()); // has 2 children on CM ROM
                }

                /*
                for (int i = 0; i < statusBarWindow.getChildCount(); i++) {
                    View childView = statusBarWindow.getChildAt(i);
                    XposedBridge.log("mStatusBarWindow child #" + i + ": " + statusBarWindow.getResources().getResourceName(childView.getId()));
                }
                */

                FrameLayout panel_holder = null;
                if (!Utils.romIsCM13) {
                    XposedBridge.log("ROM is stock");
                    panel_holder = (FrameLayout) statusBarWindow.getChildAt(5);
                } else if (Utils.romIsCM13) {
                    XposedBridge.log("ROM is CM13");
                    panel_holder = (FrameLayout) statusBarWindow.getChildAt(4); // THIS IS 5 ON STOCK ROM AND 4 ON CM13
                }

                if (panel_holder == null) {
                    XposedBridge.log("panel_holder is null");
                    return null;
                }
                FrameLayout status_bar_expanded = (FrameLayout) panel_holder.getChildAt(0);
                GridLayout keyguard_status_view = (GridLayout) status_bar_expanded.getChildAt(0);
                LinearLayout keyguard_clock_container = (LinearLayout) keyguard_status_view.getChildAt(0);
                final TextView clockView = (TextView) keyguard_clock_container.getChildAt(0);

                LinearLayout dateLayout = (LinearLayout) keyguard_clock_container.getChildAt(1);
                final TextView dateView = (TextView) dateLayout.getChildAt(0);

                int controlsContainerChildNum = 0;
                if (!Utils.romIsCM13) {
                    controlsContainerChildNum = 3;
                } else if (Utils.romIsCM13) {
                    controlsContainerChildNum = 4;
                }
                final LinearLayout controlsContainer = (LinearLayout) keyguard_clock_container.getChildAt(controlsContainerChildNum); // THIS IS 3 ON STOCK ROM


                if (controlsContainer == null) {
                    XposedBridge.log("called before layout is drawn");
                    return null;
                }

                LinearLayout durationLayout = (LinearLayout) controlsContainer.getChildAt(0);
                LinearLayout timeLayout = (LinearLayout) durationLayout.getChildAt(1);
                final SeekBar currentPosition = (SeekBar) durationLayout.getChildAt(0);
                final TextView elapsedTime = (TextView) timeLayout.getChildAt(0);
                final TextView remainingTime = (TextView) timeLayout.getChildAt(1);

                LinearLayout controls = (LinearLayout) controlsContainer.getChildAt(2);
                LinearLayout volControls = (LinearLayout) controlsContainer.getChildAt(3);
                ImageView volDown = (ImageView) volControls.getChildAt(0);
                SeekBar volumeChanger = (SeekBar) volControls.getChildAt(1);
                ImageView volUp = (ImageView) volControls.getChildAt(2);

                Context appContext = mBackdrop.getContext().createPackageContext("me.connerowen.customcover", Context.CONTEXT_IGNORE_SECURITY);
                Resources appRes = appContext.getResources();
                AssetManager appAsset = appContext.getAssets();

                FrameLayout keyguardBottomAreaView = (FrameLayout) getObjectField(param.thisObject, "mKeyguardBottomArea");
                TextView indicationText = (TextView) keyguardBottomAreaView.getChildAt(0);
                final ImageView cameraImage = (ImageView) keyguardBottomAreaView.getChildAt(2);
                final ImageView leftImage = (ImageView) keyguardBottomAreaView.getChildAt(3);
                final ImageView lockIcon = (ImageView) keyguardBottomAreaView.getChildAt(4); // CHANGE THIS?????

                RelativeLayout keyguardStatusBarView = (RelativeLayout) getObjectField(param.thisObject, "mKeyguardStatusBar");

                final ImageButton previous2 = (ImageButton) controls.getChildAt(0);
                final ImageButton play2 = (ImageButton) controls.getChildAt(1);
                final ImageButton next2 = (ImageButton) controls.getChildAt(2);

                //final Drawable pause_image = appRes.getDrawable(R.drawable.ic_pause_white_48dp);
                //final Drawable play_image = appRes.getDrawable(R.drawable.ic_play_arrow_white_48dp);
                //final Drawable next_image = appRes.getDrawable(R.drawable.ic_skip_next_white_48dp);
                //final Drawable previous_image = appRes.getDrawable(R.drawable.ic_skip_previous_white_48dp);


                final Bitmap pause_image = Utils.getBitmapFromAsset(appAsset, "pause.png");
                final Bitmap play_image = Utils.getBitmapFromAsset(appAsset, "play.png");

                // CONTROLS BUTTONS SHADOW
                //final Bitmap pause_image = Utils.bitmapShadow(pause_imageT);
                //final Bitmap play_image = Utils.bitmapShadow(play_imageT);



                RelativeLayout status_bar_expanded_header = (RelativeLayout) getObjectField(param.thisObject, "mHeader");

                //TODO: FIX THIS clockgroup linearlayout can't be cast to framelayout
                //FrameLayout dateGroup = (FrameLayout) status_bar_expanded_header.getChildAt(4);
                // this (v) is for stock ROM
                //LinearLayout clockGroup = (LinearLayout) status_bar_expanded_header.getChildAt(5);
                // this (v) is for CM ROM
                //FrameLayout clockGroup = (FrameLayout) status_bar_expanded_header.getChildAt(5);

                //FrameLayout dateGroup = (FrameLayout) status_bar_expanded_header.getChildAt(5);
                //LinearLayout clockGroup = (LinearLayout) status_bar_expanded_header.getChildAt(6);

                FrameLayout notification_panel = (FrameLayout) getObjectField(param.thisObject, "mNotificationPanel");
                FrameLayout notification_container_parent = (FrameLayout) notification_panel.getChildAt(1);
                ScrollView scroll_view = (ScrollView) notification_container_parent.getChildAt(0);
                LinearLayout layoutToModify = (LinearLayout) scroll_view.getChildAt(0);
                FrameLayout qs_panel = (FrameLayout) layoutToModify.getChildAt(0);


                LinearLayout controlsHeader = (LinearLayout) status_bar_expanded_header.getChildAt(4);
                mInstance.setContent(controlsHeader);
                LinearLayout wrapper = (LinearLayout) controlsHeader.getChildAt(0);
                //mInstance.setWrapper(wrapper);
                TextView songTitle = (TextView) wrapper.getChildAt(0);
                TextView artistName = (TextView) wrapper.getChildAt(1);
                TextView albumName = (TextView) wrapper.getChildAt(2);
                LinearLayout buttonsContainer = (LinearLayout) wrapper.getChildAt(3);
                mInstance.setWrapper(buttonsContainer);

                final ImageButton previous_head = (ImageButton) buttonsContainer.getChildAt(0);
                final ImageButton play_head = (ImageButton) buttonsContainer.getChildAt(1);
                final ImageButton next_head = (ImageButton) buttonsContainer.getChildAt(2);

                ImageView album_art = (ImageView) controlsHeader.getChildAt(1);
                mInstance.setAlbum(album_art);

                final MediaPlayer mPlayer = mInstance.getMediaPlayer();
                if (mPlayer == null) {
                    XposedBridge.log("mediaplayer is null");
                }

                final MediaController mediaController = (MediaController) XposedHelpers.getObjectField(param.thisObject, "mMediaController");

                final MediaController.TransportControls mTransportControls;
                if (mediaController != null) {
                    mController = mediaController;
                    mTransportControls = mediaController.getTransportControls();
                    XposedBridge.log("transport controls not null");
                    state = mediaController.getPlaybackState();

                }
                else {
                    mTransportControls = null;
                    XposedBridge.log("transport controls null");
                    state = null;
                }

                if (state != null) {
                    // Make sure play button image is correct
                    if (state.getState() == PlaybackState.STATE_PLAYING) {
                        play2.setImageBitmap(pause_image);
                        play_head.setImageBitmap(pause_image);
                    } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                        play2.setImageBitmap(play_image);
                        play_head.setImageBitmap(play_image);
                    }
                }



                if (state != null) {
                    play2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (play2 == v) {
                                XposedBridge.log("play button clicked");
                                if (state.getState() == PlaybackState.STATE_PLAYING) {
                                    XposedBridge.log("music is playing, changing to paused");
                                    mTransportControls.pause();
                                    play2.setImageBitmap(play_image);
                                    play2.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);

                                   mBackdropBack.animate()
                                           .scaleX(0.8f)
                                           .scaleY(0.8f)
                                           //.translationZ(0.0f)
                                           .setDuration(1000)
                                           .setInterpolator(new BounceInterpolator())
                                           .setStartDelay(2)
                                           .start();

                                } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                                    XposedBridge.log("music is paused, changing to playing");
                                    mTransportControls.play();
                                    play2.setImageBitmap(pause_image);
                                    play2.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);

                                    mBackdropBack.animate()
                                            .scaleX(1.0f)
                                            .scaleY(1.0f)
                                            //.translationZ(12.0f)
                                            .setDuration(1000)
                                            .setInterpolator(new BounceInterpolator())
                                            .setStartDelay(2)
                                            .start();

                                } else {
                                    XposedBridge.log("state not handled, state is: " + state.toString());
                                }
                            }

                        }
                    });
                    play_head.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (play_head == v) {
                                XposedBridge.log("play button clicked");
                                if (state.getState() == PlaybackState.STATE_PLAYING) {
                                    XposedBridge.log("music is playing, changing to paused");
                                    mTransportControls.pause();
                                    play_head.setImageBitmap(play_image);
                                    play_head.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                                } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                                    XposedBridge.log("music is paused, changing to playing");
                                    mTransportControls.play();
                                    play_head.setImageBitmap(pause_image);
                                    play_head.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                                } else {
                                    XposedBridge.log("state not handled, state is: " + state.toString());
                                }
                            }

                        }
                    });
                    next2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (next2 == v) {
                                XposedBridge.log("next button clicked");
                                if (state.getState() == PlaybackState.STATE_PLAYING) {
                                    XposedBridge.log("music is playing");
                                    mTransportControls.skipToNext();
                                } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                                    XposedBridge.log("music is paused");
                                    mTransportControls.skipToNext();
                                } else {
                                    XposedBridge.log("state not handled, state is: " + state.toString());
                                }
                            }

                        }
                    });
                    next_head.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (next_head == v) {
                                XposedBridge.log("next button clicked");
                                if (state.getState() == PlaybackState.STATE_PLAYING) {
                                    XposedBridge.log("music is playing");
                                    mTransportControls.skipToNext();
                                } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                                    XposedBridge.log("music is paused");
                                    mTransportControls.skipToNext();
                                } else {
                                    XposedBridge.log("state not handled, state is: " + state.toString());
                                }
                            }

                        }
                    });
                    previous2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (previous2 == v) {
                                XposedBridge.log("previous button clicked");
                                if (state.getState() == PlaybackState.STATE_PLAYING) {
                                    XposedBridge.log("music is playing");
                                    mTransportControls.skipToPrevious();
                                } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                                    XposedBridge.log("music is paused");
                                    mTransportControls.skipToPrevious();
                                } else {
                                    XposedBridge.log("state not handled, state is: " + state.toString());
                                }
                            }

                        }
                    });
                    previous_head.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (previous_head == v) {
                                XposedBridge.log("previous button clicked");
                                if (state.getState() == PlaybackState.STATE_PLAYING) {
                                    XposedBridge.log("music is playing");
                                    mTransportControls.skipToPrevious();
                                } else if (state.getState() == PlaybackState.STATE_PAUSED) {
                                    XposedBridge.log("music is paused");
                                    mTransportControls.skipToPrevious();
                                } else {
                                    XposedBridge.log("state not handled, state is: " + state.toString());
                                }
                            }

                        }
                    });
                }

                Bitmap artworkBitmap = null;
                if (mMediaMetadata != null) {
                    artworkBitmap = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                    if (artworkBitmap == null) {
                        artworkBitmap = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                        // might still be null
                    }
                }

                ViewGroup.MarginLayoutParams backdropFront_params = (ViewGroup.MarginLayoutParams) mBackdropFront.getLayoutParams();

                switch (displayMetrics.densityDpi) {
                    case DisplayMetrics.DENSITY_560:
                        backdropFront_params.width = 1350; // ORIGINAL 1300
                        backdropFront_params.height = 1350;
                        backdropFront_params.topMargin = 200; // ORIGINAL WAS 900
                        backdropFront_params.leftMargin = 45; // ORIGINAL 70
                        break;
                    case DisplayMetrics.DENSITY_XXHIGH:
                        backdropFront_params.width = 1080;
                        backdropFront_params.height = 1940;
                        backdropFront_params.topMargin = 674;
                        break;
                    case DisplayMetrics.DENSITY_XHIGH:
                        backdropFront_params.width = 720;
                        backdropFront_params.height = 627;
                        backdropFront_params.topMargin = 450;
                        break;
                }

                final boolean hasArtwork = artworkBitmap != null;
                if ((hasArtwork) && (Utils.isKeyguardLocked(mBackdrop.getContext()))) {
                //if (hasArtwork) {
                    // time to show some art!
                    if (mBackdrop.getVisibility() != View.VISIBLE) {
                        mBackdrop.setVisibility(View.VISIBLE);
                        mBackdrop.animate().alpha(1f);
                        metaDataChanged = true;
                    }
                    if (metaDataChanged) {
                        if (mBackdropBack.getDrawable() != null) {
                            Drawable drawable =
                                    mBackdropBack.getDrawable().getConstantState().newDrawable().mutate();
                            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                            if (state.getState() == PlaybackState.STATE_PAUSED) {
                                bitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1080, true);
                            }
                            else if (state.getState() == PlaybackState.STATE_PLAYING) {
                                bitmap = Bitmap.createScaledBitmap(bitmap, 1350, 1350, true);
                            }
                            else {
                                bitmap = Bitmap.createScaledBitmap(bitmap, 1350, 1350, true);
                            }

                            Drawable d = new BitmapDrawable(Utils.roundCornerImage(bitmap, 10.0f));

                            //bitmap = Bitmap.createScaledBitmap(bitmap, 0, 0, true);

                            // mBackdropBack.setImageBitmap(Utils.roundCornerImage(artworkBitmap, 10.0f));
                            mBackdropFront.setImageDrawable(d);

                            mBackdropFront.setBackground(appRes.getDrawable(R.drawable.shadow_header_new));

                            XposedBridge.log("mbackdropfront dimens:" + mBackdropFront.getWidth() + " x " + mBackdropFront.getHeight());
                            if (mScrimSrcModeEnabled) {
                                //mBackdropFront.getDrawable().mutate().setXfermode(mSrcOverXferMode);
                                //mBackdropFront.getDrawable().mutate().setColorFilter(Color.TRANSPARENT, mSrcOverXferMode);
                                XposedBridge.log("mScrimSrcModeEnabled");
                            }
                            XposedBridge.log("mbackdropfront fading in");
                            mBackdropFront.setAlpha(1f);
                            mBackdropFront.setVisibility(View.VISIBLE);
                        } else {
                            mBackdropFront.setVisibility(View.INVISIBLE);
                        }
                        mBackdropBack.setImageBitmap(artworkBitmap);
                        if (mScrimSrcModeEnabled) {
                            //mBackdropBack.getDrawable().mutate().setXfermode(mSrcXferMode);
                            XposedBridge.log("mScrimSrcModeEnabled");
                        }
                        if (mBackdropFront.getVisibility() == View.VISIBLE) {
                            XposedBridge.log("mbackdropfront fading out");
                            mBackdropFront.animate()
                                    .setDuration(250)
                                    .alpha(0f).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        mBackdropFront.setVisibility(View.INVISIBLE);
                                        mBackdropFront.animate().cancel();
                                        mBackdropFront.setImageDrawable(null);
                                    }
                            });
                        }
                    }

                    switch (displayMetrics.densityDpi) {
                        case DisplayMetrics.DENSITY_560:
                            artworkBitmap = Bitmap.createScaledBitmap(artworkBitmap, 1350, 1350, true); // 809 for s5 // ORIGINAL WAS 1080
                            break;
                        case DisplayMetrics.DENSITY_XXHIGH:
                            artworkBitmap = Bitmap.createScaledBitmap(artworkBitmap, 809, 809, true); // 809 for s5
                            break;
                        case DisplayMetrics.DENSITY_XHIGH:
                            artworkBitmap = Bitmap.createScaledBitmap(artworkBitmap, 1080, 1080, true);
                            break;

                    }

                    String song, artist, album;
                    song = mMediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                    artist = mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
                    if (artist == null) {
                        artist = mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                    }
                    album = mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);

                    XposedBridge.log("song: " + song + " album: " + album + " artist: " + artist);



                    Palette p = Palette.from(artworkBitmap).generate();
                    List<Palette.Swatch> swatches = p.getSwatches();

                    for (int i=0; i<swatches.size(); i++) {
                        Palette.Swatch thisSwatch = swatches.get(i);

                        if (swatches.size() == 1) {
                            // Only one swatch generated, rare condition (walk the moon)
                            // bgColor = thisSwatch.getRGB();
                            // textColor = thisSwatch.getTitleTextColor();
                        }
                    }

                    album_art.setImageBitmap(Utils.roundCornerImage(artworkBitmap, 22.0f));
                    album_art.setBackground(appRes.getDrawable(R.drawable.shadow_header_new));

                    Target DARK = new Target.Builder().setMinimumLightness(0f)
                            .setTargetLightness(0.26f)
                            .setMaximumLightness(0.5f)
                            .setMinimumSaturation(0.1f)
                            .setTargetSaturation(0.6f)
                            .setMaximumSaturation(1f)
                            .setPopulationWeight(0.18f)
                            .setSaturationWeight(0.22f)
                            .setLightnessWeight(0.60f)
                            .setExclusive(false)
                            .build();

                    Target LIGHT = new Target.Builder().setMinimumLightness(0.50f)
                            .setTargetLightness(0.74f)
                            .setMaximumLightness(1.0f)
                            .setMinimumSaturation(0.1f)
                            .setTargetSaturation(0.7f)
                            .setMaximumSaturation(1f)
                            .setPopulationWeight(0.18f)
                            .setSaturationWeight(0.22f)
                            .setLightnessWeight(0.60f)
                            .setExclusive(false)
                            .build();

                    Target NEUTRAL = new Target.Builder().setMinimumLightness(0.20f)
                            .setTargetLightness(0.5f)
                            .setMaximumLightness(0.8f)
                            .setMinimumSaturation(0.1f)
                            .setTargetSaturation(0.6f)
                            .setMaximumSaturation(1f)
                            .setPopulationWeight(0.18f)
                            .setSaturationWeight(0.22f)
                            .setLightnessWeight(0.60f)
                            .setExclusive(false)
                            .build();

                    Palette.Swatch light = p.getSwatchForTarget(LIGHT);
                    Palette.Swatch dark = p.getSwatchForTarget(DARK);
                    Palette.Swatch neutral = p.getSwatchForTarget(NEUTRAL);


                    vibrant = p.getLightVibrantColor(defaultColor);
                    vibrantRegular = p.getVibrantColor(defaultColor);
                    if (vibrantRegular == defaultColor) {
                        vibrantRegular = p.getLightVibrantColor(defaultColor);
                    }
                    if (vibrant == defaultColor) {
                        vibrant = p.getVibrantColor(defaultColor);
                        //vibrantRegular = p.getDarkVibrantColor(defaultColor);
                        //if (p.getLightVibrantSwatch() != null) {vibrantText = p.getLightVibrantSwatch().getTitleTextColor();}
                        // if dark vibrant is 0
                        if (vibrant == defaultColor) {
                            vibrant = p.getDarkVibrantColor(defaultColor);
                            vibrantRegular = p.getLightVibrantColor(defaultColor);
                            if (p.getDarkVibrantSwatch() != null) {vibrantText = p.getDarkVibrantSwatch().getTitleTextColor();}
                        }
                    }

                    muted = p.getMutedColor(defaultColor);
                    //muted = p.getDarkVibrantColor(defaultColor);
                    if (p.getMutedSwatch() != null) {mutedText = p.getMutedSwatch().getTitleTextColor();}
                    if (muted == defaultColor) {
                        muted = p.getLightMutedColor(defaultColor);
                        if (p.getLightMutedSwatch() != null) {mutedText = p.getLightMutedSwatch().getTitleTextColor();}
                        // if dark muted is 0
                        if (muted == defaultColor) {
                            muted = p.getDarkMutedColor(defaultColor);
                            if (p.getDarkMutedSwatch() != null) {mutedText = p.getDarkMutedSwatch().getTitleTextColor();}
                            XposedBridge.log("using light muted");
                        }
                        XposedBridge.log("using dark muted");
                    }

                    if (light != null) {
                        vibrantRegular = light.getRgb();
                        //vibrantRegular = light.getBodyTextColor();
                    }
                    if (neutral != null) {
                        vibrant = neutral.getRgb();
                        //vibrantRegular = neutral.getRgb();
                    }
                    if (dark != null) {
                        //muted = dark.getRgb();
                        vibrantRegular = dark.getRgb();
                    }

                    Typeface typeface = FontManager.getInstance().getTypeface(appContext, R.string.fontFrygia);

                    mInstance.setColorTest(vibrant);
                    mInstance.setColorMuted(vibrantText);

                    // keyguard controls
                    play2.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                    next2.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                    previous2.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);

                    /*Canvas canvas_play = new Canvas(drawnBitmap);
                    Paint mShadow = new Paint();
                    // radius=10, y-offset=2, color=black
                    mShadow.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
                    // in onDraw(Canvas)
                    canvas.drawBitmap(bitmap, 0.0f, 0.0f, mShadow);*/

                    // statusbar expanded controls
                    play_head.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                    next_head.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                    previous_head.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);

                    clockView.setVisibility(TextView.GONE);
                    dateView.setVisibility(TextView.GONE);
                    controlsContainer.setVisibility(View.VISIBLE);

                    volDown.setColorFilter(vibrant, Mode.SRC_ATOP);
                    //volDown.setAlpha(0.95f);
                    volUp.setColorFilter(vibrant, Mode.SRC_ATOP);
                    //volUp.setAlpha(0.95f);

                    // ClipDrawable d1 = (ClipDrawable) ld.findDrawableByLayerId(R.id.progressshape);
                    volumeChanger.getProgressDrawable().getCurrent().setColorFilter(vibrantRegular, Mode.MULTIPLY);

                    volumeChanger.getThumb().mutate().setColorFilter(vibrant, Mode.SRC_ATOP);

                    MediaController.PlaybackInfo mediaInfo = mediaController.getPlaybackInfo();
                    XposedBridge.log("current volume: " + mediaInfo.getCurrentVolume());
                    XposedBridge.log("max volume: " + mediaInfo.getMaxVolume());
                    XposedBridge.log("volume control: " + mediaInfo.getVolumeControl());

                    //Drawable volThumb = appRes.getDrawable(R.drawable.ic_fiber_manual_record_white_36dp);
                    //volThumb.setColorFilter(vibrant, PorterDuff.Mode.MULTIPLY);
                    volumeChanger.setProgress(mediaInfo.getCurrentVolume());
                    volumeChanger.setMax(mediaInfo.getMaxVolume());
                    //volumeChanger.setThumb(volThumb);

                    volumeChanger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        int progressChanged = 0;

                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                            progressChanged = progress;
                            mediaController.setVolumeTo(progress, AudioManager.FLAG_SHOW_UI);
                        }

                        public void onStartTrackingTouch(SeekBar seekBar) {
                            // TODO Auto-generated method stub
                        }

                        public void onStopTrackingTouch(SeekBar seekBar) {
                            // TODO Auto-generated method stub
                        }
                    });

                    long remainingT = (mMediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION)) - (state.getPosition());
                    XposedBridge.log("duration converted: " + Utils.milliToMinute(mMediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION)));
                    XposedBridge.log("current pos converted: " + Utils.milliToMinute(state.getPosition()));
                    elapsedTime.setText(Utils.milliToMinute(state.getPosition()));
                    remainingTime.setText("-" + Utils.milliToMinute(remainingT));

                    elapsedTime.setTextColor(vibrant);
                    elapsedTime.setTypeface(typeface);
                    remainingTime.setTextColor(vibrant);
                    remainingTime.setTypeface(typeface);

                    currentPosition.setProgress((int)state.getPosition());
                    currentPosition.setMax((int)(mMediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION)));

                    currentPosition.getProgressDrawable().getCurrent().setColorFilter(vibrantRegular, Mode.MULTIPLY);
                    currentPosition.getThumb().mutate().setColorFilter(vibrant, Mode.SRC_ATOP);

                    /*
                    currentPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        int progressChanged = 0;

                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                            progressChanged = progress;
                            long positionMillis = state.getPosition();
                            long durationMillis = mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                            long newPositionMillis = (((long) progress) * durationMillis) / ((long) seekBar.getMax());
                            if (newPositionMillis == durationMillis) {
                                newPositionMillis -= 500;
                            }
                            mediaController.getTransportControls().seekTo(newPositionMillis);

                            elapsedTime.setText(Utils.milliToMinute(state.getPosition()));
                            long remainingT = (mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)) - (state.getPosition());
                            remainingTime.setText(Utils.milliToMinute(remainingT));
                        }

                        public void onStartTrackingTouch(SeekBar seekBar) {
                            // TODO Auto-generated method stub
                        }

                        public void onStopTrackingTouch(SeekBar seekBar) {
                            // TODO Auto-generated method stub
                        }
                    });*/





                    // intent filter, registers after update is called once
                    Context context = mBackdrop.getContext();
                    IntentFilter lockscreenFilter3 = new IntentFilter();
                    lockscreenFilter3.addAction(Intent.ACTION_SCREEN_ON);
                    lockscreenFilter3.addAction(Intent.ACTION_TIME_TICK);
                    lockscreenFilter3.addAction(Intent.ACTION_SCREEN_OFF);
                    lockscreenFilter3.addAction(Intent.ACTION_USER_PRESENT);
                    context.registerReceiver(new BroadcastReceiver() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                                XposedBridge.log("screen unlocked, when does this get run?");

                            }
                            if(intent.getAction().equals(Intent.ACTION_SCREEN_ON))  {
                                XposedBridge.log("screen is locked, when does this get run?");
                                if (mBackdrop.getVisibility() == View.GONE) {
                                    XposedBridge.log("album art is gone from lockscreen");
                                    XposedBridge.log("packageName: " + mediaController.getPackageName());
                                    XposedBridge.log("state: " + state.toString());
                                    clockView.setVisibility(TextView.VISIBLE);
                                    dateView.setVisibility(TextView.VISIBLE);
                                    controlsContainer.setVisibility(View.GONE);
                                    mInstance.setColorTest(Color.WHITE);
                                    cameraImage.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                                    leftImage.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                                    lockIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                                }
                                /*else if (mBackdrop.getVisibility() == View.VISIBLE) {
                                    final long remainingT = (mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)) - (state.getPosition());
                                    elapsedTime.setText(Utils.milliToMinute(state.getPosition()));
                                    remainingTime.setText(Utils.milliToMinute(remainingT));

                                    final double i = remainingT / 1000.0;
                                    ; //declare this globally
                                    XposedBridge.log("remainingT: " + i);
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            long remaining = (mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)) - (state.getPosition());
                                            XposedBridge.log("remaining: " + remaining);
                                            if (remaining != 0) {
                                                elapsedTime.setText(Utils.milliToMinute(state.getPosition()));
                                                remainingTime.setText(Utils.milliToMinute(remainingT));
                                                remaining -= 1000;
                                                handler.postDelayed(this, 1000);
                                            } else {
                                                handler.removeCallbacks(this);
                                            }
                                        }
                                    }, 1000);
                                }*/

                            }
                            /*if(intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
                                if ((hasArtwork) && (mBackdrop.getVisibility() == View.VISIBLE)) {
                                    long remainingT = (mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)) - (state.getPosition());
                                    //XposedBridge.log("duration converted: " + Utils.milliToMinute(mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
                                    //XposedBridge.log("current pos converted: " + Utils.milliToMinute(state.getPosition()));
                                    elapsedTime.setText(Utils.milliToMinute(state.getPosition()));
                                    remainingTime.setText(Utils.milliToMinute(remainingT));
                                    //currentPosition.setProgress((int) state.getPosition());
                                }
                            }*/
                        }
                    }, lockscreenFilter3);


                    //controlsHeader.setBackgroundColor(Utils.manipulateColor(muted, 0.9f));
                    GradientDrawable drawableHead = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { Utils.manipulateColor(muted, 0.75f), Utils.manipulateColor(muted, 0.9f) });
                    controlsHeader.setBackground(drawableHead);

                    status_bar_expanded_header.setBackgroundColor(muted);
                    qs_panel.setBackgroundColor(muted);

                    //dateExpanded.setTextColor(mutedText);
                    //timeView.setTextColor(vibrantText);
                    //amPmView.setTextColor(vibrantText);

                    //bg = Utils.getBitmapFromAsset(appAsset, "vinylbg_large.png");
                    bg = Utils.getBitmapFromAsset(appAsset, "roundedcorner_bg.png");
                    overlay = Utils.getBitmapFromAsset(appAsset, "vinyloverlay_large.png");

                    mask = Utils.getBitmapFromAsset(appAsset, "roundedcorner_mask.png");

                    switch (displayMetrics.densityDpi) {
                        case DisplayMetrics.DENSITY_560:
                            //bg = Utils.getBitmapFromAsset(appAsset, "vinylbg_large.png");
                            //overlay = Utils.getBitmapFromAsset(appAsset, "vinyloverlay_large.png");
                            break;
                        case DisplayMetrics.DENSITY_XXHIGH:
                            bg = Bitmap.createScaledBitmap(bg, 1080, 940, true);
                            overlay = Bitmap.createScaledBitmap(overlay, 1080, 940, true);
                            break;
                        case DisplayMetrics.DENSITY_XHIGH:
                            bg = Bitmap.createScaledBitmap(bg, 1080, 940, true);
                            overlay = Bitmap.createScaledBitmap(overlay, 1080, 940, true);
                            break;

                    }

                    // status bar expanded header stuff
                    songTitle.setText(song);
                    songTitle.setTextColor(vibrant);
                    songTitle.setVisibility(View.VISIBLE);

                    /* songTitle.setShadowLayer(
                            1.5f, // radius
                            2.0f, // dx
                            2.0f, // dy
                            Color.parseColor("#FF303030") // shadow color
                    ); */

                    albumName.setText(album);
                    albumName.setTextColor(Utils.manipulateColor(vibrant, 0.9f));
                    albumName.setVisibility(View.VISIBLE);
                    /* albumName.setShadowLayer(
                            1.5f, // radius
                            2.0f, // dx
                            2.0f, // dy
                            Color.parseColor("#FF303030") // shadow color
                    ); */

                    artistName.setText(artist);
                    artistName.setTextColor(Utils.manipulateColor(vibrant, 0.9f));
                    artistName.setVisibility(View.VISIBLE);
                    /* artistName.setShadowLayer(
                            1.5f, // radius
                            2.0f, // dx
                            2.0f, // dy
                            Color.parseColor("#FF303030") // shadow color
                    ); */

                    if (typeface != null) {
                        XposedBridge.log("statusbarhook: typeface not null");
                        songTitle.setTypeface(typeface);
                        albumName.setTypeface(typeface);
                        artistName.setTypeface(typeface);
                    }
                    else {
                        XposedBridge.log("statusbarhook: typeface null");
                    }

                    // end

                    indicationText.setTextColor(vibrant);
                    /* indicationText.setShadowLayer(
                            1.5f, // radius
                            2.0f, // dx
                            2.0f, // dy
                            Color.parseColor("#FF303030") // shadow color
                    );*/
                    cameraImage.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                    leftImage.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);
                    lockIcon.setColorFilter(vibrant, PorterDuff.Mode.SRC_ATOP);

                    GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[] { Utils.manipulateColor(muted, 0.7f), Utils.manipulateColor(muted, 0.93f) });
                    //drawable.setGradientType();
                    mBackdrop.setBackground(drawable);

                    //mBackdrop.setBackgroundColor(muted);

                   // mBackdrop.setBa

                    try {
                        //TextView currentT = (TextView) mBackdrop.getChildAt(0);
                        //currentTime.setText("Test");
                        //currentTime.setTextSize(1f);
                        //currentTime.setTextColor(muted);

                        LinearLayout keyguardText = (LinearLayout) controlsContainer.getChildAt(1);
                        TextView keyguardSong = (TextView) keyguardText.getChildAt(0);
                        TextView keyguardArtist = (TextView) keyguardText.getChildAt(1);

                        keyguardSong.setText(song);
                        keyguardSong.setTextColor(vibrant);

                        //Typeface typeface = FontManager.getInstance().getTypeface(context, "fonts/Frygia-Regular.ttf");
                        if (typeface != null) {
                            XposedBridge.log("statusbarhook: typeface not null");
                            keyguardSong.setTypeface(typeface);
                        }
                        else {
                            XposedBridge.log("statusbarhook: typeface null");
                            //keyguardSong.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        }

                        // Set TextView Text shadow
                        /* keyguardSong.setShadowLayer(
                                1.5f, // radius
                                2.0f, // dx
                                2.0f, // dy
                                Color.parseColor("#FF303030") // shadow color
                        ); */

                        keyguardArtist.setText(artist + " — " + album);
                        keyguardArtist.setTextColor(vibrantRegular);
                        //keyguardArtist.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        keyguardArtist.setAlpha(1.0f);
                        if (typeface != null) {
                            keyguardArtist.setTypeface(typeface);
                        }
                        /* keyguardArtist.setShadowLayer(
                                1.5f, // radius
                                2.0f, // dx
                                2.0f, // dy
                                Color.parseColor("#FF303030") // shadow color
                        ); */

                        //ViewGroup.MarginLayoutParams keyguardTextParams = (ViewGroup.MarginLayoutParams) keyguardText.getLayoutParams();
                        ViewGroup.MarginLayoutParams keyguardControlsParams = (ViewGroup.MarginLayoutParams) controlsContainer.getLayoutParams();
                        switch (displayMetrics.densityDpi) {
                            case DisplayMetrics.DENSITY_560:
                                //keyguardTextParams.topMargin = 810; //31.64% down
                                //keyguardControlsParams.topMargin = 663;
                                break;
                            case DisplayMetrics.DENSITY_XXHIGH:
                                //keyguardTextParams.topMargin = 607; //31.64% down
                                //keyguardControlsParams.topMargin = 460;
                                break;
                            case DisplayMetrics.DENSITY_XHIGH:
                                //keyguardTextParams.topMargin = 405; //31.64% down
                                //keyguardControlsParams.topMargin = 258;
                                break;

                        }
                        //keyguardTextParams.leftMargin = 500;
                        //keyguardText.requestLayout();
                    } catch (ClassCastException exc) {
                        XposedBridge.log("cannot cast textview to imageview");
                    }

                    ViewGroup.MarginLayoutParams backdropBack_params = (ViewGroup.MarginLayoutParams) mBackdropBack.getLayoutParams();

                    switch (displayMetrics.densityDpi) {
                        case DisplayMetrics.DENSITY_560:
                            // phones like nexus 6p & note 4 (2560x1440 most of the time)
                            XposedBridge.log("device has density of 560");
                            backdropBack_params.width = 1350; // ORIGINAL IS 1440
                            backdropBack_params.height = 1350; // ORIGINAL IS 1253
                            backdropBack_params.topMargin = 200; //35.1% down // ORIGINAL WAS 900
                            backdropBack_params.leftMargin = 45;
                            break;
                        case DisplayMetrics.DENSITY_XXHIGH:
                            //phones like galaxy s5 (1920x1080 most of the time) //65 top 74 left
                            XposedBridge.log("device has density of 480");
                            backdropBack_params.width = 1080;
                            backdropBack_params.height = 940;
                            backdropBack_params.topMargin = 674; //35.1% down
                            break;
                        case DisplayMetrics.DENSITY_XHIGH:
                            // phones like galaxy s3 (1280x720 most of the time)
                            XposedBridge.log("device has density of 320");
                            backdropBack_params.width = 720;
                            backdropBack_params.height = 627;
                            backdropBack_params.topMargin = 450; //35.1% down
                            break;
                    }

                    //left, top
                    switch (displayMetrics.densityDpi) {
                        case DisplayMetrics.DENSITY_560:
                            //artworkBitmap = themeUtil.drawMultipleBitmapsOnImageView(artworkBitmap, bg, overlay, null, 98, 86, displayMetrics, vibrant, album);
                            //artworkBitmap = themeUtil.drawMultipleBitmapsOnImageView(artworkBitmap, bg, null, mask, 270, 77, displayMetrics, vibrant, album);
                            break;
                        case DisplayMetrics.DENSITY_XXHIGH:
                            artworkBitmap = themeUtil.drawMultipleBitmapsOnImageView(artworkBitmap, bg, overlay, null, 74, 65, displayMetrics, vibrant, album);
                            break;
                        case DisplayMetrics.DENSITY_XHIGH:
                            artworkBitmap = themeUtil.drawMultipleBitmapsOnImageView(artworkBitmap, bg, overlay, null, 74, 65, displayMetrics, vibrant, album);
                            break;

                    }

                    artworkBitmap = Utils.roundCornerImage(artworkBitmap, 10.0f);

                    mBackdropBack.setImageBitmap(artworkBitmap);

                    themeUtil.saveAlbumArt(artworkBitmap, album);
                    //mBackdropBack.setBackground(appRes.getDrawable(R.drawable.bg));
                    mBackdropBack.setBackground(appRes.getDrawable(R.drawable.shadow_header_new));

                    //mBackdropBack.setElevation(12.0f);
                    //backdropBack.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    //backdropBack.setBackgroundColor(muted);
                    //status_bar_expanded_header.requestLayout();
                    //controlsHeader.requestLayout();
                    mBackdropBack.requestLayout();

                } else {
                    // need to hide the album art, either because we are unlocked or because
                    // the metadata isn't there to support it
                    if (mBackdrop.getVisibility() != View.GONE) {
                        {
                            mBackdrop.animate()
                                    // Never let the alpha become zero - otherwise the RenderNode
                                    // won't draw anything and uninitialized memory will show through
                                    // if mScrimSrcModeEnabled. Note that 0.001 is rounded down to 0 in
                                    // libhwui.
                                    .alpha(0.002f)
                                    .setInterpolator(mBackdropInterpolator)
                                    .setDuration(300)
                                    .setStartDelay(0)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            mBackdrop.setVisibility(View.GONE);
                                            mBackdropFront.animate().cancel();
                                            mBackdropBack.animate().cancel();
                                            mHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mBackdropFront.setVisibility(View.INVISIBLE);
                                                    mBackdropFront.animate().cancel();
                                                    mBackdropFront.setImageDrawable(null);
                                                }
                                            });
                                        }
                                    });
                            if (mKeyguardFadingAway) {
                                mBackdrop.animate()
                                        // Make it disappear faster, as the focus should be on the activity
                                        // behind.
                                        .setDuration(mKeyguardFadingAwayDuration / 2)
                                        .setStartDelay(mKeyguardFadingAwayDelay)
                                        .setInterpolator(mLinearInterpolator)
                                        .start();
                            }
                        }
                    }
                }
                return null;
            }
        });

        Class<?> KeyguardIndicationController = findClass("com.android.systemui.statusbar.KeyguardIndicationController", classLoader);

        try {
            findAndHookMethod(KeyguardIndicationController, "computeColor", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(mInstance.getColorTest());
                }
            });
        } catch (NoSuchMethodError e) {}

        Class<?> StatusBarHeaderView = findClass("com.android.systemui.statusbar.phone.StatusBarHeaderView", classLoader);

        try {
            findAndHookMethod(StatusBarHeaderView, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    TextView time = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTime");
                    TextView amPm = (TextView) XposedHelpers.getObjectField(param.thisObject, "mAmPm");
                    TextView dateCollapsed = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateCollapsed");
                    TextView dateExpanded = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateExpanded");
                    TextView batteryLevel = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                    ImageButton settingsButton = (ImageButton) XposedHelpers.getObjectField(param.thisObject, "mSettingsButton");

                    //time.setTextColor(mInstance.getColorMuted());
                    /*timeText = time;
                    amPm.setTextColor(mInstance.getColorMuted());
                    dateCollapsed.setTextColor(mInstance.getColorMuted());
                    dateExpanded.setTextColor(mInstance.getColorMuted());
                    batteryLevel.setTextColor(mInstance.getColorMuted());
                    settingsButton.setColorFilter(mInstance.getColorMuted(), PorterDuff.Mode.SRC_ATOP);*/
                }
            });

            findAndHookMethod(StatusBarHeaderView, "updateEverything", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                }
            });

            findAndHookMethod(StatusBarHeaderView, "loadDimens", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int collapsedHeight = (int) XposedHelpers.getObjectField(param.thisObject, "mCollapsedHeight");
                    int expandedHeight = (int) XposedHelpers.getObjectField(param.thisObject, "mExpandedHeight");
                    collapsedHeight = 600; //38.2% of the height
                    expandedHeight = 800;
                    XposedHelpers.setObjectField(param.thisObject, "mCollapsedHeight", collapsedHeight);
                    XposedHelpers.setObjectField(param.thisObject, "mExpandedHeight", expandedHeight);

                }
            });

            findAndHookMethod(StatusBarHeaderView, "updateLayoutValues", float.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //XposedBridge.log("updatelayoutvalues found");
                    float currentT = (float) param.args[0];
                    float mCurrent = (float) XposedHelpers.getObjectField(param.thisObject, "mCurrentT");
                    float t3 = Math.max(0, currentT - 0.7f) / 0.3f;


                    try {
                        LinearLayout wrapper = mInstance.getWrapper();
                        LinearLayout content = mInstance.getContent();
                        ImageView album = mInstance.getAlbum();


                        float contentY = 400f * (1 - currentT) + 600f * currentT;

                        float imageScale = 360f * (1 - currentT) + 540f * currentT;
                        float imageX = 990f * (1 - currentT) + 790f * currentT; // ORIGINALLY WAS 650f & 450f

                        float wrapperAlpha = 1.0f * (1 - t3) + 0f * t3;



                        //RelativeLayout.LayoutParams newH = new RelativeLayout.LayoutParams(content.getLayoutParams());

                        //LinearLayout.LayoutParams newAlbum = (LinearLayout.LayoutParams) album.getLayoutParams();


                        if (!album.isInLayout()) {
                            ViewGroup.LayoutParams newAlbum = (ViewGroup.LayoutParams) album.getLayoutParams();
                            album.setX(imageX);
                            newAlbum.height = (int) imageScale;
                            newAlbum.width = (int) imageScale;
                            album.setLayoutParams(newAlbum);
                        }
                        else {
                            //XposedBridge.log("album is currently in the middle of layout");
                        }

                        if (!content.isInLayout()) {
                            RelativeLayout.LayoutParams newH = (RelativeLayout.LayoutParams) content.getLayoutParams();
                            newH.height = (int) contentY;
                            content.setLayoutParams(newH);
                        }
                        else {
                            //XposedBridge.log("content is currently in the middle of layout");
                        }

                        //album.requestLayout();
                        //content.requestLayout();

                        if (wrapperAlpha == 0f) {
                            wrapper.setVisibility(View.INVISIBLE);
                        } else {
                            wrapper.setVisibility(View.VISIBLE);
                            wrapper.setAlpha(wrapperAlpha);
                        }

                        if ((album.getWidth() == 540) && (wrapperAlpha > 0f))
                        {
                            XposedBridge.log("this shouldn't happen");
                            ViewGroup.LayoutParams newAlbum = (ViewGroup.LayoutParams) album.getLayoutParams();
                            album.setX(imageX);
                            newAlbum.height = (int) imageScale;
                            newAlbum.width = (int) imageScale;
                            album.setLayoutParams(newAlbum);
                            RelativeLayout.LayoutParams newH = (RelativeLayout.LayoutParams) content.getLayoutParams();
                            newH.height = (int) contentY;
                            content.setLayoutParams(newH);
                        }


                    }
                    catch (NullPointerException e) {
                        Log.d("CustomCover", "something is null: " + e.getMessage());
                    }

                }
            });
        } catch (NoSuchMethodError e) {}

        Class<?> ScrimController = findClass("com.android.systemui.statusbar.phone.ScrimController", classLoader);

        // disable lock screen dim
        findAndHookMethod(ScrimController, "setScrimBehindColor", float.class, new XC_MethodHook() {
            //@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                boolean mKeyguardShowing = XposedHelpers.getBooleanField(param.thisObject, "mKeyguardShowing");
                int opacity = 0;
                float overlayAlpha = opacity / 100f; //Alpha value between 0 (invisible) and 1 (fully visible
                if (mKeyguardShowing) {
                    param.args[0] = overlayAlpha;
                }
            }
        }); //end findandhookmethod Scrimcontroller

        Class<?> KeyguardClock = findClass("com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm", classLoader);

        // container y translation
        XposedHelpers.findAndHookMethod(KeyguardClock, "getClockY", new Object[]{new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam) throws Throwable {
                Object localObject = paramAnonymousMethodHookParam.getResultOrThrowable();
                if ((localObject != null) && ((localObject instanceof Integer))) {
                    int screenDPI = deviceDPI;
                    mInstance.setDPI(screenDPI);
                    switch (screenDPI) {
                        case 560:
                            paramAnonymousMethodHookParam.setResult(Integer.valueOf(((Integer) paramAnonymousMethodHookParam.getResult()).intValue() + 1030)); //380 is good for 2560x1440
                            break;
                        case 480:
                            paramAnonymousMethodHookParam.setResult(Integer.valueOf(((Integer) paramAnonymousMethodHookParam.getResult()).intValue() + -230)); //380 is good for 2560x1440
                            break;
                        case 320:
                            paramAnonymousMethodHookParam.setResult(Integer.valueOf(((Integer) paramAnonymousMethodHookParam.getResult()).intValue() + -180)); //380 is good for 2560x1440
                            break;

                    }

                }

            }
        }});

        //notifications top padding
        XposedHelpers.findAndHookMethod(KeyguardClock, "getClockNotificationsPadding", new Object[]{new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam) throws Throwable {
                //XposedBridge.log("getClockNotificationsPadding");
                Object localObject = paramAnonymousMethodHookParam.getResultOrThrowable();
                if ((localObject != null) && ((localObject instanceof Integer))) {
                    paramAnonymousMethodHookParam.setResult(Integer.valueOf(((Integer) paramAnonymousMethodHookParam.getResult()).intValue() + 800)); // -50 ?? ORIGINAL 1100
                }
            }
        }});


            XC_MethodHook addRemoveIconHook = new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //mInstance.setStatusIcons((LinearLayout) getObjectField(param.thisObject, "mStatusIcons"));
                    if (param.method.getName().equals("addIcon"))
                        mInstance.refreshStatusIconColors();
                }

            };

            try

            {
                Class<?> StatusBarIcon = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIcon", null);
                findAndHookMethod(PhoneStatusBar, "addIcon", String.class, int.class, int.class, StatusBarIcon, addRemoveIconHook);
                findAndHookMethod(PhoneStatusBar, "removeIcon", String.class, int.class, int.class, addRemoveIconHook);
            }

            catch(
            Throwable t
            )

            {
                t.printStackTrace();
            }

            Class<?> StatusBarIconView = XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView", classLoader);
            XposedBridge.hookAllConstructors(StatusBarIconView, new

                            XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    ImageView view = (ImageView) param.thisObject;
                                    mInstance.addSystemIconView(view, true);
                                }
                            }

            );


        } //end statusbarhook


    }

package me.connerowen.customcover;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.graphics.Bitmap;
import android.widget.FrameLayout;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.content.Context;
import android.media.MediaMetadata;
import android.widget.ImageButton;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import android.app.AndroidAppHelper;
import android.graphics.Bitmap.*;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.PaletteAsyncListener;
import android.widget.RelativeLayout;
import android.app.Notification;
import android.util.AttributeSet;
import android.content.IntentFilter;
import android.widget.TextClock;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.graphics.Paint;

import java.util.ArrayList;

import me.connerowen.customcover.hooks.KeyguardStatusBarViewHook;
import me.connerowen.customcover.hooks.MediaPlayerHook;
import me.connerowen.customcover.hooks.StatusBarHook;
import me.connerowen.customcover.hooks.BatteryHook;
import me.connerowen.customcover.hooks.StatusBarViewHook;
import me.connerowen.customcover.hooks.SignalClusterHook;

import de.robv.android.xposed.XposedBridge;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam;


@SuppressWarnings("RedundantArrayCreation")
public class CustomCover implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    public int defaultColor = 0xFF000000;
    public int vibrant = 0xFFFFFFFF;
    public int muted = 0xFF000000;
    public ThemeUtils themeUtil = new ThemeUtils();
    public static XSharedPreferences prefs;
    private static String MODULE_PATH = null;
    public static XModuleResources modRes = null;

    private static View mStatusBarView;
    private static View mNavigationBarView;
    private static KitKatBattery mKitKatBattery;

    private static ArrayList<ImageView> mSystemIconViews = new ArrayList<ImageView>();
    private static ArrayList<ImageView> mNotificationIconViews = new ArrayList<ImageView>();
    private static ArrayList<TextView> mTextLabels = new ArrayList<TextView>();

    /* Notification icons */
    private static LinearLayout mStatusIcons = null;

    private TextView indicationText = null;

    private LinearLayout content = null;
    private LinearLayout wrapper = null;
    private ImageView album = null;

    private MediaPlayer mPlayer = null;

    private static FrameLayout mobileCombo = null;
    private int screenDPI;

    private static int mColorForStatusIcons = 0;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XposedBridge.log("AlbumArt initZygote");

        mStatusBarView = null;

        MODULE_PATH = startupParam.modulePath;
        prefs = new XSharedPreferences(CustomCover.class.getPackage().getName());
    }

    private final XC_MethodHook sInitHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            int result = 30000;
            param.setResult(result);
        }
    };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals("android")) {
            if (!Utils.romIsCM13) {
                XposedBridge.log("found android");
                final Class<?> hookClass = XposedHelpers.findClass("com.android.server.power.PowerManagerService", lpparam.classLoader);
                XposedBridge.log("found powermanagerservice");
                XposedBridge.hookAllMethods(hookClass, "getScreenOffTimeoutLocked", sInitHook);
            }
            else if (Utils.romIsCM13) {
                XposedBridge.log("ROM is CM13");
            }
        }


        else if (!lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        new StatusBarHook(this, lpparam.classLoader);
        new StatusBarViewHook(this, lpparam.classLoader);
        //TODO: Fix BatteryHook for CM ROM
        new BatteryHook(this, lpparam.classLoader);
        new SignalClusterHook(this, lpparam.classLoader);
        new KeyguardStatusBarViewHook(this, lpparam.classLoader);


    } //end handleloadpackage

    public void setStatusBarIconsTint(int iconTint) {

        mColorForStatusIcons = iconTint;
        try {
            if (mSystemIconViews != null) {
                for (ImageView view : mSystemIconViews) {
                    if (view != null) {
                        view.setColorFilter(iconTint, PorterDuff.Mode.SRC_ATOP);
                    } else {
                        mSystemIconViews.remove(view);
                    }
                }
            }

            if (mNotificationIconViews != null) {
                for (ImageView view : mNotificationIconViews) {
                    if (view != null) {
                        view.setColorFilter(iconTint, PorterDuff.Mode.SRC_ATOP);
                    } else {
                        mNotificationIconViews.remove(view);
                    }
                }
            }

            if (mTextLabels != null) {
                for (TextView view : mTextLabels) {
                    if (view != null) {
                        view.setTextColor(iconTint);
                    } else {
                        mTextLabels.remove(view);
                    }
                }
            }

            if (mStatusBarView != null) {
                Intent intent = new Intent("gravitybox.intent.action.STATUSBAR_COLOR_CHANGED");
                intent.putExtra("iconColorEnable", true);
                intent.putExtra("iconColor", iconTint);
                mStatusBarView.getContext().sendBroadcast(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setColorForLayout(mStatusIcons, iconTint, PorterDuff.Mode.SRC_ATOP);
        setKitKatBatteryColor(iconTint);
    }

    private static void setColorForLayout(LinearLayout statusIcons, int color, PorterDuff.Mode mode) {
        if (color == 0)
            return;

        if (statusIcons == null)
            return;

        for (int i = 0; i < statusIcons.getChildCount(); i++) {
            View childView = statusIcons.getChildAt(i);
            if (childView instanceof ImageView) {
                ImageView view = (ImageView) childView;
                if (view != null) {
                    view.setColorFilter(color, mode);
                }
            }
        }
    }

    private void setKitKatBatteryColor(int iconColor) {
        if (mKitKatBattery == null) {
            return;
        }
        mKitKatBattery.updateBattery(iconColor);
    }

    public void addSystemIconView(ImageView imageView) {
        addSystemIconView(imageView, false);
    }

    public void addTextLabel(TextView textView) {
        mTextLabels.add(textView);

    }


    public void setColorTest(int color) {
        vibrant = color;
    }
    public int getColorTest() {
        return vibrant;
    }

    public void setColorMuted(int color) {
        muted = color;
    }
    public int getColorMuted() {
        return muted;
    }

    public void setContent(LinearLayout content1) {
        content = content1;
    }
    public LinearLayout getContent() {
        return content;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        mPlayer = mediaPlayer;
    }
    public MediaPlayer getMediaPlayer() {
        return mPlayer;
    }

    public void setWrapper(LinearLayout wrapper1) {
        wrapper = wrapper1;
    }
    public LinearLayout getWrapper() {
        return wrapper;
    }

    public void setAlbum(ImageView album1) {
        album = album1;
    }
    public ImageView getAlbum() {
        return album;
    }

    public void setDPI(int dpi) {
        screenDPI = dpi;
    }
    public int getDPI() {
        return screenDPI;
    }

    public XSharedPreferences getPrefs() {
        return prefs;
    }

    public void addSystemIconView(ImageView imageView, boolean applyColor) {
        if (!mSystemIconViews.contains(imageView))
            mSystemIconViews.add(imageView);

        if (applyColor) {
            imageView.setColorFilter(mColorForStatusIcons, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public void setStatusBarView(View view) {
        mStatusBarView = view;
    }

    public void setKitKatBatteryView(KitKatBattery kitkatBattery) {
        mKitKatBattery = kitkatBattery;
        setKitKatBatteryColor(getColorTest());
    }

    public void refreshStatusIconColors() {
        if (mStatusIcons != null)
            setColorForLayout(mStatusIcons, mColorForStatusIcons, PorterDuff.Mode.SRC_ATOP);
    }

}

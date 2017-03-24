package me.connerowen.customcover.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.connerowen.customcover.CustomCover;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import me.connerowen.customcover.Utils;

public class MediaPlayerHook {
    private CustomCover mInstance;

    private XC_MethodHook mediaHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedBridge.log("we're in mediaplayer");
            mInstance.setMediaPlayer((MediaPlayer) param.thisObject);
        }
    };

    public MediaPlayerHook(CustomCover instance, ClassLoader classLoader) {
        mInstance = instance;

        XposedBridge.log("about to try and hook mediaplayer");
        try {
            XposedBridge.hookAllConstructors(XposedHelpers.findClass("android.media.MediaPlayer", classLoader), mediaHook);
        } catch (Throwable t2) {
            XposedBridge.log("<<<========== MediaNotificationSeekBar MediaPlayer exception captured: " + t2.toString());
        }
    }
}

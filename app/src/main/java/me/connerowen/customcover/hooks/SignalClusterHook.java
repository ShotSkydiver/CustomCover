package me.connerowen.customcover.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.robv.android.xposed.XposedBridge;
import me.connerowen.customcover.CustomCover;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import me.connerowen.customcover.Utils;

public class SignalClusterHook {
    private static final String[] SIGNAL_CLUSTER_ICON_NAMES = {
            "mWifi", "mEthernet", "mAirplane"
    };

    private CustomCover mInstance;

    public SignalClusterHook(CustomCover instance, ClassLoader classLoader) {
        mInstance = instance;
        doHooks(classLoader);
    }


    private XC_MethodHook mSignalClusterHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            for (String name : SIGNAL_CLUSTER_ICON_NAMES) {
                try {
                    ImageView view = (ImageView) XposedHelpers.getObjectField(param.thisObject, name);
                    mInstance.addSystemIconView(view);
                } catch (NoSuchFieldError e) {
                    XposedBridge.log("Couldn't find field " + name + "in class " + param.getClass().getName());
                }
            }
        }
    };

    private void doHooks(ClassLoader classLoader) {
        String className = "com.android.systemui.statusbar.SignalClusterView";
        String methodName = "onAttachedToWindow";
        try {
            Class<?> SignalClusterView = XposedHelpers.findClass(className, classLoader);

            XposedBridge.log("signalclusterview class found");


            try {
                findAndHookMethod(SignalClusterView, methodName, mSignalClusterHook);
            } catch (NoSuchMethodError e) {
                XposedBridge.log("Not hooking method " + className + "." + methodName);
            }
        } catch (ClassNotFoundError e) {
            // Really shouldn't happen, but we can't afford a crash here.
            XposedBridge.log("Not hooking class: " + className);
        }
    }


}

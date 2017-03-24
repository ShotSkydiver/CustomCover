package me.connerowen.customcover.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import me.connerowen.customcover.CustomCover;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import me.connerowen.customcover.Utils;

public class StatusBarViewHook {
    private CustomCover mInstance;


    public StatusBarViewHook(CustomCover instance, ClassLoader classLoader) {
        mInstance = instance;
        try {
            Class<?> PhoneStatusBarView = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", classLoader);
            XposedBridge.log("phonestatusbarview class found");
            XposedBridge.hookAllConstructors(PhoneStatusBarView, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    IntentFilter lockscreenFilter = new IntentFilter();
                    lockscreenFilter.addAction(Intent.ACTION_SCREEN_ON);
                    lockscreenFilter.addAction(Intent.ACTION_SCREEN_OFF);
                    lockscreenFilter.addAction(Intent.ACTION_USER_PRESENT);
                    context.registerReceiver(new BroadcastReceiver() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onReceive(Context context, Intent intent) {

                            if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                                mInstance.setStatusBarIconsTint(Color.WHITE);

                            }

                            if (Utils.isKeyguardLocked(context)) {
                                mInstance.setStatusBarIconsTint(mInstance.getColorTest());


                            }
                        }
                    }, lockscreenFilter);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mInstance.setStatusBarView((View) param.thisObject);
                }
            });
        } catch (ClassNotFoundError e) {
        }
    }
}
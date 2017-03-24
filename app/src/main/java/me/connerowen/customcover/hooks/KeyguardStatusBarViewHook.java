package me.connerowen.customcover.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.PowerManager;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.view.ViewOverlay;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;
import me.connerowen.customcover.CustomCover;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import me.connerowen.customcover.ThemeUtils;
import me.connerowen.customcover.Utils;

public class KeyguardStatusBarViewHook {
    private CustomCover mInstance;
    public ThemeUtils themeUtil = new ThemeUtils();
    public static final String INTENT_SPOTIFY_PLAYBACK_STATE_CHANGED = "com.spotify.music.playbackstatechanged";
    public static final String INTENT_SPOTIFY_METADATA_CHANGED = "com.spotify.music.metadatachanged";

    public KeyguardStatusBarViewHook(CustomCover instance, ClassLoader classLoader) {
        mInstance = instance;
        try {
            Class<?> KeyguardStatusBarView = findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", classLoader);
            XposedBridge.log("KeyguardStatusBarView class found");
            XposedBridge.hookAllConstructors(KeyguardStatusBarView, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    IntentFilter lockscreenFilter2 = new IntentFilter();
                    lockscreenFilter2.addAction(Intent.ACTION_SCREEN_ON);
                    lockscreenFilter2.addAction(Intent.ACTION_SCREEN_OFF);
                    lockscreenFilter2.addAction(Intent.ACTION_USER_PRESENT);
                    lockscreenFilter2.addAction(INTENT_SPOTIFY_PLAYBACK_STATE_CHANGED);
                    lockscreenFilter2.addAction(INTENT_SPOTIFY_METADATA_CHANGED);
                    context.registerReceiver(new BroadcastReceiver() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            RelativeLayout keyguardStatusBarView = (RelativeLayout) param.thisObject; // id: keyguard_header

                            LinearLayout system_icons_super_container = (LinearLayout) keyguardStatusBarView.getChildAt(1); // id: system_icons_super_container
                            FrameLayout system_icons_container = (FrameLayout) system_icons_super_container.getChildAt(0);
                            LinearLayout system_icons = (LinearLayout) system_icons_container.getChildAt(0);
                            LinearLayout signal_cluster = (LinearLayout) system_icons.getChildAt(1);
                            LinearLayout mobile_signal_group = (LinearLayout) signal_cluster.getChildAt(4);

                            // this is for if there is no SIM inserted
                            FrameLayout no_sims_combo = (FrameLayout) signal_cluster.getChildAt(5);
                            ImageView no_sims = null;
                            try {
                                no_sims = (ImageView) no_sims_combo.getChildAt(0);
                            } catch (NullPointerException e) {
                                //XposedBridge.log("there is a SIM");
                            }

                            //mobile_combo is FrameLayout on stock ROM
                            FrameLayout mobile_combo_wrapper;
                            try {
                                mobile_combo_wrapper = (FrameLayout) mobile_signal_group.getChildAt(0);

                            } catch (ClassCastException exc) {
                                XposedBridge.log("cannot cast framelayout to linearlayout, we're not on stock ROM");
                                LinearLayout mobile_combo = (LinearLayout) mobile_signal_group.getChildAt(0);
                                mobile_combo_wrapper = (FrameLayout) mobile_combo.getChildAt(0);

                            }
                            ImageView mobile_signal = null;
                            ImageView mobile_type = null;

                            try {
                                mobile_signal = (ImageView) mobile_combo_wrapper.getChildAt(0);
                                mobile_type = (ImageView) mobile_combo_wrapper.getChildAt(2);
                            }
                            catch (NullPointerException e) {
                                //XposedBridge.log("no SIM is inserted, mobile data bars do not exist");
                            }


                            if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                                try {
                                    mobile_signal.setColorFilter(mInstance.getColorTest(), PorterDuff.Mode.SRC_ATOP);
                                    mobile_type.setColorFilter(mInstance.getColorTest(), PorterDuff.Mode.SRC_ATOP);
                                } catch (NullPointerException e) {
                                    no_sims.setColorFilter(mInstance.getColorTest(), PorterDuff.Mode.SRC_ATOP);
                                }
                            }

                            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                                try {
                                    mobile_signal.setColorFilter(mInstance.getColorTest(), PorterDuff.Mode.SRC_ATOP);
                                    mobile_type.setColorFilter(mInstance.getColorTest(), PorterDuff.Mode.SRC_ATOP);
                                } catch (NullPointerException e) {
                                    no_sims.setColorFilter(mInstance.getColorTest(), PorterDuff.Mode.SRC_ATOP);
                                }
                            }

                            if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                                try {
                                    mobile_signal.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                                    mobile_type.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                                } catch (NullPointerException e) {
                                    no_sims.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                                }
                            }
                        }
                    }, lockscreenFilter2);
                }
            });

            findAndHookMethod(KeyguardStatusBarView, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    TextView carrierLabel = (TextView) XposedHelpers.getObjectField(param.thisObject, "mCarrierLabel");
                    /* carrierLabel.setShadowLayer(
                            1.5f, // radius
                            2.0f, // dx
                            2.0f, // dy
                            Color.parseColor("#FF303030") // shadow color
                    ); */
                    mInstance.addTextLabel(carrierLabel);
                    TextView batteryLevel = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                    /* batteryLevel.setShadowLayer(
                            1.5f, // radius
                            2.0f, // dx
                            2.0f, // dy
                            Color.parseColor("#FF303030") // shadow color
                    ); */
                    mInstance.addTextLabel(batteryLevel);
                }
            });
        } catch (ClassNotFoundError e) {
        }
    }
}
package me.connerowen.customcover.hooks;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.lang.reflect.Method;

import me.connerowen.customcover.CustomCover;
import me.connerowen.customcover.KitKatBattery;
import me.connerowen.customcover.R;
import me.connerowen.customcover.Utils;

public class BatteryHook {
    private CustomCover mInstance;
    private static final String[] BATTERY_VIEWS_CLASSES = new String[]{
            "com.android.systemui.BatteryMeterView",
            "com.android.systemui.BatteryCircleMeterView",
            "com.android.systemui.BatteryPercentMeterView"
    };

    private Context context = null;

    public BatteryHook(final CustomCover instance, ClassLoader classLoader) {
        mInstance = instance;


        XposedBridge.log("in batteryhook");

        Class<?> BatteryView = findClass("com.android.systemui.BatteryMeterView", classLoader);
        XposedBridge.log("batteryview: class found");
        XposedBridge.hookAllConstructors(BatteryView, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                if (!Utils.romIsCM13) {
                    XposedBridge.log("batteryview: setting context");
                    context = (Context) param.args[0];
                }

                /*View batteryView = (View) param.thisObject;
                AttributeSet attrs = (AttributeSet) param.args[1];
                //Resources res = batteryView.getResources();

                final int[] arrayN = new int[3];
                arrayN[0] = 4;
                arrayN[1] = 15;
                arrayN[2] = 100;
                final int N = 3;
                final int[] mmColors = new int[2*N];
                for (int i=0; i<N; i++) {
                    mmColors[2*i] = arrayN[i];
                    mmColors[2*i+1] = 0xFFFF3300;
                }
                XposedBridge.log("changed color");
                XposedHelpers.setObjectField(param.thisObject, "mColors", mmColors);

                context = (Context) param.args[0];

                /*XposedBridge.log("about to change color");
                final int[] colors = (int[]) getObjectField(param.thisObject, "mColors");
                XposedBridge.log("color before change: " + colors[colors.length - 1]);
                colors[colors.length - 1] = Color.YELLOW;
                setObjectField(param.thisObject, "mColors", colors);

                final Paint framePaint = (Paint) getObjectField(param.thisObject, "mFramePaint");
                framePaint.setColor(Color.GREEN);
                framePaint.setAlpha(100);

                final Paint batteryPaint = (Paint) getObjectField(param.thisObject, "mBatteryPaint");
                batteryPaint.setColor(Color.BLUE);
                batteryPaint.setAlpha(100);

                int lightModeFillColor = (int) getObjectField(param.thisObject, "mLightModeFillColor");
                lightModeFillColor = Color.CYAN;
                XposedHelpers.setObjectField(param.thisObject, "mLightModeFillColor", lightModeFillColor);

                int lightModeBGColor = (int) getObjectField(param.thisObject, "mLightModeBackgroundColor");
                lightModeBGColor = Color.MAGENTA;
                XposedHelpers.setObjectField(param.thisObject, "mLightModeBackgroundColor", lightModeBGColor);

                batteryView.invalidate();
                */
            }
        });

        //TODO: Doesn't work on CM13


        if (!Utils.romIsCM13) {
            XposedBridge.log("batteryview: ROM is stock");

            findAndHookMethod(BatteryView, "draw", Canvas.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("batteryhook: drawing battery");
                    Canvas c = (Canvas) param.args[0];
                    Path shapePath = (Path) XposedHelpers.getObjectField(param.thisObject, "mShapePath");
                    Path shapeNewPath = shapePath;
                    Paint defaultBatteryPaint = (Paint) XposedHelpers.getObjectField(param.thisObject, "mBatteryPaint");
                    Paint batteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    batteryPaint.setDither(true);
                    batteryPaint.setStrokeWidth(0);
                    batteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    if (context != null) {
                        XposedBridge.log("batteryhook: context not null");
                        if (Utils.isKeyguardLocked(context)) {
                            XposedBridge.log("batteryhook: keyguard locked");
                            batteryPaint.setColor(mInstance.getColorTest());
                        } else {
                            XposedBridge.log("batteryhook: keyguard not locked");
                            batteryPaint.setColor(defaultBatteryPaint.getColor());
                        }
                    } else {
                        XposedBridge.log("batteryhook: context is null");
                        batteryPaint.setColor(defaultBatteryPaint.getColor());
                    }

                    c.drawPath(shapeNewPath, batteryPaint);
                }
            });
        }


        /*
        XC_MethodHook drawHook = new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("drawing battery");
                Canvas c = (Canvas) param.args[0];
                Path shapePath = (Path) XposedHelpers.getObjectField(param.thisObject, "mShapePath");
                Path shapeNewPath = shapePath;
                Paint defaultBatteryPaint = (Paint) XposedHelpers.getObjectField(param.thisObject, "mBatteryPaint");
                Paint batteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                batteryPaint.setDither(true);
                batteryPaint.setStrokeWidth(0);
                batteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                if (context != null) {
                    if (Utils.isKeyguardLocked(context)) {
                        batteryPaint.setColor(mInstance.getColorTest());
                    }
                    else {
                        batteryPaint.setColor(defaultBatteryPaint.getColor());
                    }
                }
                else {
                    XposedBridge.log("context is null");
                    batteryPaint.setColor(defaultBatteryPaint.getColor());
                }

                c.drawPath(shapeNewPath, batteryPaint);
            }
        };

        try {
            Method drawMethod = findMethodBestMatch(BatteryView, "draw", Canvas.class);

            XposedBridge.hookMethod(drawMethod, drawHook);
        }
        catch (NoSuchMethodError e) {
            XposedBridge.log("Couldn't find draw method in batterymeterview");
        }
        */

        /*for (final String batteryViewClass : BATTERY_VIEWS_CLASSES) {
            try {
                Class<?> BatteryView = findClass(batteryViewClass, classLoader);
                findAndHookMethod(BatteryView, "onAttachedToWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        View batteryView = (View) param.thisObject;
                        int parentId = ((View) batteryView.getParent()).getId();
                        Resources res = batteryView.getResources();

                        XposedBridge.log("battery class:" + batteryViewClass);
                        //if (parentId != res.getIdentifier("signal_battery_cluster", "id", "com.android.systemui"))
                        //    return;

                        XposedBridge.log("if test passed");

                        //int visibility = (Integer) callMethod(param.thisObject, "getVisibility");
                        //if (visibility == View.VISIBLE)
                            mInstance.setKitKatBatteryView(new KitKatBattery(batteryView, batteryViewClass));
                    }
                });
            } catch (XposedHelpers.ClassNotFoundError ignored) {
            }
        }*/
    }
}
package com.example.datainsert.winlator.all;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

import android.annotation.SuppressLint;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.EnvVars;
import com.winlator.renderer.GLRenderer;
import com.winlator.widget.TouchpadView;
import com.winlator.xserver.Cursor;
import com.winlator.xserver.CursorManager;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.requests.DrawRequests;

import java.nio.ByteBuffer;
import java.util.Locale;

public class XserverNavMenuControl {
    private static final String TAG = "XserverNavMenuControl";
    private static final String PREF_KEY_IS_GAME_STYLE_CURSOR = "IS_GAME_STYLE_CURSOR";
    public static boolean isGameStyleCursor = false;
    public static XServerDisplayActivity aInstance;

    @SuppressLint("SourceLockedOrientationActivity")
    public static void addItems(XServerDisplayActivity a) {
        try {
//            QH.refreshIsTest(a);
            Log.d(TAG, "addItems: id为啥获取不到navigationview" + R.id.NavigationView);
            NavigationView navigationView = a.findViewById(R.id.NavigationView);
            DrawerLayout drawerLayout = a.findViewById(R.id.DrawerLayout);
            PulseAudio pulseAudio = new PulseAudio(a);

            SubMenu subMenu = navigationView.getMenu().addSubMenu(10, 132, 2, QH.string.额外功能);

            if (QH.versionCode <= 5) {
                subMenu.add(PulseAudio.TITLE).setOnMenuItemClickListener(item -> {
                    pulseAudio.showDialog();
                    drawerLayout.closeDrawers();
                    return true;
                });
            }


//            if(QH.isTest){
//                subMenu.add("测试spinner").setOnMenuItemClickListener(item->{
//                    AlertDialog dialog = new AlertDialog.Builder(a).setView(R.layout.container_detail_fragment).create();
//                    dialog.show();
//                    ContainerSettings.addOptionsTest(a,dialog.findViewById(R.id.SScreenSize).getRootView());
//                    return true;
//                });
//            }


            //记得根据默认设置进行初始化

            if (QH.versionCode <= 5) {
                //pulse自动启动
                if (PulseAudio.isAutoRun(a))
                    pulseAudio.installAndExec(true);
                //添加pulse环境变量
                Log.d(TAG, "addItems: 反射获取环境变量map");
                EnvVars envVars = QH.reflectGetField(XServerDisplayActivity.class, a, "envVars",  true);
                envVars.put("PULSE_SERVER", "tcp:127.0.0.1:4713");
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    static LinearLayout linearBitmapList;
    public static void addBitmap(ByteBuffer data, int dstX, int dstY, int w, int h, int depth, DrawRequests.Format format){
        try {
            boolean depthIs1 = depth == 1;
            int[] colors = new int[w * h];
            try {
                if(depthIs1 && format == DrawRequests.Format.Z_PIXMAP){
                    byte[] bytes = data.array();
                    int stride = ((w + 32 - 1) >> 5) << 2;
                    for(int y=0; y<h; y++){
                        for(int x=0; x<w; x++){
                            int mask = (1 << (x & 7));
                            int bit = (bytes[stride*y + (x>>3)] & mask) != 0 ? 1 : 0;
                            colors[w*y+x] = bit !=0 ? Color.WHITE : Color.BLACK;
                        }
                    }
                }
                else
                    for(int i=0; i<colors.length; i++) {
                        colors[i] = depthIs1 ?data.get() : data.getInt();
                    }
            }catch (Exception e){
                e.printStackTrace();
            }
            Bitmap bitmap = Bitmap.createBitmap(colors,w, h, Bitmap.Config.ARGB_8888);
            linearBitmapList.post(()->{
                ImageView imageView = new ImageView(aInstance);
                imageView.setImageBitmap(bitmap);
                linearBitmapList.addView(imageView, 0, new ViewGroup.LayoutParams(80,80));
                View view = new View(aInstance);
                view.setBackgroundColor(Color.RED);
                view.setMinimumHeight(4);
                linearBitmapList.addView(view, 0);
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        data.rewind();
    }

    public static void addBitmap(Drawable image) {
        try {
            ByteBuffer buffer = image.getData();
            int[] colors = new int[image.width * image.height];
            for(int i=0; i<colors.length; i++) {
                colors[i] = buffer.getInt() | 0xff000000;
            }
            buffer.rewind();
            Bitmap bitmap = Bitmap.createBitmap(colors,image.width, image.height, Bitmap.Config.ARGB_8888);
            linearBitmapList.post(()->{
                ImageView imageView = new ImageView(aInstance);
                imageView.setImageBitmap(bitmap);
                linearBitmapList.addView(imageView, 0, new ViewGroup.LayoutParams(80,80));
                View view = new View(aInstance);
                view.setBackgroundColor(Color.RED);
                view.setMinimumHeight(4);
                linearBitmapList.addView(view, 0);

                imageView.setOnClickListener(v->{
                    FrameLayout frameLayout = new FrameLayout(aInstance);
                    ImageView bigImage = new ImageView(aInstance);
                    if(bitmap.getWidth()<50 || bigImage.getHeight() < 50)
                        bigImage.setLayoutParams(new FrameLayout.LayoutParams(QH.px(aInstance,160),QH.px(aInstance,160)));
                    bigImage.setImageBitmap(bitmap);
                    frameLayout.addView(bigImage);
                    new AlertDialog.Builder(aInstance)
                            .setView(frameLayout)
                            .show();
                });
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static boolean getIsGameStyleCursorFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_IS_GAME_STYLE_CURSOR, false);
    }
    public static void setIsGameStyleCursor(Context a, boolean isGame, boolean updatePef) {
        if (updatePef)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_IS_GAME_STYLE_CURSOR, isGame).apply();
        isGameStyleCursor = isGame;
    }
}

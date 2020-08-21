package com.example.wypermission.setting;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

// 具体的手机 VIVO的
public class VIVOStartSettings implements SettingInterface {

    @Override
    public Intent getActivityStartIntent(Context context) {
        Intent appIntent = context.getPackageManager().getLaunchIntentForPackage("coom.iqoo.secure");
        if (appIntent != null && Build.VERSION.SDK_INT < 23) {
            context.startActivity(appIntent);
        }
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Settings.ACTION_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }
}

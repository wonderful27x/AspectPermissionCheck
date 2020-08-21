package com.example.wypermission.setting;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

// 具体的手机 OPPO的
public class OPPOStartSettings implements SettingInterface {

    @Override
    public Intent getActivityStartIntent(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }
}

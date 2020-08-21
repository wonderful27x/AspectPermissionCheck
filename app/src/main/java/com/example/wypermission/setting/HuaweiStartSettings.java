package com.example.wypermission.setting;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

// 具体的手机 华为的
public class HuaweiStartSettings implements SettingInterface {

    @Override
    public Intent getActivityStartIntent(Context context) {
        // 华为的Intent
        Intent intent = new Intent("com.example.wypermission");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
        intent.setComponent(comp);
        return  intent;
    }
}

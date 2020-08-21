package com.example.wypermission.setting;

import android.content.Context;
import android.content.Intent;

/**
 * 通过系统去启动权限设置界面的接口，因为不同的手机有不同的启动方式
 */
public interface SettingInterface {
    //获取启动Activity的Intent
    public Intent getActivityStartIntent(Context context);
}

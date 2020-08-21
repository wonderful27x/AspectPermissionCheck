package com.example.wypermission.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @Author wonderful
 * @Description sharedPreferences持久化工具
 * @Date 2019-8-30
 */
public class MemoryUtil {

    public static final String FILE_NAME = "comExampleWonderfulChat";

    public static void sharedPreferencesSaveString(Context context,String key, String msg){
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key,msg);
        editor.apply();
    }

    public static String sharedPreferencesGetString(Context context,String key){
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getString(key,"");
    }

    public static void sharedPreferencesSaveBoolean(Context context,String key, boolean msg){
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key,msg);
        editor.apply();
    }

    public static boolean sharedPreferencesGetBoolean(Context context,String key){
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(key,false);
    }

    public static boolean sharedPreferencesGetBoolean(Context context,String key,boolean defaultType){
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(key,defaultType);
    }
}

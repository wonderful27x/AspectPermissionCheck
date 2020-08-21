package com.example.wypermission.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.collection.SimpleArrayMap;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.wypermission.setting.DefaultStartSettings;
import com.example.wypermission.setting.HuaweiStartSettings;
import com.example.wypermission.setting.OPPOStartSettings;
import com.example.wypermission.setting.SettingInterface;
import com.example.wypermission.setting.VIVOStartSettings;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关于权限校验的各种工具方法
 */
public class PermissionUtil {

    private static final String TAG = "PermissionUtil";

    //定义8种需要申请的运行时权限，key：权限，value：权限的最小sdk版本
    private static SimpleArrayMap<String,Integer> MIN_SDK_PERMISSIONS = new SimpleArrayMap<>(8);

    static {
        MIN_SDK_PERMISSIONS.put("com.android.voicemail.permission.ADD_VOICEMAIL", 14);
        MIN_SDK_PERMISSIONS.put("android.permission.BODY_SENSORS", 20);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_EXTERNAL_STORAGE", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.USE_SIP", 9);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.SYSTEM_ALERT_WINDOW", 23);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_SETTINGS", 23);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_CALENDAR", 23);
        MIN_SDK_PERMISSIONS.put("android.permission.CALL_PHONE", 23);
    }

    //定义一些常见手机类型
    public static final String MANUFACTURER_DEFAULT = "Default";//默认
    public static final String MANUFACTURER_HUAWEI = "huawei";  //华为
    public static final String MANUFACTURER_MEIZU = "meizu";    //魅族
    public static final String MANUFACTURER_XIAOMI = "xiaomi";  //小米
    public static final String MANUFACTURER_SONY = "sony";      //索尼
    public static final String MANUFACTURER_OPPO = "oppo";      //oppo
    public static final String MANUFACTURER_LG = "lg";          //lg
    public static final String MANUFACTURER_VIVO = "vivo";      //vivo
    public static final String MANUFACTURER_SAMSUNG = "samsung";//三星
    public static final String MANUFACTURER_LETV = "letv";      //乐视
    public static final String MANUFACTURER_ZTE = "zte";        //中兴
    public static final String MANUFACTURER_YULONG = "yulong";  //酷派
    public static final String MANUFACTURER_LENOVO = "lenovo";  //联想

    //实现了权限跳转的手机
    private static Map<String,Class<?>> permissionSettingPhone = new HashMap<>();

    static {
        permissionSettingPhone.put(MANUFACTURER_DEFAULT, DefaultStartSettings.class);
        permissionSettingPhone.put(MANUFACTURER_OPPO, OPPOStartSettings.class);
        permissionSettingPhone.put(MANUFACTURER_VIVO, VIVOStartSettings.class);
        permissionSettingPhone.put(MANUFACTURER_HUAWEI, HuaweiStartSettings.class);
    }

    /**
     * 判断传入的权限是否需要申请
     * @param context
     * @param permissions
     * @return 返回需要申请的权限
     */
    public static List<String> needPermissionRequest(Context context,String... permissions){
        List<String> permissionList = new ArrayList<>();
        for (String permission:permissions){
            //如果权限存在并且没有被授权则说明需要去申请权限
            if (permissionExists(permission) && !permissionGranted(context,permission)){
                permissionList.add(permission);
            }
        }
        return permissionList;
    }

    /**
     * 判断当前sdk版本中是否存在这个权限
     * @param permission
     * @return
     */
    private static boolean permissionExists(String permission){
        Integer minSdk = MIN_SDK_PERMISSIONS.get(permission);
        //如果是定义的权限库中的权限并且当前sdk版本大于等于最小运行时权限版本，则说明权限存在
        if (minSdk != null && Build.VERSION.SDK_INT >= minSdk){
            return true;
        }
        return false;
    }

    /**
     * 判断权限是否已经被授权了
     * @param context
     * @param permission
     * @return
     */
    private static boolean permissionGranted(Context context,String permission){
        int granted = ContextCompat.checkSelfPermission(context, permission);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 验证授权结果判断权限是否真的申请成功了
     * @param grantedResults
     * @return 返回授权结果
     */
    public static PermissionResult permissionRequestResult(Activity activity,String[] permissions,int[] grantedResults){
        PermissionResult permissionResult = new PermissionResult();
        for (int index=0; index<grantedResults.length; index++){
            int result = grantedResults[index];
            //如果授权了则加入授权列表
            if (result == PackageManager.PERMISSION_GRANTED){
                permissionResult.granted.add(permissions[index]);
            }else if (result == PackageManager.PERMISSION_DENIED){
                //如果是拒绝了权限
                if(shouldShowRequestPermissionRationale(activity,permissions[index])){
                    permissionResult.denied.add(permissions[index]);
                }
                //否则是拒绝了并勾选了不再提醒
                else {
                    permissionResult.cancel.add(permissions[index]);
                }
            }
        }
        return permissionResult;
    }

    /**
     * 判断是否需要提示用户权限被拒绝了
     * @param activity
     * @param permission
     * @return
     *
     * shouldShowRequestPermissionRationale
     * 1，在允许询问时返回true ；
     * 2，在权限通过 或者权限被拒绝并且禁止询问时返回false 但是有一个例外，就是重来没有询问过的时候，
     * 也是返回的false 所以单纯的使用shouldShowRequestPermissionRationale去做什么判断，
     * 是没用的，只能在请求权限回调后再使用。
     * Google的原意是：
     * 1，没有申请过权限，申请就是了，所以返回false；
     * 2，申请了用户拒绝了，那你就要提示用户了，所以返回true；
     * 3，用户选择了拒绝并且不再提示，那你也不要申请了，也不要提示用户了，所以返回false；
     * 4，已经允许了，不需要申请也不需要提示，所以返回false；
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission){
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,permission)){
            return true;
        }
        return false;
    }

    /**
     * 跳转到系统权限设置界面
     */
    public static void startAndroidSettings(Context context){
        //从实现了跳转的集合中获取当前手机的跳转Class
        Class<?> aClass = permissionSettingPhone.get(Build.MANUFACTURER.toLowerCase());
        if (aClass != null){
            try {
                SettingInterface settingInterface = (SettingInterface) aClass.newInstance();
                Intent intent = settingInterface.getActivityStartIntent(context);
                context.startActivity(intent);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

}

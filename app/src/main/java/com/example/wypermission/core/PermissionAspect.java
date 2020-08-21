package com.example.wypermission.core;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import com.example.wypermission.annotation.IPermission;
import com.example.wypermission.annotation.Permission;
import com.example.wypermission.annotation.PermissionCancel;
import com.example.wypermission.annotation.PermissionDenied;
import com.example.wypermission.utils.MemoryUtil;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.Map;

/**
 * 权限申请切面
 */
@Aspect
public class PermissionAspect {

    private static final String TAG = "PermissionAspect";

    //&& @annotation(permission)能够获取注解
    @Pointcut("execution(@com.example.wypermission.annotation.Permission * *(..)) && @annotation(permission)")
    public void permissionPointCut(Permission permission){}

    @Around("permissionPointCut(permission)")
    public void executeAnnotationMethod(final ProceedingJoinPoint joinPoint, final Permission permission){
        final Object object = joinPoint.getThis();
        if (object == null){
            Log.d(TAG, "Aspect calls an error of null object!");
            throw new NullPointerException("Aspect calls an error of null object!");
        }
        Activity context = null;
        if (object instanceof Activity){
            context = (Activity) object;
        }else if (object instanceof Fragment){
            context = ((Fragment)object).getActivity();
        }

        if (context == null){
            Log.d(TAG, "Aspect calls an error of null Activity!");
            throw new NullPointerException("Aspect calls an error of null Activity!");
        }

        //先判断权限是否已经授权了
        List<String> needPermissions = PermissionUtil.needPermissionRequest(context,permission.value());
        if (needPermissions.size() == 0){
            try {
                joinPoint.proceed();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return;
        }else {
            //获取被拒绝了并且没有勾选不再提醒的权限数量
            int count = 0;
            for (String needs:needPermissions){
                if (PermissionUtil.shouldShowRequestPermissionRationale(context,needs)){
                    count++;
                }
            }
            //如果是0并且不是第一次申请权限，说明所有的权限都是勾选了不在提醒的权限，则不在启动权限申请界面而是跳转到系统设置界面
            //测试发现调用系统禁止权限shouldShowRequestPermissionRationale为true
            boolean firstPermissionCheck = MemoryUtil.sharedPreferencesGetBoolean(context,"firstPermissionCheck",true);
            if (firstPermissionCheck){
                MemoryUtil.sharedPreferencesSaveBoolean(context,"firstPermissionCheck",false);
            }
            if (count == 0 && !firstPermissionCheck){
                PermissionUtil.startAndroidSettings(context);
                return;
            }
        }
        //转成数组
        final String[] permissionArray = new String[needPermissions.size()];
        needPermissions.toArray(permissionArray);
        //所有的检查都通过了则将权限申请交给PermissionActivity去完成
        PermissionActivity.permissionRequest(context, permissionArray, permission.requestCode(), new IPermission() {
            @Override
            public void granted(List<String> permission) {
                printList("granted",permission);
                //权限申请通过了
                try {
                    //参数校验
                    annotationMethodLegalCheck(object,Permission.class,new Class<?>[]{List.class});
                    //获取方法的参数列表,并修改参数
                    final Object[] arguments = getArguments(joinPoint);
                    if (arguments.length >0){
                        List<String> permissions = (List<String>) arguments[0];
                        if (permission != null){
                            permissions.clear();
                            permissions.addAll(permission);
                        }
                    }
                    //执行注解方法
                    joinPoint.proceed();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }

            @Override
            public void denied(List<String> permission) {
                printList("denied",permission);
                //参数校验，并获取需要调用的方法
                Map<Method,Boolean> methodMap = annotationMethodLegalCheck(object,PermissionDenied.class,new Class<?>[]{List.class});
                //调用方法
                for (Map.Entry<Method,Boolean> entry:methodMap.entrySet()){
                    //有参数，参数可以有多个，但是一定要和annotationMethodLegalCheck校验的一致
                    if (entry.getValue()){
                        invokeAnnotation(object,entry.getKey(),permission);
                    }
                    //无参数
                    else {
                        invokeAnnotation(object,entry.getKey());
                    }
                }
            }

            @Override
            public void cancel(List<String> permission) {
                printList("cancel",permission);
                //参数校验，并获取需要调用的方法
                Map<Method,Boolean> methodMap = annotationMethodLegalCheck(object,PermissionCancel.class,new Class<?>[]{List.class});
                //调用方法
                for (Map.Entry<Method,Boolean> entry:methodMap.entrySet()){
                    //有参数，参数可以有多个，但是一定要和annotationMethodLegalCheck校验的一致
                    if (entry.getValue()){
                        invokeAnnotation(object,entry.getKey(),permission);
                    }
                    //无参数
                    else {
                        invokeAnnotation(object,entry.getKey());
                    }
                }
            }
        });
    }

    /**
     * 获取方法的实参列表
     * * AspectJ使用org.aspectj.lang.JoinPoint接口表示目标类连接点对象，如果是环绕增强时，使用org.aspectj.lang.ProceedingJoinPoint表示连接点对象，该类是JoinPoint的子接口。任何一个增强方法都可以通过将第一个入参声明为JoinPoint访问到连接点上下文的信息。我们先来了解一下这两个接口的主要方法：
     * * 1)JoinPoint
     * *    java.lang.Object[] getArgs()：获取连接点方法运行时的入参列表；
     * *    Signature getSignature() ：获取连接点的方法签名对象；
     * *    java.lang.Object getTarget() ：获取连接点所在的目标对象；
     * *    java.lang.Object getThis() ：获取代理对象本身；
     * * 2)ProceedingJoinPoint
     * * ProceedingJoinPoint继承JoinPoint子接口，它新增了两个用于执行连接点方法的方法：
     * *    java.lang.Object proceed() throws java.lang.Throwable：通过反射执行目标对象的连接点处的方法；
     * *    java.lang.Object proceed(java.lang.Object[] args) throws java.lang.Throwable：通过反射执行目标对象连接点处的方法，不过使用新的入参替换原来的入参。
     *
     * @param proceedingJoinPoint
     * @return
     */
    private Object[] getArguments(ProceedingJoinPoint proceedingJoinPoint){
        if (proceedingJoinPoint == null)return null;

        //printList("代理对象" , proceedingJoinPoint.getThis().getClass().getName());
        //printList("连接点所在的目标对象" , proceedingJoinPoint.getTarget().getClass().getName());
        //printList("连接点的方法名" , proceedingJoinPoint.getSignature().getName());

        Object[] objectArray = proceedingJoinPoint.getArgs();

        printList("实参列表" , objectArray);

        return objectArray;
    }

    /**
     * 将用户的授权结果回调给申请权限者，通过反射调用对应注解的方法来实现
     * @param object
     * @param method
     * @param parameters
     */
    public void invokeAnnotation(Object object, Method method,Object... parameters){
        try {
            method.invoke(object,parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * 注解方法的规范性检查
     * @param object 目标对象
     * @param annotationClass 注解class
     * @param requestParameters 被注解的方法所要求的参数列表
     * @return 返回需要调用的方法，key：需要调用的方法，value:是否有参数
     */
    private Map<Method,Boolean> annotationMethodLegalCheck(Object object, Class<? extends Annotation> annotationClass, Class<?> [] requestParameters){
        Map<Method,Boolean> methodMap = new HashMap<>();
        //遍历找到注解的方法
        Method[] methods = object.getClass().getDeclaredMethods();
        for (Method method:methods){
            method.setAccessible(true);
            //是被注解annotationClass注解的方法
            if (method.isAnnotationPresent(annotationClass)){
                Class<?>[] parameterTypes = method.getParameterTypes();
                //如果方法无任何参数也是默认可以的
                if (parameterTypes.length == 0){
                    methodMap.put(method,false);
                    continue;
                }
                //否则如何和要求的参数数量不一致则抛出异常
                if (parameterTypes.length != requestParameters.length){
                    throwException(annotationClass,method,requestParameters);
                }
                //如如果和要求的参数不能一一对应则抛出异常
                for (int i=0; i<parameterTypes.length; i++){
                    if (!parameterTypes[i].equals(requestParameters[i])){
                        throwException(annotationClass,method,requestParameters);
                    }
                }
                //否则说明是有参数的，加入集合保存
                methodMap.put(method,true);
            }
        }
        return methodMap;
    }

    //抛出异常
    private void throwException(Class<? extends Annotation> annotationClass,Method method,Class<?> [] requestParameters){
        throw new IllegalArgumentException(
                "\n错误：注解"
                + annotationClass.getName()
                + "注解的方法"
                + method.getDeclaringClass().getName()
                + "."
                + method.getName()
                + "定义了错误的参数！！！\n"
                + "正确的参数列表可以为空或者为：\n"
                + classNames(requestParameters)
        );
    }

    private List<String> classNames(Class<?> [] requestParameters){
        List<String> list = new ArrayList<>();
        for (Class<?> clazz:requestParameters){
            list.add(clazz.getName());
        }
        return list;
    }

    @SafeVarargs
    private final <T> void printList(String message, T... content){
        for (T str:content)
        Log.d(TAG, message + ": " + str);
    }
}

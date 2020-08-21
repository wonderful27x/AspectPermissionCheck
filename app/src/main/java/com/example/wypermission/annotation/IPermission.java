package com.example.wypermission.annotation;

import java.util.List;

/**
 * 用户对权限申请的造作结果接口
 */
public interface IPermission {
    public void granted(List<String> permission);   //同意了权限,参数：同意的授权的权限
    public void denied(List<String> permission);    //拒绝了权限,参数：拒绝授权的权限
    public void cancel(List<String> permission);    //拒绝并勾选了不在提示,参数：拒绝并勾选了不在提示的权限
}

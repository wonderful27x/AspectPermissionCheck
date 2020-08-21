package com.example.wypermission.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限申请的授权结果
 */
public class PermissionResult {

    public List<String> granted = new ArrayList<>(); //同意授权了的权限
    public List<String> denied = new ArrayList<>();  //被拒绝的权限
    public List<String> cancel = new ArrayList<>();  //被拒绝并且勾选了不再提醒的权限

}

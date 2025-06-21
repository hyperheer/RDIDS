package com.example.rdds.userInfo;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class LoginStateCache {
    private static final String PREF_NAME = "user";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_DEFECT_POINT_LIST = "defect_point_list";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public LoginStateCache(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // 保存用户信息
    public void saveUserInfo(int userId, String username, int userType) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putInt(KEY_USER_TYPE, userType);
        editor.apply();
    }

    public void saveDefectPointList(String defectPointList) {
        editor.putString(KEY_DEFECT_POINT_LIST, defectPointList);
        editor.apply();
    }

    // 获取用户 ID
    public int getUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    // 获取用户名
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    // 获取用户类型
    public int getUserType() {
        return sharedPreferences.getInt(KEY_USER_TYPE, -1);
    }

    public String getDefectPointList() { return  sharedPreferences.getString(KEY_DEFECT_POINT_LIST, ""); }

    // 清除用户信息

    public void clearUserInfo() {
        editor.clear();
        editor.apply();
    }

    // 检查用户是否已登录
    public boolean isUserLoggedIn() {
        return getUserId() != -1;
    }
}
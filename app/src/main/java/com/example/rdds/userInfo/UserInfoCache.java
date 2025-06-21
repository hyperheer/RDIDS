package com.example.rdds.userInfo;

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.content.SharedPreferences;

public class UserInfoCache {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_NAME = "Name";
    private static final String KEY_GENDER = "Gender";
    private static final String KEY_PHONE = "Phone";
    private static final String KEY_DETECTION_HISTORY = "detectionHistory";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public UserInfoCache(Context context,String PREF_NAME) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // 保存用户信息
    public void saveUserInfo(String name, int gender , String phone) {
        editor.putString(KEY_NAME, name);
        editor.putInt(KEY_GENDER, gender);
        editor.putString(KEY_PHONE, phone);
        editor.apply();
    }

    // 保存历史记录
    public void saveDetectionHistory(String detectionHistory){
        editor.putString(KEY_DETECTION_HISTORY,detectionHistory);
        editor.apply();
    }

    // 获取用户 ID
    public int getUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    // 获取用户名
    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    // 获取用户类型
    public int getUserType() {
        return sharedPreferences.getInt(KEY_USER_TYPE, -1);
    }

    // 获取用户姓名
    public String getName() {return sharedPreferences.getString(KEY_NAME, "");}

    // 获取用户性别
    public int getGender() {
        return sharedPreferences.getInt(KEY_GENDER, 0);
    }

    // 获取用户电话号码
    public String getPhone() {
        return sharedPreferences.getString(KEY_PHONE, "");
    }

    // 获取用户的检测历史记录
    public String getDetectionHistory() { return sharedPreferences.getString(KEY_DETECTION_HISTORY,""); }

    // 清除用户信息
    public void clearUserInfo() {
        editor.clear();
        editor.apply();
    }

}
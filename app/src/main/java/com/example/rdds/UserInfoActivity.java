package com.example.rdds;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.rdds.userInfo.LoginStateCache;
import com.example.rdds.userInfo.UserInfoCache;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;
import java.io.IOException;
public class UserInfoActivity extends AppCompatActivity {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String UpdataUserInfo_URL = GlobalData.getInstance().getHTTP_Address()+"/uc/update_user_info";
    private static final String GetUserInfo_URL = GlobalData.getInstance().getHTTP_Address()+"/uc/get_user_info";
    private EditText etName, etPhone;
    private Spinner spinnerGender;
    private Button btnEdit, btnConfirm;
    private String[] genders = {"男", "女"};
    private int genderPosition = 0;
    private LoginStateCache loginStateCache;
    private UserInfoCache userInfoCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        loginStateCache = new LoginStateCache(this);

        userInfoCache =new UserInfoCache(this, loginStateCache.getUsername());

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        spinnerGender = findViewById(R.id.spinner_gender);
        btnEdit = findViewById(R.id.btn_edit);
        btnConfirm = findViewById(R.id.btn_confirm);

        // 初始化性别下拉框
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // 从服务器加载数据
        loadUserData(userInfoCache.getName(),userInfoCache.getGender(),userInfoCache.getPhone());
        getServerInfoData();
        enableEditMode(false);
        // 设置性别选择监听器
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                genderPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 编辑按钮点击事件
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableEditMode(true);
            }
        });

        // 确认按钮点击事件
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateServerData();
            }
        });

        findViewById(R.id.back_to_mine).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // 加载用户数据
    private void loadUserData(String name ,int gender,String phone ) {
        etName.setText(name);
        spinnerGender.setSelection(gender);
        etPhone.setText(phone);
    }

    // 保存用户数据到本地
    private void saveUserData() {
        String name = etName.getText().toString();
        String phone = etPhone.getText().toString();
        userInfoCache.saveUserInfo(name,genderPosition,phone);
        //Toast.makeText(this, "个人信息已保存", Toast.LENGTH_SHORT).show();
    }

    // 更新服务器数据
    private void updateServerData() {

        String name = etName.getText().toString();
        String phone = etPhone.getText().toString();
        String gender = genders[genderPosition];

        OkHttpClient client = new OkHttpClient();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", loginStateCache.getUsername());
            jsonObject.put("name", name);
            jsonObject.put("gender", gender);
            jsonObject.put("phone", phone);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(UpdataUserInfo_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(UserInfoActivity.this,
                        "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    int code = json.getInt("code");
                    String message = json.getString("message");

                    runOnUiThread(() -> {
                        switch (code) {
                            case 200: // 修改成功
                                try {
                                    Toast.makeText(UserInfoActivity.this, message, Toast.LENGTH_SHORT).show();
                                    saveUserData();
                                    enableEditMode(false);
                                } catch (Exception e) {
                                    Toast.makeText(UserInfoActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 400: // 用户不存在
                                Toast.makeText(UserInfoActivity.this, message, Toast.LENGTH_SHORT).show();
                                enableEditMode(false);
                                break;
                            default:
                                Toast.makeText(UserInfoActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(UserInfoActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }



    private void getServerInfoData() {

        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", loginStateCache.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(GetUserInfo_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(UserInfoActivity.this,
                        "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    int code = json.getInt("code");
                    String message = json.getString("message");
                    runOnUiThread(() -> {
                        switch (code) {
                            case 200: // 修改成功
                                try {
                                    JSONObject data = json.getJSONObject("data");
                                    String name = data.getString("name");
                                    String gender_s = data.getString("gender");
                                    String phone = data.getString("phone");
                                    int gender=0;
                                    if (gender_s.equals("女"))  gender=1;
                                    loadUserData(name,gender,phone);
                                    //Toast.makeText(UserInfoActivity.this, message, Toast.LENGTH_SHORT).show();
                                    saveUserData();
                                } catch (Exception e) {
                                    //Toast.makeText(UserInfoActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 400: // 用户不存在
                                //Toast.makeText(UserInfoActivity.this, message, Toast.LENGTH_SHORT).show();
                                enableEditMode(false);
                                break;
                            default:
                                //Toast.makeText(UserInfoActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(UserInfoActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });



    }


    // 启用/禁用编辑模式
    private void enableEditMode(boolean enable) {
        etName.setEnabled(enable);
        etPhone.setEnabled(enable);
        spinnerGender.setEnabled(enable);
        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnConfirm.setVisibility(enable ? View.VISIBLE : View.GONE);
        btnConfirm.setEnabled(enable);
    }
}
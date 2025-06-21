package com.example.rdds;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.rdds.permission.PermissionManager;
import com.example.rdds.userInfo.LoginStateCache;
import com.example.rdds.userInfo.UserInfoCache;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private EditText et_username;
    private EditText et_password;
    private Spinner userTypeSpinner;

    private PermissionManager permissionManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String LOGIN_URL = GlobalData.getInstance().getHTTP_Address()+"/auth/login";

    private LoginStateCache loginStateCache;
    private UserInfoCache userInfoCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginStateCache=new LoginStateCache(this);
        // 检查是否已经登录
        if (loginStateCache.isUserLoggedIn()) {
            int userType = loginStateCache.getUserType();
            if (userType==0) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(LoginActivity.this, ManagerMainActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        permissionManager = PermissionManager.getInstance(this);

        // 仅在首次启动时请求权限
        if (!permissionManager.hasRequestedLocationPermission()) {
            requestLocationPermission();
        }

        //点击注册
        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到注册页面
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.user_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_selectable_list_item);
        userTypeSpinner.setAdapter(adapter);

        // 设置用户名输入过滤器
        InputFilter[] usernameFilters = new InputFilter[]{new LoginActivity.UsernameInputFilter()};
        et_username.setFilters(usernameFilters);

        // 设置密码输入过滤器
        InputFilter[] passwordFilters = new InputFilter[]{new LoginActivity.PasswordInputFilter()};
        et_password.setFilters(passwordFilters);

        // 用户名输入监听
        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().matches("^[a-zA-Z_][a-zA-Z0-9_]{5,19}$")) {
                    et_username.setError("用户名必须6-20位字母数字或下划线，且不能以数字开头");
                }
            }
        });

        // 密码输入监听
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().matches("^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{6,20}$")) {
                    et_password.setError("密码必须6-20位且包含字母、数字和特殊符号");
                }
            }
        };
        et_password.addTextChangedListener(passwordWatcher);

        findViewById(R.id.login).setOnClickListener(v -> {
            String username = et_username.getText().toString();
            String password = et_password.getText().toString();
            String userType = userTypeSpinner.getSelectedItem().toString();
            int user_type;
            if (userType.equals("作业人员")) {
                user_type = 0;
            } else {
                user_type = 1;
            }

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            } else {
                LoginUser(username, password, user_type);
            }
        });
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            // 标记已请求过权限
            permissionManager.markLocationPermissionRequested();
        }
    }

    // 用户名输入过滤器
    private class UsernameInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            String newText = dest.subSequence(0, dstart) +
                    source.subSequence(start, end).toString() +
                    dest.subSequence(dend, dest.length());

            if (!newText.matches("^[a-zA-Z_][a-zA-Z0-9_]{0,19}$")) {
                return "";
            }
            return null;
        }
    }

    // 密码输入过滤器
    private class PasswordInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            String newText = dest.subSequence(0, dstart) +
                    source.subSequence(start, end).toString() +
                    dest.subSequence(dend, dest.length());

            if (newText.length() > 20) {
                return "";
            }
            return null;
        }
    }

    private void LoginUser(String username, String password, int usertype) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("password", password);
            jsonObject.put("user_type", usertype);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this,
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
                            case 200: // 登录成功
                                try {
                                    JSONObject data = json.getJSONObject("data");
                                    int userId = data.getInt("user_id");
                                    String username = data.getString("username");
                                    int userType = data.getInt("user_type");
                                    // 保存用户信息到缓存
                                    loginStateCache.saveUserInfo(userId, username, userType);
                                    userInfoCache = new UserInfoCache(LoginActivity.this, loginStateCache.getUsername());
                                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                    if (usertype == 0) {
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    } else {
                                        startActivity(new Intent(LoginActivity.this, ManagerMainActivity.class));
                                    }
                                    finish();
                                } catch (JSONException e) {
                                    Toast.makeText(LoginActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 400: // 参数错误
                            case 401: // 密码错误
                            case 404: // 用户不存在
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(LoginActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
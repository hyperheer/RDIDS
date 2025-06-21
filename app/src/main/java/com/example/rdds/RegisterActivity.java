package com.example.rdds;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private EditText et_username;
    private EditText et_password;
    private EditText et_password2;


    private Spinner userTypeSpinner;


    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String REGISTER_URL = GlobalData.getInstance().getHTTP_Address()+"/auth/register";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        et_password2 = findViewById(R.id.et_password2);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.user_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_selectable_list_item);
        userTypeSpinner.setAdapter(adapter);

        // 设置用户名输入过滤器
        InputFilter[] usernameFilters = new InputFilter[]{new UsernameInputFilter()};
        et_username.setFilters(usernameFilters);

        // 设置密码输入过滤器
        InputFilter[] passwordFilters = new InputFilter[]{new PasswordInputFilter()};
        InputFilter[] password2Filters = new InputFilter[]{new PasswordInputFilter()};
        et_password.setFilters(passwordFilters);
        et_password2.setFilters(password2Filters);

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

        TextWatcher password2Watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().matches("^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{6,20}$")) {
                    et_password2.setError("密码必须6-20位且包含字母、数字和特殊符号");
                }
            }
        };
        et_password.addTextChangedListener(passwordWatcher);
        et_password2.addTextChangedListener(password2Watcher);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        findViewById(R.id.register).setOnClickListener(v -> {
            String username = et_username.getText().toString();
            String password = et_password.getText().toString();
            String password2 = et_password2.getText().toString();
            String userType = userTypeSpinner.getSelectedItem().toString();
            int user_type;
            if(userType.equals("作业人员")){
                user_type=0;
            }else
                user_type=1;

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(password2)) {
                Toast.makeText(RegisterActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(password2)) {
                Toast.makeText(RegisterActivity.this, "密码确认有误", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(username, password,user_type);
            }
        });
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



    private void registerUser(String username, String password,int usertype) {
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
                .url(REGISTER_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this,
                        "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    int code = json.getInt("code");
                    String message = json.getString("message");

                    runOnUiThread(() -> {
                        if (code == 200) {
                            Toast.makeText(RegisterActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "注册失败: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this,
                            "响应解析错误", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}

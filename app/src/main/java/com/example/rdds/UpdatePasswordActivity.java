package com.example.rdds;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rdds.userInfo.LoginStateCache;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.io.IOException;

public class UpdatePasswordActivity extends AppCompatActivity {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String UpdataPassword_URL = GlobalData.getInstance().getHTTP_Address()+"/uc/update_password";

    private EditText et_newpassword;
    private EditText et_newpassword2;

    private LoginStateCache loginStateCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_password);

        loginStateCache = new LoginStateCache(this);

        // 初始化控件
        et_newpassword = findViewById(R.id.et_new_password);
        et_newpassword2 = findViewById(R.id.et_confirm_password);

        // 设置密码输入过滤器
        InputFilter[] passwordFilters = new InputFilter[]{new UpdatePasswordActivity.PasswordInputFilter()};
        InputFilter[] password2Filters = new InputFilter[]{new UpdatePasswordActivity.PasswordInputFilter()};
        et_newpassword.setFilters(passwordFilters);
        et_newpassword2.setFilters(password2Filters);

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
                    et_newpassword.setError("密码必须6-20位且包含字母、数字和特殊符号");
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
                    et_newpassword2.setError("密码必须6-20位且包含字母、数字和特殊符号");
                }
            }
        };
        et_newpassword.addTextChangedListener(passwordWatcher);
        et_newpassword2.addTextChangedListener(password2Watcher);


        // 修改密码点击事件
        findViewById(R.id.btn_update_new_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String new_pwd = et_newpassword.getText().toString();
                String confirm_pwd = et_newpassword2.getText().toString();

                if(TextUtils.isEmpty(new_pwd) || TextUtils.isEmpty(confirm_pwd)){
                    Toast.makeText(UpdatePasswordActivity.this,"密码不能为空",Toast.LENGTH_SHORT).show();
                }else if(!new_pwd.equals(confirm_pwd)){
                    Toast.makeText(UpdatePasswordActivity.this,"新密码和确认密码不一致",Toast.LENGTH_SHORT).show();
                }else{
                    String username = loginStateCache.getUsername();
                    UpdataPassword(username,new_pwd);
                }
            }
        });

        // 退出修改界面
        findViewById(R.id.back_to_mine).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }



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


    private void UpdataPassword(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("new_password", password);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(UpdataPassword_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(UpdatePasswordActivity.this,
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
                                    Toast.makeText(UpdatePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                                    finish();
                                } catch (Exception e) {
                                    Toast.makeText(UpdatePasswordActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case 400: // 用户不存在
                                Toast.makeText(UpdatePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                                break;
                            case 500: // 用户不存在
                                Toast.makeText(UpdatePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(UpdatePasswordActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(UpdatePasswordActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}
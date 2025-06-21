package com.example.rdds.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rdds.GlobalData;
import com.example.rdds.LoginActivity;
import com.example.rdds.MainActivity;
import com.example.rdds.ManagerMainActivity;
import com.example.rdds.R;
import com.example.rdds.UpdatePasswordActivity;
import com.example.rdds.UserInfoActivity;
import com.example.rdds.userInfo.LoginStateCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MineFragment extends Fragment {


    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String URL = GlobalData.getInstance().getHTTP_Address()+"/ip/get_result_image";
    private LoginStateCache loginStateCache;

    private ImageView iv_t;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        iv_t=view.findViewById(R.id.iv_wky);

        loginStateCache = new LoginStateCache(requireContext());

        RelativeLayout updatePasswordLayout = view.findViewById(R.id.rl_updatapassword);

        TextView tv_username=view.findViewById(R.id.tv_username);

        tv_username.setText(loginStateCache.getUsername());

        //get();



        // 为修改密码布局设置点击事件
        updatePasswordLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建意图，跳转到修改密码页面
                Intent intent = new Intent(requireContext(), UpdatePasswordActivity.class);
                startActivity(intent);
            }
        });


        RelativeLayout userinfoLayout = view.findViewById(R.id.rl_userinfo);

        userinfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建意图，跳转到个人信息页面
                Intent intent = new Intent(requireContext(), UserInfoActivity.class);
                startActivity(intent);
            }
        });

        // 找到退出按钮
        Button logoutButton = view.findViewById(R.id.logout);

        // 获取 UserInfoCache 实例
        Context context = requireContext();
        LoginStateCache loginStateCache = new LoginStateCache(context);

        // 为退出按钮设置点击监听器
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 清除用户信息
                loginStateCache.clearUserInfo();

                // 启动 LoginActivity
                Intent intent = new Intent(context, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // 关闭当前 Activity
                requireActivity().finish();
            }
        });

        return view;
    }



    private void get() {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("result_id", 21);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();


        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 处理网络失败
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        // 成功响应：处理图片流（非 JSON 数据）
                        handleImageResponse(response);
                    } else {
                        // 错误响应：解析 JSON 错误信息
                        handleErrorResponse(response);
                    }
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                            "响应处理异常", Toast.LENGTH_SHORT).show());
                }
            }

            /**
             * 处理成功响应（图片流）
             */
            private void handleImageResponse(Response response) throws IOException {
                // 获取图片输入流（示例：显示在 ImageView 中）
                InputStream inputStream = response.body().byteStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                requireActivity().runOnUiThread(() -> {
                    if (bitmap != null) {
                        // 在 UI 线程中设置图片（需替换为实际 ImageView 变量）
                        iv_t.setImageBitmap(bitmap);
                        Toast.makeText(requireContext(), "图片加载成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "图片解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            /**
             * 处理错误响应（JSON 格式）
             */
            private void handleErrorResponse(Response response) throws IOException {

                try {
                    String errorBody = response.body().string();
                    JSONObject json = new JSONObject(errorBody);
                    int code = json.getInt("code");
                    String message = json.getString("message");

                    requireActivity().runOnUiThread(() -> {
                        switch (code) {
                            case 400: // 缺少参数
                                Toast.makeText(requireContext(), "参数错误: " + message, Toast.LENGTH_SHORT).show();
                                break;
                            case 404: // 记录或文件不存在
                                Toast.makeText(requireContext(), "未找到资源: " + message, Toast.LENGTH_SHORT).show();
                                break;
                            case 500: // 服务器错误
                                Toast.makeText(requireContext(), "服务器异常: " + message, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(requireContext(), "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                }catch (Exception e) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }

            }
        });
    }
}
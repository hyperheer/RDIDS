package com.example.rdds;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;




public class DefectDetailActivity extends AppCompatActivity {

    private JSONObject defectPoint;
    private ImageView iv_defectPhoto;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String URL = GlobalData.getInstance().getHTTP_Address()+"/ip/get_result_image";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defect_detail);


        iv_defectPhoto=findViewById(R.id.iv_defectPhoto);
        Intent intent = getIntent();

        String jsonData = intent.getStringExtra("defectPoint");
        if (jsonData != null) {
            try{
                defectPoint = new JSONObject(jsonData);
                if (defectPoint!=null) {
                    // 显示详细信息
                    TextView usernameTv = findViewById(R.id.username);
                    TextView locationTv = findViewById(R.id.location);
                    TextView timeTv = findViewById(R.id.time);
                    TextView typeTv = findViewById(R.id.type);
                    TextView severityTv = findViewById(R.id.severity);

                    usernameTv.setText("上报人: " + defectPoint.getString("username"));
                    locationTv.setText("位置: " + defectPoint.getString("gpsLocation"));
                    timeTv.setText("检测时间: " + defectPoint.getString("detectionTime"));
                    typeTv.setText("缺陷类型: " + defectPoint.getString("defectType"));
                    severityTv.setText("严重程度: " + defectPoint.getString("severity"));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        getDefectPhoto();

        findViewById(R.id.wky).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void getDefectPhoto() {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("result_id", defectPoint.getString("resultId"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(DefectDetailActivity.this,
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
                    runOnUiThread(() -> Toast.makeText(DefectDetailActivity.this,
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

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        // 在 UI 线程中设置图片（需替换为实际 ImageView 变量）
                        iv_defectPhoto.setImageBitmap(bitmap);
                        Toast.makeText(DefectDetailActivity.this, "图片加载成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DefectDetailActivity.this, "图片解析失败", Toast.LENGTH_SHORT).show();
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

                    runOnUiThread(() -> {
                        switch (code) {
                            case 400: // 缺少参数
                                Toast.makeText(DefectDetailActivity.this, "参数错误: " + message, Toast.LENGTH_SHORT).show();
                                break;
                            case 404: // 记录或文件不存在
                                Toast.makeText(DefectDetailActivity.this, "未找到资源: " + message, Toast.LENGTH_SHORT).show();
                                break;
                            case 500: // 服务器错误
                                Toast.makeText(DefectDetailActivity.this, "服务器异常: " + message, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(DefectDetailActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                }catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(DefectDetailActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }

            }
        });
    }



}    
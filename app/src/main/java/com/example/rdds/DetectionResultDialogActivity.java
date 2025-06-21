package com.example.rdds;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rdds.detectionResult.DetectionResult;
import com.example.rdds.userInfo.LoginStateCache;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetectionResultDialogActivity extends AppCompatActivity {

    private JSONObject dataObject;
    private String results="";

    private LoginStateCache loginStateCache;

    private TextView location;
    private TextView time;
    private TextView type;
    private TextView severity;
    private TextView detecting;
    // 初始化按钮
    private Button btnCancel;
    private Button btnSave;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String SERVER_URL = GlobalData.getInstance().getHTTP_Address()+"/ip/confirm_save";
    private static final String UpImage_URL = GlobalData.getInstance().getHTTP_Address()+"/ip/image_process";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_detection_result);
        loginStateCache =new LoginStateCache(this);
        location = findViewById(R.id.location);
        time = findViewById(R.id.time);
        type = findViewById(R.id.type);
        severity = findViewById(R.id.yan);
        detecting =findViewById(R.id.detecting);
        // 初始化按钮
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSave);

        // 获取传递过来的数据
        Intent intent = getIntent();

        String PhotoPath=intent.getStringExtra("PhotoPath");
        String gps=intent.getStringExtra("gps");

        uploadImageWithOkHttp(PhotoPath,gps);
        Log.d("wky66", "onCreate: "+results);




        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建 OkHttp 客户端
                OkHttpClient client = new OkHttpClient();

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("result_id", dataObject.getString("result_id"));
                    jsonObject.put("save", 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetectionResultDialogActivity.this, "JSON创建失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(DetectionResultDialogActivity.this,
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
                                            //Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Toast.makeText(DetectionResultDialogActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                        }
                                        break;
                                    case 400: // 用户不存在
                                        Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 404: // 用户不存在
                                        Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 500: // 用户不存在
                                        Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        Toast.makeText(DetectionResultDialogActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(DetectionResultDialogActivity.this,
                                    "响应解析异常", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkHttpClient client = new OkHttpClient();

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("result_id", dataObject.getString("result_id"));
                    jsonObject.put("save", 1);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetectionResultDialogActivity.this, "JSON创建失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(DetectionResultDialogActivity.this,
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
                                            Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Toast.makeText(DetectionResultDialogActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                        }
                                        break;
                                    case 400: // 用户不存在
                                        Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 404: // 用户不存在
                                        Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 500: // 用户不存在
                                        Toast.makeText(DetectionResultDialogActivity.this, message, Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        Toast.makeText(DetectionResultDialogActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(DetectionResultDialogActivity.this,
                                    "响应解析异常", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
                finish();
            }
        });
    }

    private void settext(JSONObject dataObject) {
        //location.setText("拍摄位置:" + dataObject.getString("gps_location"));
    }


    public void uploadImageWithOkHttp(String imagePath,String gpsLocation) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
        }

        // 创建 OkHttp 客户端
        OkHttpClient client = new OkHttpClient();

        // 获取当前用户信息和位置信息
        String userName = loginStateCache.getUsername();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String detectionTime = sdf.format(new Date());

        // 构建 MultipartBody
        MediaType mediaType = MediaType.parse("image/jpg");
        RequestBody imageBody = RequestBody.create(imageFile, mediaType);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_name", userName)
                .addFormDataPart("gps_location", gpsLocation)
                .addFormDataPart("detection_time", detectionTime)
                .addFormDataPart("image", imageFile.getName(), imageBody)
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(UpImage_URL)
                .post(requestBody)
                .build();

        // 发送异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        // 获取 data 字段对应的 JSON 对象
                        dataObject = jsonObject.getJSONObject("data");

                        DetectionResult resultSingleton = DetectionResult.getInstance();

                        resultSingleton.addData(dataObject);

                        // 删除图片
                        File photoFile = new File(imagePath);
                        if (photoFile.exists()) {
                            boolean deleted = photoFile.delete();
                            if (deleted) {
                                Log.d(TAG, "临时图片已删除: " + imagePath);
                            } else {
                                Log.e(TAG, "删除临时图片失败: " + imagePath);
                            }
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(DetectionResultDialogActivity.this,
                                    "检测成功!",
                                    Toast.LENGTH_LONG).show();
                            try {
                                time.setText("拍摄时间:" + dataObject.getString("detection_time"));
                                location.setText("拍摄位置:" + dataObject.getString("gps_location"));
                                type.setText("检测类型:" + dataObject.getString("defect_type"));
                                severity.setText("严重程度:" + dataObject.getInt("severity"));
                                detecting.setText("检测结果");
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(DetectionResultDialogActivity.this, "解析数据失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                            detecting.setText("检测失败");
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(DetectionResultDialogActivity.this, "上传失败：" + response.code() + response.message(), Toast.LENGTH_LONG).show();
                        detecting.setText("检测失败");
                    });
                    // 删除图片
                    File photoFile = new File(imagePath);
                    if (photoFile.exists()) {
                        boolean deleted = photoFile.delete();
                        if (deleted) {
                            Log.d(TAG, "临时图片已删除: " + imagePath);
                        } else {
                            Log.e(TAG, "删除临时图片失败: " + imagePath);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(DetectionResultDialogActivity.this, "网络请求失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }



}

package com.example.rdds;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.example.rdds.detectionResult.DetectionResult;
import com.example.rdds.fragment.GroupFragment;
import com.example.rdds.fragment.HomeFragment;
import com.example.rdds.fragment.MineFragment;
import com.example.rdds.permission.PermissionManager;
import com.example.rdds.userInfo.LoginStateCache;
import com.example.rdds.userInfo.UserInfoCache;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import org.json.*;

public class MainActivity extends AppCompatActivity implements AMapLocationListener {

    private static final int REQUEST_CODE = 1;

    private HomeFragment homeFragment;
    private GroupFragment groupFragment;
    private MineFragment mineFragment;

    private BottomNavigationView bottomNavigationView;

    private PermissionManager permissionManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int CAMERA_PERMISSION_REQUEST = 1002;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String GetUserRecord_URL = GlobalData.getInstance().getHTTP_Address()+"/ip/get_user_results";

    private static final String GetUserBoundary_URL = GlobalData.getInstance().getHTTP_Address()+"/th/get_team_area";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private String currentPhotoPath;
    private String gpsLocation;

    // 声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    // 声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    private LoginStateCache loginStateCache;

    private UserInfoCache userInfoCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginStateCache = new LoginStateCache(this);

        userInfoCache = new UserInfoCache(MainActivity.this,loginStateCache.getUsername());

        // 权限管理器初始化
        permissionManager = PermissionManager.getInstance(this);

        // 检查定位权限
        if (!permissionManager.hasRequestedLocationPermission()) {
            requestLocationPermission();
        }

        // 地图隐式权限开启
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);

        // 初始化控件
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // 设置bottomNavigationView点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.home) {
                    selectedFragment(0);
                } else if (item.getItemId() == R.id.group) {
                    selectedFragment(2);
                } else if (item.getItemId() == R.id.user) {
                    selectedFragment(3);
                } else if (item.getItemId() == R.id.photograph) {
                    if (checkCameraPermission()) {
                        dispatchTakePictureIntent();
                        /*selectedFragment(0);
                        bottomNavigationView.setSelectedItemId(R.id.home);*/
                    } else {
                        requestCameraPermission();
                    }
                }
                return true;
            }
        });

        selectedFragment(0);

        //bottomNavigationView.setSelectedItemId(R.id.home);

        // 初始化定位
        initLocation();
    }

    // 主页跳转逻辑
    private void selectedFragment(int position) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        hideFragment(fragmentTransaction);
        if (position == 0) {
            if (homeFragment == null) {
                homeFragment = new HomeFragment();
                fragmentTransaction.add(R.id.content, homeFragment);
            } else {
                fragmentTransaction.show(homeFragment);
            }
            getUserRecord();
        } else if (position == 2) {
            if (groupFragment == null) {
                groupFragment = new GroupFragment();
                fragmentTransaction.add(R.id.content, groupFragment);
            } else {
                fragmentTransaction.show(groupFragment);
            }
        } else if (position == 3) {
            if (mineFragment == null) {
                mineFragment = new MineFragment();
                fragmentTransaction.add(R.id.content, mineFragment);
            } else {
                fragmentTransaction.show(mineFragment);
            }
        }

        // 提交
        fragmentTransaction.commit();
    }

    private void getUserBoundary() {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_name",loginStateCache.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(GetUserBoundary_URL)
                .post(body)
                .build();


        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 处理网络失败
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                                    homeFragment.clearBoundaryToMap();
                                    homeFragment.getBoundaryList().clear();
                                    String boundary=json.getString("data");
                                    Log.d("wky" , "onResponse: "+boundary);
                                    parseLatLngString(boundary);
                                    homeFragment.addBoundaryToMap();
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            default:
                                Toast.makeText(MainActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseLatLngString(String boundary) {
        String[] boundaryList = boundary.split("_");
        for (String latLngString : boundaryList)
        {
            List<LatLng> b=new ArrayList<>();
            // 按分号分割坐标点
            String[] points = latLngString.split(";");
            for (String point : points) {
                if (point == null || point.trim().isEmpty()) {
                    continue;
                }
                // 按逗号分割经纬度
                String[] latLng = point.split(",");
                if (latLng.length != 2) {
                    // 格式错误，跳过当前点
                    continue;
                }

                try {
                    double longitude = Double.parseDouble(latLng[0].trim());
                    double latitude = Double.parseDouble(latLng[1].trim());
                    b.add(new LatLng(latitude, longitude));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            homeFragment.getBoundaryList().add(b);
        }
    }
    // 检查经纬度范围是否合理（可选辅助方法）
    private boolean isValidLatLng(double longitude, double latitude) {
        return longitude >= -180 && longitude <= 180 &&
                latitude >= -90 && latitude <= 90;
    }

    // 隐藏其他界面
    private void hideFragment(FragmentTransaction fragmentTransaction) {
        if (homeFragment != null) {
            fragmentTransaction.hide(homeFragment);
        }
        if (mineFragment != null) {
            fragmentTransaction.hide(mineFragment);
        }
        if (groupFragment != null) {
            fragmentTransaction.hide(groupFragment);
        }
    }

    // 检查定位权限是否开启
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // 请求定位权限
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    private void initLocation() {
        try{
            // 初始化定位
            mLocationClient = new AMapLocationClient(getApplicationContext());
            // 设置定位回调监听
            mLocationClient.setLocationListener(this);
            // 初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            // 设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            // 设置是否只定位一次,默认为false
            mLocationOption.setOnceLocation(false);
            // 设置定位时间间隔
            mLocationOption.setInterval(5000);
            // 设置是否强制刷新WIFI，默认为强制刷新
            mLocationOption.setWifiActiveScan(true);
            // 设置是否允许模拟位置,默认为false，不允许模拟位置
            mLocationOption.setMockEnable(false);
            // 给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            // 启动定位
            if (checkLocationPermission()) {
                mLocationClient.startLocation();
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "定位初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 定位改变
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                // 定位成功回调信息，设置相关消息
                double latitude = aMapLocation.getLatitude();
                double longitude = aMapLocation.getLongitude();
                gpsLocation = latitude + "," + longitude;
                // Toast.makeText(this, "当前定位：" + gpsLocation, Toast.LENGTH_SHORT).show();
            } else {
                // 显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                //Toast.makeText(this, "定位失败，错误码: " + aMapLocation.getErrorCode() + ", 错误信息: " + aMapLocation.getErrorInfo(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // 检查相机权限是否开启
    private boolean checkCameraPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // 请求拍照权限
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    // 打开相机拍照
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.rdds.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // 保存照片
    private File createImageFile() throws IOException {
        // 创建图像
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );


        // 保存图像路径
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // 完成拍照后，触发上传与跳转页面的逻辑
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 上传图片
            Intent intent = new Intent(MainActivity.this, DetectionResultDialogActivity.class);
            String photoPath = currentPhotoPath;
            String gps =gpsLocation;
            intent.putExtra("PhotoPath", photoPath);
            intent.putExtra("gps", gps);
            startActivityForResult(intent, REQUEST_CODE);

            //uploadImageWithOkHttp(currentPhotoPath);

            // 返回主页
            //selectedFragment(0);
            //bottomNavigationView.setSelectedItemId(R.id.home);
        }
        else if (requestCode == REQUEST_CODE) {
            // Activity 已关闭，执行后续操作
            bottomNavigationView.setSelectedItemId(R.id.home);
        }
    }

    // 图像上传的方法



    // 请求权限之后进行标记
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 标记已请求过权限
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionManager.markLocationPermissionRequested();
                mLocationClient.startLocation();
            } else {
                Toast.makeText(this, "定位权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionManager.markCameraPermissionRequested();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 销毁activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
    }

    private void getUserRecord() {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id",loginStateCache.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败", Toast.LENGTH_SHORT).show();
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(GetUserRecord_URL)
                .post(body)
                .build();


        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 处理网络失败
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                                    JSONArray jsonArray=json.getJSONArray("data");
                                    homeFragment.getDefectPointList().clear();
                                    homeFragment.clearMarkerToMap();
                                    if (jsonArray != null) { // 增加非空验证
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            JSONObject jsonDefectPoint = jsonArray.getJSONObject(i);
                                            DefectPoint defectPoint=new DefectPoint(jsonDefectPoint.getString("result_id"),loginStateCache.getUsername(),jsonDefectPoint.getString("gps_location"),jsonDefectPoint.getString("detection_time"),jsonDefectPoint.getString("defect_type"),jsonDefectPoint.getString("severity"));
                                            homeFragment.getDefectPointList().add(defectPoint);
                                        }
                                    }
                                    homeFragment.addMarkersToMap();
                                    homeFragment.clearBoundaryToMap();
                                    homeFragment.getBoundaryList().clear();
                                    if(json.get("boundary")!=null){
                                        String boundary=json.getString("boundary");
                                        //Log.d("wxh666", "onResponse: "+boundary);
                                        parseLatLngString(boundary);
                                        homeFragment.addBoundaryToMap();
                                    }




                                    //Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            default:
                                Toast.makeText(MainActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
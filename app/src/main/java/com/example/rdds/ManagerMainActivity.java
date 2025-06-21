package com.example.rdds;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.example.rdds.fragment.GroupFragment;
import com.example.rdds.fragment.HomeFragment;
import com.example.rdds.fragment.ManagerGroupFragment;
import com.example.rdds.fragment.ManagerHomeFragment;
import com.example.rdds.fragment.MineFragment;
import com.example.rdds.permission.PermissionManager;
import com.example.rdds.userInfo.LoginStateCache;
import com.example.rdds.userInfo.UserInfoCache;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ManagerMainActivity extends AppCompatActivity {

    private ManagerHomeFragment managerHomeFragment;
    private ManagerGroupFragment managerGroupFragment;
    private MineFragment mineFragment;

    private BottomNavigationView bottomNavigationView;

    private PermissionManager permissionManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private static final String GetTeamRecord_URL = GlobalData.getInstance().getHTTP_Address()+"/ip/get_team_results";
    private static final String GetUserBoundary_URL = GlobalData.getInstance().getHTTP_Address()+"/th/get_team_area";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private LoginStateCache loginStateCache;

    private UserInfoCache userInfoCache;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_main);


        loginStateCache = new LoginStateCache(this);
        userInfoCache =new UserInfoCache(this,loginStateCache.getUsername());

        permissionManager = PermissionManager.getInstance(this);

        // 检查权限
        if (!permissionManager.hasRequestedLocationPermission()) {
            requestLocationPermission();
        }

        MapsInitializer.updatePrivacyShow(this, true,true);
        MapsInitializer.updatePrivacyAgree(this, true);

        // 初始化控件
        bottomNavigationView = findViewById(R.id.bottomNavigationViewManager);
        // 设置bottomNavigationView点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.home_manager){
                    selectedFragment(0);
                }else if(item.getItemId() == R.id.group_manager){
                    selectedFragment(1);
                }else if(item.getItemId() == R.id.user_manager){
                    selectedFragment(2);
                }
                return true;
            }
        });

        selectedFragment(0);
    }

    private void selectedFragment(int position){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        hideFragment(fragmentTransaction);
        if(position == 0){
            if(managerHomeFragment == null){
                managerHomeFragment = new ManagerHomeFragment();
                fragmentTransaction.add(R.id.contentManager,managerHomeFragment);
            }else{
                fragmentTransaction.show(managerHomeFragment);
            }
            getTeamRecord();
        }else if(position == 1){
            if(managerGroupFragment == null){
                managerGroupFragment = new ManagerGroupFragment();
                fragmentTransaction.add(R.id.contentManager,managerGroupFragment);
            }else{
                fragmentTransaction.show(managerGroupFragment);
            }
        }else if(position == 2){
            if(mineFragment == null){
                mineFragment = new MineFragment();
                fragmentTransaction.add(R.id.contentManager,mineFragment);
            }else{
                fragmentTransaction.show(mineFragment);
            }
        }

        // 提交
        fragmentTransaction.commit();
    }


    private void hideFragment(FragmentTransaction fragmentTransaction){
        if(managerHomeFragment != null){
            fragmentTransaction.hide(managerHomeFragment);
        }
        if(mineFragment != null){
            fragmentTransaction.hide(mineFragment);
        }
        if(managerGroupFragment != null){
            fragmentTransaction.hide(managerGroupFragment);
        }
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
                runOnUiThread(() -> Toast.makeText(ManagerMainActivity.this, "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                                    managerHomeFragment.clearBoundaryToMap();
                                    managerHomeFragment.getBoundaryList().clear();
                                    String boundary=json.getString("data");
                                    Log.d("wky" , "onResponse: "+boundary);
                                    parseLatLngString(boundary);
                                    managerHomeFragment.addBoundaryToMap();
                                    Toast.makeText(ManagerMainActivity.this, message, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(ManagerMainActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            default:
                                Toast.makeText(ManagerMainActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ManagerMainActivity.this,
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
            managerHomeFragment.getBoundaryList().add(b);
        }
    }

    // 检查经纬度范围是否合理（可选辅助方法）
    private boolean isValidLatLng(double longitude, double latitude) {
        return longitude >= -180 && longitude <= 180 &&
                latitude >= -90 && latitude <= 90;
    }


    private void getTeamRecord() {

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
                .url(GetTeamRecord_URL)
                .post(body)
                .build();


        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 处理网络失败
                runOnUiThread(() -> Toast.makeText(ManagerMainActivity.this, "网络连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                                    managerHomeFragment.getDefectPointList().clear();
                                    managerHomeFragment.clearMarkerToMap();
                                    if (jsonArray != null) { // 增加非空验证
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            JSONObject jsonDefectPoint = jsonArray.getJSONObject(i);
                                            DefectPoint defectPoint=new DefectPoint(jsonDefectPoint.getString("result_id"),jsonDefectPoint.getString("username"),jsonDefectPoint.getString("gps_location"),jsonDefectPoint.getString("detection_time"),jsonDefectPoint.getString("defect_type"),jsonDefectPoint.getString("severity"));
                                            managerHomeFragment.getDefectPointList().add(defectPoint);
                                        }
                                    }
                                    managerHomeFragment.addMarkersToMap();


                                    managerHomeFragment.clearBoundaryToMap();
                                    managerHomeFragment.getBoundaryList().clear();
                                    String boundary=json.getString("boundary");
                                    parseLatLngString(boundary);
                                    managerHomeFragment.addBoundaryToMap();



                                    //Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(ManagerMainActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            default:
                                //Toast.makeText(ManagerMainActivity.this, "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ManagerMainActivity.this,
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}
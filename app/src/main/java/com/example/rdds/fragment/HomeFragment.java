package com.example.rdds.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.ParcelUuid;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BaseOverlay;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.example.rdds.DefectDetailActivity;
import com.example.rdds.DefectPoint;
import com.example.rdds.GlobalData;
import com.example.rdds.R;
import com.example.rdds.permission.PermissionManager;
import com.example.rdds.userInfo.LoginStateCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment implements AMap.OnMarkerClickListener, AMap.OnInfoWindowClickListener, AMap.InfoWindowAdapter{

    private MapView mapView;
    private AMap aMap;
    private List<DefectPoint> defectPointList = new ArrayList<>();
    private List<Marker> MarkerList = new ArrayList<>();
    private Button btn_search_time;

    private PermissionManager permissionManager;
    // private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private List<Polygon> polygonList=new ArrayList<>();
    private List<List<LatLng>> boundaryList = new ArrayList<>();

    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;

    private LoginStateCache loginStateCache;
    private static final String GetUserRecordTime_URL = GlobalData.getInstance().getHTTP_Address()+"/ip/get_user_results_time";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        loginStateCache = new LoginStateCache(requireContext());
        // 加载Fragment布局
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        permissionManager = PermissionManager.getInstance(requireContext());

        // 获取地图控件
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState); // 必须调用

        // 初始化地图
        if (aMap == null) {
            aMap = mapView.getMap();
            initMap();
        }
        btn_search_time = view.findViewById(R.id.btn_search_time);
        setupClickListeners();

        // 添加标记点到地图
        //addMarkersToMap();

        // 设置标记点点击事件监听
        aMap.setOnMarkerClickListener(this);

        // 设置信息窗口点击事件监听
        aMap.setOnInfoWindowClickListener(this);

        // 设置自定义信息窗口适配器
        aMap.setInfoWindowAdapter(this);



        return view;
    }

    private void setupClickListeners() {
        btn_search_time.setOnClickListener(v -> search_time());
    }

    private void initMap() {
        // 设置地图类型
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        if (permissionManager.hasLocationPermission(requireContext())) {
            enableMyLocation();
            aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
                @Override
                public void onMapLoaded() {
                    // 地图加载完成后，请求一次定位并移动到定位位置
                    startSingleLocationRequest();
                }
            });
        }
    }


    private void hideKeyboard(View view) {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void search_time() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_find_by_time, null);

        EditText start = dialogView.findViewById(R.id.t_start);
        EditText end = dialogView.findViewById(R.id.t_end);
        Button btnConfirm = dialogView.findViewById(R.id.t_confirm);
        Button btnCancel = dialogView.findViewById(R.id.t_cancel);

        TextWatcher startPasswordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                    start.setError("日期格式如下'2025-6-6T10:00:00'");
                }
            }
        };
        start.addTextChangedListener(startPasswordWatcher);
        TextWatcher endPasswordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                    end.setError("日期格式如下'2025-6-6T10:00:00'");
                }
            }
        };
        end.addTextChangedListener(endPasswordWatcher);

        start.setHint("请输入起始时间"); // 明确提示输入名称
        end.setHint("请输入结束时间");
        AlertDialog dialog = builder.setView(dialogView).create();

        btnConfirm.setOnClickListener(v -> {
            String startTime = start.getText().toString().trim();
            String endTime = end.getText().toString().trim();

            start.clearFocus();
            end.clearFocus();

            hideKeyboard(start);
            hideKeyboard(end);

            getUserRecordByTime(startTime, endTime);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void getUserRecordByTime(String startTime, String endTime) {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id",loginStateCache.getUserId());
            jsonObject.put("start_time",startTime);
            jsonObject.put("end_time",endTime);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "JSON创建失败", Toast.LENGTH_SHORT).show();
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(GetUserRecordTime_URL)
                .post(body)
                .build();


        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "获取结果失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    int code = json.getInt("code");
                    String message = json.getString("message");

                    getActivity().runOnUiThread(() -> {
                        switch (code) {
                            case 200: // 登录成功
                                try {
                                    JSONArray jsonArray=json.getJSONArray("data");
                                    getDefectPointList().clear();
                                    clearMarkerToMap();
                                    if (jsonArray != null) { // 增加非空验证
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            JSONObject jsonDefectPoint = jsonArray.getJSONObject(i);
                                            DefectPoint defectPoint=new DefectPoint(jsonDefectPoint.getString("result_id"),loginStateCache.getUsername(),jsonDefectPoint.getString("gps_location"),jsonDefectPoint.getString("detection_time"),jsonDefectPoint.getString("defect_type"),jsonDefectPoint.getString("severity"));
                                            getDefectPointList().add(defectPoint);
                                        }
                                    }
                                    addMarkersToMap();
                                    //Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(requireContext(), "数据解析错误", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            default:
                                Toast.makeText(requireContext(), "未知错误: " + code, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                            "响应解析异常", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private void enableMyLocation() {
        try {
            // 启用地图定位图层
            aMap.setMyLocationEnabled(true);
            aMap.getUiSettings().setMyLocationButtonEnabled(true); // 显示定位按钮

            // 设置初始定位模式为跟随（仅首次定位时生效）
            MyLocationStyle myLocationStyle = new MyLocationStyle();
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            myLocationStyle.interval(1000); // 定位间隔（毫秒）
            aMap.setMyLocationStyle(myLocationStyle);

            // 设置地图移动动画
            aMap.moveCamera(CameraUpdateFactory.zoomTo(15)); // 设置默认缩放级别

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "地图定位初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    // 重写Fragment生命周期方法，转发到MapView
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    // 新增方法：请求单次定位并移动地图视角
    private void startSingleLocationRequest() {
        if (locationClient == null) {
            try {
                locationClient = new AMapLocationClient(requireContext());
                locationOption = new AMapLocationClientOption();

                // 设置定位模式为高精度模式
                locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                // 仅定位一次
                locationOption.setOnceLocation(true);

                locationClient.setLocationOption(locationOption);
                locationClient.setLocationListener(new AMapLocationListener() {
                    @Override
                    public void onLocationChanged(AMapLocation location) {
                        if (location != null && location.getErrorCode() == 0) {
                            // 定位成功，移动地图到定位位置
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                            // 关闭定位客户端，避免持续定位
                            if (locationClient != null) {
                                locationClient.stopLocation();
                                locationClient.onDestroy();
                                locationClient = null;
                            }
                        } else {
                            // 定位失败，显示错误信息
                            String errorMsg = "定位失败，错误码：" + location.getErrorCode() +
                                    "，错误信息：" + location.getErrorInfo();
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        // 启动定位
        locationClient.startLocation();
    }

    public List<DefectPoint> getDefectPointList(){
        return defectPointList;
    }
    public List<List<LatLng>> getBoundaryList(){return boundaryList;}

    public void clearMarkerToMap(){
        for(Marker marker: MarkerList){
            marker.destroy();
        }
    }
    public void addMarkersToMap() {
        for (DefectPoint point : defectPointList) {
            // 解析经纬度
            String[] latLngArray = point.getGpsLocation().split(",");
            double latitude = Double.parseDouble(latLngArray[0]);
            double longitude = Double.parseDouble(latLngArray[1]);

            // 创建经纬度对象
            LatLng latLng = new LatLng(latitude, longitude);

            // 创建标记点选项
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(point.getDefectType())
                    .snippet("点击查看详细信息")
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.logo3)));

            // 将标记点添加到地图
            Marker marker = aMap.addMarker(markerOptions);
            MarkerList.add(marker);
            // 可以将数据点信息存储在Marker的对象中，以便后续使用
            marker.setObject(point);
        }
    }

    public void clearBoundaryToMap(){
        for(Polygon polygon :polygonList){
            if (polygon!=null)
                polygon.remove();
        }
    }
    public void addBoundaryToMap() {
        for(List<LatLng> boundary :boundaryList)
        {
            Log.d("wky666", "addBoundaryToMap: 1");
            Polygon polygon =aMap.addPolygon(new PolygonOptions()
                    .addAll(boundary).strokeColor(Color.RED)           // 边界颜色为红色
                    .strokeWidth(10)                   // 边界宽度为4px
                    .fillColor(Color.argb(51, 38, 156, 253))  // 蓝色填充，透明度20%（51/255 ≈ 20%）
                    .zIndex(1));
            polygonList.add(polygon);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // 切换信息窗口的显示/隐藏状态
        if (marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
        } else {
            marker.showInfoWindow();
        }
        // 返回true表示事件已处理，不会触发默认行为
        return true;
    }

    /**
     * 信息窗口点击事件处理
     */
    @Override
    public void onInfoWindowClick(Marker marker) {
        // 获取标记点关联的数据
        DefectPoint defectPoint = (DefectPoint) marker.getObject();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("resultId",defectPoint.getResultId());
            jsonObject.put("username", defectPoint.getUsername());
            jsonObject.put("gpsLocation", defectPoint.getGpsLocation());
            jsonObject.put("detectionTime", defectPoint.getDetectionTime());
            jsonObject.put("defectType", defectPoint.getDefectType());
            jsonObject.put("severity", defectPoint.getSeverity());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        if (defectPoint != null) {
            // 创建意图，跳转到详情页
            Intent intent = new Intent(requireActivity(), DefectDetailActivity.class);
            // 将数据点信息传递给详情页
            intent.putExtra("defectPoint", jsonObject.toString());
            startActivity(intent);
        }
    }

    /**
     * 自定义信息窗口内容
     */
    @Override
    public View getInfoWindow(Marker marker) {
        View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);

        // 获取自定义布局中的控件
        TextView titleTv = infoWindow.findViewById(R.id.title);
        TextView snippetTv = infoWindow.findViewById(R.id.snippet);

        // 设置信息窗口内容
        titleTv.setText(marker.getTitle());
        snippetTv.setText(marker.getSnippet());

        return infoWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // 如果实现了getInfoWindow()方法，此方法可以返回null
        return null;
    }
}
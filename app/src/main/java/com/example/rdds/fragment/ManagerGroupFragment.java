package com.example.rdds.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import okhttp3.Callback;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Polygon;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.district.DistrictItem;
import com.amap.api.services.district.DistrictResult;
import com.amap.api.services.district.DistrictSearch;
import com.amap.api.services.district.DistrictSearchQuery;
import com.example.rdds.GlobalData;
import com.example.rdds.LoginActivity;
import com.example.rdds.MainActivity;
import com.example.rdds.ManagerMainActivity;
import com.example.rdds.R;
import com.example.rdds.userInfo.LoginStateCache;
import com.example.rdds.userInfo.UserInfoCache;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ManagerGroupFragment extends Fragment {
    private static final String TAG = "ManagerGroupFragment";
    private Button createGroupBtn, refreshBtn, dissolveBtn, areaDivisionBtn;
    private TextView teamName;
    private RecyclerView rvMembers;
    private LinearLayout llMemberArea;
    private MemberAdapter adapter;
    private List<Member> memberList = new ArrayList<>();
    private List<String> need = new ArrayList<>();
    private String AdCode = null;
    private boolean isGroupCreated = false; // 记录是否已创建小组
    private String currentGroupIdentifier = "";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String CREATE_GROUP_URL = GlobalData.getInstance().getHTTP_Address()+"/th/create_team";
    private static final String GET_MEMBERS_URL = GlobalData.getInstance().getHTTP_Address()+"/th/show_team_info";
    private static final String DISBAND_GROUP_URL = GlobalData.getInstance().getHTTP_Address()+"/th/disband_team";
    private static final String GET_TEAM_NAME_URL = GlobalData.getInstance().getHTTP_Address()+"/th/get_team_name";
    private static final String UPLOAD_AREA_URL = GlobalData.getInstance().getHTTP_Address()+"/th/upload_team_area";
    private LoginStateCache loginStateCache;
    private UserInfoCache userInfoCache;
    private static final double GRID_SIZE_THRESHOLD = 0.001; // 约100米
    private String province;
    private String city;
    private String distinct;


    public ManagerGroupFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_group, container, false);
        loginStateCache = new LoginStateCache(requireContext());
        userInfoCache = new UserInfoCache(requireContext(), loginStateCache.getUsername());
        isGroupCreated = true;
        refreshMembers();
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        teamName=view.findViewById(R.id.team_name);
        createGroupBtn = view.findViewById(R.id.btn_join_group);
        refreshBtn = view.findViewById(R.id.btn_refresh);
        dissolveBtn = view.findViewById(R.id.btn_exit_group);
        areaDivisionBtn = view.findViewById(R.id.btn_area_division);

        rvMembers = view.findViewById(R.id.rv_members);
        llMemberArea = view.findViewById(R.id.ll_member_area);

        rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MemberAdapter(memberList);
        rvMembers.setAdapter(adapter);

        createGroupBtn.setText("创建小组");
        dissolveBtn.setText("解散小组");
    }

    private void setupClickListeners() {
        createGroupBtn.setOnClickListener(v -> showCreateGroupDialog());
        refreshBtn.setOnClickListener(v -> {
            refreshMembers();
        });
        dissolveBtn.setOnClickListener(v -> showDissolveConfirmDialog());
        areaDivisionBtn.setOnClickListener(v -> showAreaDivisionDialog());
    }

    private void refreshTeam() {
        OkHttpClient client = new OkHttpClient();

        Request request;
        JSONObject json = new JSONObject();

        try {
            json.put("username", loginStateCache.getUsername());
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        request = new Request.Builder()
                .url(GET_TEAM_NAME_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try(ResponseBody body = response.body()){
                    String responseBody = body.string();
                    Log.d(TAG, "获取小组响应: " + responseBody);
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    int code = jsonResponse.getInt("code");
                    Log.d(TAG, "code: " + code);
                    switch (code) {
                        case 200:
                            isGroupCreated = true;
                            currentGroupIdentifier = jsonResponse.getString("data");
                            break;
                        case 201:
                            isGroupCreated = false;
                            currentGroupIdentifier = "";
                            break;
                        case 400:
                        case 500:
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    showToast("获取小组失败: " + jsonResponse.getString("message"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                llMemberArea.setVisibility(View.GONE);
                            });
                            break;
                    }
                }
                catch (JSONException e) {
                    Log.e(TAG, "小组名数据解析失败: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        showToast("小组名数据解析失败");
                        llMemberArea.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "获取小组名失败: " + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    showToast("获取小组名失败");
                    llMemberArea.setVisibility(View.GONE);
                });
            }
        });
    }

    private void refreshMembers() {
        OkHttpClient client = new OkHttpClient();

        Request request;
        JSONObject json = new JSONObject();

        try {
            json.put("user_id", loginStateCache.getUserId()); // 与加入时的参数一致
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "JSON创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        request = new Request.Builder()
                .url(GET_MEMBERS_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try(ResponseBody body = response.body()){
                    String responseBody = body.string();
                    Log.d(TAG, "成员响应: " + responseBody);
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    int code = jsonResponse.getInt("code");
                    Log.d(TAG, "code: " + code);
                    switch (code) {
                        case 200:
                            JSONArray dataArray = jsonResponse.getJSONArray("data");
                            memberList.clear();
                            currentGroupIdentifier=jsonResponse.getString("team_name");
                            if (dataArray != null && dataArray.length() > 0) { // 增加非空验证
                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject memberJson = dataArray.getJSONObject(i);
                                    String user_name,name,gender,user_type,area_name;
                                    if(memberJson.get("username") == null){
                                        user_name = "";
                                    }else{
                                        user_name = memberJson.getString("username");
                                    }
                                    if(memberJson.get("name") == null){
                                        name = "";
                                    }else{
                                        name = memberJson.getString("name");
                                    }
                                    if(memberJson.get("gender") == null){
                                        gender = "";
                                    }else{
                                        gender = memberJson.getString("gender");
                                    }
                                    if(memberJson.get("user_type") == null){
                                        user_type = "";
                                    }else{
                                        user_type = memberJson.getString("user_type");
                                        Log.d(TAG, "user_type: " + user_type);
                                        if (user_type.equals("0"))
                                            user_type="工作人员";
                                        else
                                            user_type="管理员";
                                    }
                                    if(memberJson.get("area_name") == null){
                                        area_name = "";
                                    }else{
                                        area_name = memberJson.getString("area_name");
                                    }
                                    Log.d(TAG, "user_type: " + user_type);
                                    memberList.add(new Member(user_name, name, gender, user_type, area_name));
                                    requireActivity().runOnUiThread(() -> {
                                        if (adapter != null) {
                                            adapter.notifyDataSetChanged();
                                        }
                                        dissolveBtn.setVisibility(View.VISIBLE);
                                        areaDivisionBtn.setVisibility(View.VISIBLE);
                                        createGroupBtn.setVisibility(View.GONE);
                                        teamName.setText("组名："+currentGroupIdentifier);
                                    });
                                }
                            } else {
                                showToast("该小组暂无成员");
                            }

                            // 主线程更新 UI
                            requireActivity().runOnUiThread(() -> {
                                llMemberArea.setVisibility(memberList.isEmpty() ? View.GONE : View.VISIBLE);
                            });
                            break;
                        case 404:
                            isGroupCreated = false;
                            currentGroupIdentifier = "";
                            //showToast("请先创建小组，如果已经创建，请稍后刷新");
                            memberList.clear(); // 清空本地成员列表

                            // 主线程更新 UI（使用 requireActivity() 确保非空）
                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged(); // 通知适配器数据变更
                                teamName.setText(currentGroupIdentifier);
                                dissolveBtn.setVisibility(View.GONE);
                                areaDivisionBtn.setVisibility(View.GONE);
                                llMemberArea.setVisibility(View.GONE);
                                createGroupBtn.setVisibility(View.VISIBLE);
                            });
                            break;
                        case 400:
                        case 500:
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    showToast("获取成员失败: " + jsonResponse.getString("message"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                llMemberArea.setVisibility(View.GONE);
                            });
                            break;
                    }
                }
                catch (JSONException e) {
                    Log.e(TAG, "成员解析失败: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        showToast("成员数据解析失败");
                        llMemberArea.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "刷新失败: " + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    showToast("刷新失败");
                    llMemberArea.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_group, null);

        EditText etGroupInput = dialogView.findViewById(R.id.et_group_id);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        etGroupInput.setHint("请输入小组名称"); // 明确提示输入名称
        AlertDialog dialog = builder.setView(dialogView).create();

        btnConfirm.setOnClickListener(v -> {
            String groupName = etGroupInput.getText().toString().trim();
            if (groupName.isEmpty()) {
                etGroupInput.setError("请输入小组名称");
                return;
            }

            etGroupInput.clearFocus();
            hideKeyboard(etGroupInput);
            createGroup(groupName); // 传递小组名称作为标识
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void createGroup(String groupName) {
        JSONObject json = new JSONObject();
        OkHttpClient client = new OkHttpClient();
        String username = loginStateCache.getUsername();
        try {
            json.put("team_name", groupName);
            json.put("username", username);
        } catch (JSONException e) {
            showToast("参数错误");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(CREATE_GROUP_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.getInt("code") == 200) {
                        isGroupCreated = true;
                        currentGroupIdentifier = groupName; // 保存小组名称作为标识
                        showToast("创建小组成功");
                        requireActivity().runOnUiThread(() -> {
                            //refreshTeam();
                            refreshMembers(); // 立即刷新成员列表
                        });
                    } else {
                        showToast("创建失败: " + jsonResponse.getString("code") + jsonResponse.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "创建响应解析失败: ", e);
                    showToast("响应解析失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "创建请求失败: " + e.getMessage());
                showToast("网络请求失败");
            }
        });
    }

    private void showDissolveConfirmDialog() {
        JSONObject json = new JSONObject();
        String username = loginStateCache.getUsername(); // 获取当前用户名
        OkHttpClient client = new OkHttpClient();
        try {
            // 统一使用与加入小组一致的参数名 "team_name"
            json.put("team_name", currentGroupIdentifier);
            Log.d(TAG, currentGroupIdentifier);
            json.put("username", username); // 添加用户名参数，标识退出的用户
        } catch (JSONException e) {
            showToast("参数错误");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(DISBAND_GROUP_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "解散响应: " + responseBody);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        int code = jsonResponse.getInt("code");
                        String message = jsonResponse.optString("message", ""); // 安全获取消息

                        if (code == 200) {
                            isGroupCreated = false;
                            currentGroupIdentifier = "";
                            memberList.clear(); // 清空本地成员列表

                            // 主线程更新 UI（使用 requireActivity() 确保非空）
                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged(); // 通知适配器数据变更
                                dissolveBtn.setVisibility(View.GONE);
                                areaDivisionBtn.setVisibility(View.GONE);
                                llMemberArea.setVisibility(View.GONE);
                                createGroupBtn.setVisibility(View.VISIBLE);
                                teamName.setText(currentGroupIdentifier);
                                showToast("解散小组成功");
                            });
                        } else {
                            // 在主线程显示服务器返回的错误信息
                            requireActivity().runOnUiThread(() ->
                                    showToast("解散失败: " + message)
                            );
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解散响应解析失败: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                showToast("解散响应解析失败")
                        );
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "退出请求失败: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        showToast("退出请求失败，请检查网络")
                );
            }
        });
    }

    private void showAreaDivisionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_area_divide, null);

        EditText etProvinceInput = dialogView.findViewById(R.id.et_province);
        EditText etCityInput = dialogView.findViewById(R.id.et_city);
        EditText etDistinctInput = dialogView.findViewById(R.id.et_district);
        Button btnConfirm = dialogView.findViewById(R.id.btn_area_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_area_cancel);

        etProvinceInput.setHint("请输入省份"); // 明确提示输入名称
        etCityInput.setHint("请输入城市");
        etDistinctInput.setHint("请输入区县");
        AlertDialog dialog = builder.setView(dialogView).create();

        btnConfirm.setOnClickListener(v -> {
            province = etProvinceInput.getText().toString().trim();
            city = etCityInput.getText().toString().trim();
            distinct = etDistinctInput.getText().toString().trim();

            etProvinceInput.clearFocus();
            etCityInput.clearFocus();
            etDistinctInput.clearFocus();

            hideKeyboard(etProvinceInput);
            hideKeyboard(etCityInput);
            hideKeyboard(etDistinctInput);

            searchDistrict(province, city, distinct); // 传递小组名称作为标识
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // 行政区查询
    private void searchDistrict(String province, String city, String district) {
        // 检查输入参数
        if (isEmpty(province) && isEmpty(city) && isEmpty(district)) {
            showToast("至少需要提供一个有效的行政区划名称");
            Log.e(TAG, "至少需要提供一个有效的行政区划名称");
            return;
        }

        if (!isEmpty(province) && isEmpty(city) && !isEmpty(district)){
            showToast("输入不能出现只输入省和区县");
            Log.e(TAG, "输入不能出现只输入省和区县");
            return;
        }

        // 确定搜索层级和关键词
        need.clear();

        if (province != null && !province.isEmpty()) {
            Log.d(TAG, "开始搜索省份: " + province);
            need.add(province);
        }
        if (city != null && !city.isEmpty()) {
            Log.d(TAG, "开始搜索城市: " + city);
            need.add(city);
        }
        if (district != null && !district.isEmpty()) {
            Log.d(TAG, "开始搜索区县: " + district);
            need.add(district);
        }

        try{
            performSearch(0);
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    private void performSearch(int level) throws AMapException {
        DistrictSearchQuery query = new DistrictSearchQuery();
        query.setKeywords(need.get(level));
        query.setShowBoundary(true);

        DistrictSearch search = new DistrictSearch(requireContext());
        search.setQuery(query);

        search.setOnDistrictSearchListener(new DistrictSearch.OnDistrictSearchListener() {
            @Override
            public void onDistrictSearched(DistrictResult result) {
                if (!validateSearchResult(result, need.get(level))) {
                    return;
                }

                // 找到唯一匹配的行政区
                DistrictItem nowMatchedItem = findMatchedItem(result.getDistrict(), need.get(level));
                if (nowMatchedItem == null) {
                    showToast("未找到匹配的行政区划: " + need.get(level));
                    return;
                }

                // 如果还有下一级需要搜索，继续递归
                if (level < need.size() - 1) {
                    Log.d(TAG, "层级" + need.get(level) + "搜索完成，继续搜索层级");
                    DistrictItem nextMatchedItem = findMatchedItem(nowMatchedItem.getSubDistrict(), need.get(level + 1));
                    try {
                        postHandle(nextMatchedItem.getAdcode(), level + 1);
                    } catch (AMapException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // 所有层级搜索完成，处理最终结果
                    handleFinalResult(nowMatchedItem);
                }
            }
        });

        search.searchDistrictAsyn();
    }

    private void postHandle(String AdCode, int level) throws AMapException {
        DistrictSearchQuery query = new DistrictSearchQuery();
        query.setKeywords(AdCode);
        query.setShowBoundary(true);

        DistrictSearch search = new DistrictSearch(requireContext());
        search.setQuery(query);

        search.setOnDistrictSearchListener(new DistrictSearch.OnDistrictSearchListener() {
            @Override
            public void onDistrictSearched(DistrictResult result) {
                if (!validateSearchResult(result, need.get(level))) {
                    return;
                }

                // 找到唯一匹配的行政区
                DistrictItem nowMatchedItem = findMatchedItem(result.getDistrict(), need.get(level));
                if (nowMatchedItem == null) {
                    showToast("未找到匹配的行政区划: " + need.get(level));
                    return;
                }

                // 如果还有下一级需要搜索，继续递归
                if (level < need.size() - 1) {
                    Log.d(TAG, "层级" + need.get(level) + "搜索完成，继续搜索层级");
                    DistrictItem nextMatchedItem = findMatchedItem(nowMatchedItem.getSubDistrict(), need.get(level + 1));
                    try {
                        postHandle(nextMatchedItem.getAdcode(), level + 1);
                    } catch (AMapException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // 所有层级搜索完成，处理最终结果
                    handleFinalResult(nowMatchedItem);
                }
            }
        });

        search.searchDistrictAsyn();
    }

    // 验证搜索结果是否有效
    private boolean validateSearchResult(DistrictResult result, String searchKeyword) {
        if (result == null) {
            String errorMsg = result != null
                    ? "搜索失败: " + result.getAMapException().getErrorMessage()
                    : "搜索结果为空";
            Log.e(TAG, errorMsg);
            showToast(errorMsg);
            return false;
        }

        if (result.getAMapException().getErrorCode() != 1000) {
            String errorMsg = "搜索失败，错误码: " + result.getAMapException().getErrorCode();
            Log.e(TAG, errorMsg);
            showToast(errorMsg);
            return false;
        }

        List<DistrictItem> districtItems = result.getDistrict();
        if (districtItems == null || districtItems.isEmpty()) {
            String errorMsg = "未找到匹配的行政区划: " + searchKeyword;
            Log.e(TAG, errorMsg);
            showToast(errorMsg);
            return false;
        }

        return true;
    }

    // 查找匹配的行政区项
    private DistrictItem findMatchedItem(List<DistrictItem> items, String keyword) {
        DistrictItem exactMatch = null;
        int exactMatchCount = 0;

        // 先查找完全匹配的项
        for (DistrictItem item : items) {
            if (keyword.equals(item.getName())) {
                exactMatch = item;
                exactMatchCount++;
            }
        }

        // 如果有多个完全匹配项，提示用户
        if (exactMatchCount > 1) {
            showToast("找到多个匹配项");
            Log.w(TAG, "找到多个匹配项");
            return null;
        }

        // 如果有完全匹配项，返回第一个
        if (exactMatch != null) {
            return exactMatch;
        }

        // 如果没有完全匹配，返回第一个结果
        if (!items.isEmpty()) {
            showToast("未找到匹配结果");
            Log.w(TAG, "未找到匹配结果");
        }

        return null;
    }

    // 检查所有参数是否为空
    private boolean isEmpty(String params) {
        return params == null || params.isEmpty();
    }

    // 辅助方法：处理最终搜索结果
    private void handleFinalResult(DistrictItem item) {
        String[] boundaryStr = item.districtBoundary();
        if (boundaryStr == null || boundaryStr.length == 0) {
            Log.e(TAG, "未获取到有效的边界数据");
            showToast("未获取到有效的边界数据");
            return;
        }
        String boundary_to_server=convertBoundaryToString(boundaryStr);
        //List<List<LatLng>> boundary = parseBoundary(boundaryStr);
        //String boundary_to_server=convertBoundaryToString(boundary);
        Log.d(TAG, "handleFinalResult: "+boundary_to_server);
        if (boundary_to_server.isEmpty()) {
            Log.e(TAG, "解析边界数据失败");
            showToast("解析边界数据失败");
            return;
        }

        // 为划分的区域编号，并上传
        uploadDividedAreas(item.getName(), province, city,boundary_to_server);
    }


    private String convertBoundaryToString(String[] boundaryStr) {
        if (boundaryStr == null || boundaryStr.length == 0) {
            return ""; // 处理空数组的情况
        }

        StringBuilder result = new StringBuilder(boundaryStr[0]);
        for (int i = 1; i < boundaryStr.length; i++) {
            result.append("_").append(boundaryStr[i]);
        }
        return result.toString();
    }
    private void uploadDividedAreas(String areaName, String province, String city,String boundary){
        Log.d(TAG, "开始上传设置好的区域");
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();
        String area = province + city + areaName;
        try{
            jsonObject.put("area_name", area);
            jsonObject.put("user_id", loginStateCache.getUserId());
            jsonObject.put("team_boundary", boundary);

        }catch (Exception e){
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(UPLOAD_AREA_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "上传区域响应: " + responseBody);
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    int code = jsonResponse.getInt("code");
                    String message = jsonResponse.optString("message", ""); // 安全获取消息

                    if (code == 200) {
                        requireActivity().runOnUiThread(() -> {
                            refreshMembers();
                            Toast.makeText(requireContext(), "上传区域成功", Toast.LENGTH_SHORT).show();
                        });


                    } else {
                        // 在主线程显示服务器返回的错误信息
                        requireActivity().runOnUiThread(() ->
                                showToast("上传区域失败: " + code + message)
                        );
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "上传区域响应解析失败: " + e.getMessage());
                    requireActivity().runOnUiThread(() ->
                            showToast("上传区域响应解析失败")
                    );
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "上传区域请求失败: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        showToast("上传区域请求失败，请检查网络")
                );
            }
        });
    }

    // 解析边界字符串为LatLng列表
    private List<List<LatLng>> parseBoundary(String[] boundaryStr) {
        List<List<LatLng>> boundary = new ArrayList<>();
        for (String str : boundaryStr) {
            List<LatLng> b= new ArrayList<>();
            String[] latLngStr = str.split(";");
            for (String point : latLngStr) {
                String[] coords = point.split(",");
                if (coords.length == 2) {
                    b.add(new LatLng(Double.parseDouble(coords[1]),
                            Double.parseDouble(coords[0])));
                }
            }
            boundary.add(b);
        }
        return boundary;
    }

    private void hideKeyboard(View view) {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    // 成员数据类
    private static class Member {
        String username, name,gender, user_type, area_name;
        public Member(String username, String name, String gender, String user_type, String area_name) {
            this.username = username;
            this.name = name;
            this.gender = gender;
            this.user_type = user_type;
            this.area_name = area_name;
        }
    }

    // RecyclerView 适配器
    private static class MemberAdapter extends RecyclerView.Adapter<ManagerGroupFragment.MemberAdapter.ViewHolder> {
        private final List<ManagerGroupFragment.Member> members;
        public MemberAdapter(List<ManagerGroupFragment.Member> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public ManagerGroupFragment.MemberAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.groupmember_item, parent, false);
            return new ManagerGroupFragment.MemberAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ManagerGroupFragment.MemberAdapter.ViewHolder holder, int position) {
            ManagerGroupFragment.Member member = members.get(position);
            holder.userName.setText(member.username);
            holder.Name.setText(member.name);
            holder.Gender.setText(member.gender);
            holder.usertype.setText(member.user_type);
            holder.areaName.setText(member.area_name);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView userName, Name,Gender,  usertype, areaName;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.tv_username);
                Name = itemView.findViewById(R.id.tv_name);
                Gender = itemView.findViewById(R.id.tv_gender);
                usertype = itemView.findViewById(R.id.tv_usertype);
                areaName = itemView.findViewById(R.id.tv_area);
            }
        }
    }
}

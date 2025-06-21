package com.example.rdds.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rdds.GlobalData;
import com.example.rdds.R;
import com.example.rdds.userInfo.LoginStateCache;
import com.example.rdds.userInfo.UserInfoCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GroupFragment extends Fragment {
    private static final String TAG = "GroupFragment";
    private Button btnJoinGroup, btnRefresh, btnExitGroup;
    private TextView teamName;
    private RecyclerView rvMembers;
    private LinearLayout llMemberArea;
    private MemberAdapter adapter;
    private List<Member> memberList = new ArrayList<>();
    private boolean isJoined = false;

    private String currentGroupIdentifier = "";
    private OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String JOIN_GROUP_URL = GlobalData.getInstance().getHTTP_Address() + "/th/join_team";
    private static final String GET_MEMBERS_URL = GlobalData.getInstance().getHTTP_Address() + "/th/show_team_info";
    private static final String EXIT_GROUP_URL = GlobalData.getInstance().getHTTP_Address() + "/th/esc_team";
    private LoginStateCache loginStateCache;
    private UserInfoCache userInfoCache;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);
        loginStateCache = new LoginStateCache(requireContext());
        userInfoCache = new UserInfoCache(requireContext(), loginStateCache.getUsername());
        isJoined= true;
        refreshMembers();
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        teamName=view.findViewById(R.id.team_name);
        btnJoinGroup = view.findViewById(R.id.btn_join_group);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnExitGroup = view.findViewById(R.id.btn_exit_group);
        rvMembers = view.findViewById(R.id.rv_members);
        llMemberArea = view.findViewById(R.id.ll_member_area);

        // 初始化 RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MemberAdapter(memberList);
        rvMembers.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnJoinGroup.setOnClickListener(v -> showJoinGroupDialog());
        btnRefresh.setOnClickListener(v -> refreshMembers());
        btnExitGroup.setOnClickListener(v -> exitGroup());
    }

    private void showJoinGroupDialog() {
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
            joinGroup(groupName); // 传递小组名称作为标识
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void joinGroup(String groupName) {
        JSONObject json = new JSONObject();
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
                .url(JOIN_GROUP_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "加入响应: " + responseBody);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.getInt("code") == 200) {
                            isJoined = true;
                            currentGroupIdentifier = groupName; // 保存小组名称作为标识
                            showToast("加入小组成功");
                            requireActivity().runOnUiThread(() -> {
                                refreshMembers(); // 立即刷新成员列表
                            });
                        } else {
                            showToast("加入失败: " + jsonResponse.getString("code") + jsonResponse.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "加入响应解析失败: ", e);
                        showToast("响应解析失败");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "加入请求失败: " + e.getMessage());
                showToast("网络请求失败");
            }
        });
    }

    private void refreshMembers() {
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
                    Log.d(TAG, code + jsonResponse.getString("message"));
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
                                    memberList.add(new Member(user_name, name, gender, user_type, area_name));
                                    requireActivity().runOnUiThread(() -> {
                                        if (adapter != null) {
                                            adapter.notifyDataSetChanged();
                                        }
                                        btnExitGroup.setVisibility(View.VISIBLE);
                                        btnJoinGroup.setVisibility(View.GONE);
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
                            isJoined = false;
                            currentGroupIdentifier = "";
                            showToast("请先加入小组，如果已经加入，请稍后刷新");
                            memberList.clear(); // 清空本地成员列表

                            // 主线程更新 UI（使用 requireActivity() 确保非空）
                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged(); // 通知适配器数据变更
                                btnExitGroup.setVisibility(View.GONE);
                                llMemberArea.setVisibility(View.GONE);
                                btnJoinGroup.setVisibility(View.VISIBLE);
                                teamName.setText(currentGroupIdentifier);
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

    private void exitGroup() {
        JSONObject json = new JSONObject();
        String username = loginStateCache.getUsername(); // 获取当前用户名
        try {
            // 统一使用与加入小组一致的参数名 "team_name"
            json.put("team_name", currentGroupIdentifier);
            json.put("username", username); // 添加用户名参数，标识退出的用户
        } catch (JSONException e) {
            showToast("参数错误");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(EXIT_GROUP_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "退出响应: " + responseBody);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        int code = jsonResponse.getInt("code");
                        String message = jsonResponse.optString("message", ""); // 安全获取消息

                        if (code == 200) {
                            isJoined = false;
                            currentGroupIdentifier = "";
                            memberList.clear(); // 清空本地成员列表

                            // 主线程更新 UI（使用 requireActivity() 确保非空）
                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged(); // 通知适配器数据变更
                                btnExitGroup.setVisibility(View.GONE);
                                llMemberArea.setVisibility(View.GONE);
                                btnJoinGroup.setVisibility(View.VISIBLE);
                                teamName.setText(currentGroupIdentifier);
                                showToast("退出小组成功");
                            });
                        } else {
                            // 在主线程显示服务器返回的错误信息
                            requireActivity().runOnUiThread(() ->
                                    showToast("退出失败: " + message)
                            );
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "退出响应解析失败: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                showToast("退出响应解析失败")
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
    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    private void hideKeyboard(View view) {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
    private static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
        private final List<Member> members;
        public MemberAdapter(List<Member> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.groupmember_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Member member = members.get(position);
            holder.userName.setText(member.username);
            holder.Name.setText(member.name);
            holder.Gender.setText(member.gender);
            holder.usertype.setText(member.user_type);
            holder.Area_name.setText(member.area_name);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView userName, Name,Gender,  usertype, Area_name;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.tv_username);
                Name = itemView.findViewById(R.id.tv_name);
                Gender = itemView.findViewById(R.id.tv_gender);
                usertype = itemView.findViewById(R.id.tv_usertype);
                Area_name = itemView.findViewById(R.id.tv_area);
            }
        }
    }
}
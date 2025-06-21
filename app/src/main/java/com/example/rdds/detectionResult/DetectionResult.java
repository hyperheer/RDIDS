package com.example.rdds.detectionResult;

import org.json.JSONArray;
import org.json.JSONObject;

public class DetectionResult {
    private static DetectionResult instance;
    private JSONArray dataArray;

    private DetectionResult() {
        dataArray = new JSONArray();
    }

    public static DetectionResult getInstance() {
        if (instance == null) {
            synchronized (DetectionResult.class) {
                if (instance == null) {
                    instance = new DetectionResult();
                }
            }
        }
        return instance;
    }

    // 添加单条数据
    public void addData(JSONObject dataObject) {
        dataArray.put(dataObject);
    }

    // 清空所有数据
    public void clearAllData() {
        dataArray = new JSONArray();
    }

    // 获取数据条数
    public int size() {
        return dataArray.length();
    }

    // 获取整个 JSON 数组
    public JSONArray getDataArray() {
        return dataArray;
    }
}
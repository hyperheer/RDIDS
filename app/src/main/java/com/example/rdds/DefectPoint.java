package com.example.rdds;

import java.io.Serializable;

public class DefectPoint implements Serializable {
    private String resultId;
    private String username;
    private String gpsLocation;
    private String detectionTime;
    private String defectType;
    private String severity;

    // 构造方法
    public DefectPoint(String resultId,String username, String gpsLocation, String detectionTime, String defectType, String severity) {
        this.resultId=resultId;
        this.username = username;
        this.gpsLocation = gpsLocation;
        this.detectionTime = detectionTime;
        this.defectType = defectType;
        this.severity = severity;
    }

    // Getter和Setter方法
    public String getResultId() {
        return resultId;
    }
    public void setResultId(String resultId) {
        this.resultId = resultId;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGpsLocation() {
        return gpsLocation;
    }

    public void setGpsLocation(String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public String getDetectionTime() {
        return detectionTime;
    }

    public void setDetectionTime(String detectionTime) {
        this.detectionTime = detectionTime;
    }

    public String getDefectType() {
        return defectType;
    }

    public void setDefectType(String defectType) {
        this.defectType = defectType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}    
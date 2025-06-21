package com.example.rdds;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;

public class DetectionResultAdapter extends RecyclerView.Adapter<DetectionResultAdapter.DetectionResultViewHolder> {

    private JSONObject dataObject;

    public DetectionResultAdapter(JSONObject dataObject) {
        this.dataObject = dataObject;
    }

    @NonNull
    @Override
    public DetectionResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detection_result, parent, false);
        return new DetectionResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetectionResultViewHolder holder, int position) {
        try {
            holder.tvLocation.setText("拍摄位置:" + dataObject.getString("gps_location"));
            holder.tvDefectionTime.setText("拍摄时间:" + dataObject.getString("detection_time"));
            holder.tvDefectType.setText("检测类型:" + dataObject.getString("defect_type"));
            holder.tvSeverity.setText("严重程度:" + dataObject.getInt("severity"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static class DetectionResultViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocation, tvDefectType, tvDefectionTime, tvSeverity;

        public DetectionResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDefectType = itemView.findViewById(R.id.tvDefectType);
            tvDefectionTime = itemView.findViewById(R.id.tvDetectionTime);
            tvSeverity = itemView.findViewById(R.id.tvSeverity);
        }
    }
}

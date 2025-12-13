package com.example.bay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.R;
import com.example.bay.model.ForecastDay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.VH> {

    private final List<ForecastDay> items = new ArrayList<>();

    public void setItems(List<ForecastDay> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weather_forecast_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ForecastDay d = items.get(position);

        h.tvDay.setText(d.dayLabel != null ? d.dayLabel : "");
        h.tvTemp.setText(String.format(Locale.getDefault(), "%d° / %d°", d.maxTemp, d.minTemp));
        h.tvDesc.setText(d.description != null ? d.description : "");

        h.ivIcon.setImageResource(mapIconToDrawable(d.iconCode));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDay, tvTemp, tvDesc;
        ImageView ivIcon;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }

    private int mapIconToDrawable(String iconCode) {
        if (iconCode == null) return R.drawable.pcloudy;

        if (iconCode.startsWith("01")) return R.drawable.pcloudy;
        if (iconCode.startsWith("02")) return R.drawable.pcloudy;
        if (iconCode.startsWith("03")) return R.drawable.pcloudy;
        if (iconCode.startsWith("04")) return R.drawable.pcloudy;
        if (iconCode.startsWith("09")) return R.drawable.pcloudy;
        if (iconCode.startsWith("10")) return R.drawable.pcloudy;
        if (iconCode.startsWith("11")) return R.drawable.pcloudy;
        if (iconCode.startsWith("13")) return R.drawable.pcloudy;
        if (iconCode.startsWith("50")) return R.drawable.pcloudy;

        return R.drawable.pcloudy;
    }
}

package com.example.bay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.R;
import com.example.bay.model.ForecastDay;
import com.example.bay.util.WeatherIconMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying a list of weather forecast items.
 * The layout {@code item_weather_forecast_day.xml} must contain the following views:
 *   - {@code ivIcon}        : ImageView for the weather icon
 *   - {@code tvDay}         : TextView for the day label (Today, Mon, …)
 *   - {@code ivRainIcon}    : ImageView for rain icon (optional, visibility GONE when no rain)
 *   - {@code tvRainPercent} : TextView for rain probability (e.g., "75%")
 *   - {@code tvTempMin}     : TextView for minimum temperature
 *   - {@code tvTempMax}     : TextView for maximum temperature
 *   - {@code pbTempRange}   : ProgressBar representing the temperature range
 */
public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ViewHolder> {

    private final List<ForecastDay> items = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forecast_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastDay day = items.get(position);

        holder.tvDay.setText(day.getDayLabel());
        holder.ivIcon.setImageResource(WeatherIconMapper.getIconResource(day.getIconCode()));

        // Rain probability handling
        int rain = day.getRainPercent();
        if (rain > 0) {
            holder.ivRainIcon.setVisibility(View.VISIBLE);
            holder.tvRainPercent.setVisibility(View.VISIBLE);
            holder.tvRainPercent.setText(rain + "%");
            // Choose appropriate rain icon based on intensity (simple heuristic)
            if (rain >= 70) {
                holder.ivRainIcon.setImageResource(R.drawable.ic_rain_heavy);
            } else {
                holder.ivRainIcon.setImageResource(R.drawable.ic_rain_light);
            }
        } else {
            holder.ivRainIcon.setVisibility(View.GONE);
            holder.tvRainPercent.setVisibility(View.GONE);
        }

        // Temperature presentation
        holder.tvTempMin.setText(String.format("%d°", day.getMinTemp()));
        holder.tvTempMax.setText(String.format("%d°", day.getMaxTemp()));

        // Temperature range progress bar – we map the min/max of the currently displayed set
        // to a 0‑100 scale. For simplicity we use the min of the list as 0 and max of the list as 100.
        int overallMin = findOverallMin();
        int overallMax = findOverallMax();
        if (overallMax > overallMin) {
            int progress = (day.getMaxTemp() - overallMin) * 100 / (overallMax - overallMin);
            holder.pbTempRange.setProgress(progress);
        } else {
            holder.pbTempRange.setProgress(0);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Replace the current data set with a new list of forecast days.
     */
    public void setItems(@NonNull List<ForecastDay> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Helper to find the lowest temperature in the current list. */
    private int findOverallMin() {
        int min = Integer.MAX_VALUE;
        for (ForecastDay d : items) {
            if (d.getMinTemp() < min) {
                min = d.getMinTemp();
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /** Helper to find the highest temperature in the current list. */
    private int findOverallMax() {
        int max = Integer.MIN_VALUE;
        for (ForecastDay d : items) {
            if (d.getMaxTemp() > max) {
                max = d.getMaxTemp();
            }
        }
        return max == Integer.MIN_VALUE ? 0 : max;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvDay;
        ImageView ivRainIcon;
        TextView tvRainPercent;
        TextView tvTempMin;
        TextView tvTempMax;
        ProgressBar pbTempRange;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvDay = itemView.findViewById(R.id.tvDay);
            ivRainIcon = itemView.findViewById(R.id.ivRainIcon);
            tvRainPercent = itemView.findViewById(R.id.tvRainPercent);
            tvTempMin = itemView.findViewById(R.id.tvTempMin);
            tvTempMax = itemView.findViewById(R.id.tvTempMax);
            pbTempRange = itemView.findViewById(R.id.pbTempRange);
        }
    }
}

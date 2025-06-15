package com.josephklenk.androidproject;

import android.app.AlertDialog;
import android.widget.ImageButton;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {
    private Cursor cursor;
    private Context context;
    private DatabaseHelper dbHelper;

    public interface WeightUpdateListener {
        void onWeightUpdated();
    }

    private WeightUpdateListener updateListener;

    public void setUpdateListener(WeightUpdateListener listener) {
        this.updateListener = listener;
    }

    public WeightAdapter(Cursor cursor) {
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        this.dbHelper = new DatabaseHelper(context);
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weight, parent, false);
        return new WeightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        if (cursor != null && cursor.moveToPosition(position)) {
            try {
                int idIndex = cursor.getColumnIndex("id");
                int weightIndex = cursor.getColumnIndex("weight");
                int dateIndex = cursor.getColumnIndex("date");

                if (weightIndex != -1 && dateIndex != -1 && idIndex != -1) {
                    long id = cursor.getLong(idIndex);
                    double weight = cursor.getDouble(weightIndex);
                    String date = cursor.getString(dateIndex);
                    holder.weightText.setText(String.format("%.1f lbs", weight));
                    holder.dateText.setText(date);

                    holder.deleteButton.setOnClickListener(v -> {
                        new AlertDialog.Builder(context)
                                .setTitle("Delete Weight")
                                .setMessage("Are you sure you want to delete this weight entry?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    dbHelper.deleteWeight(id);
                                    if (updateListener != null) {
                                        updateListener.onWeightUpdated();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        cursor = newCursor;
        notifyDataSetChanged();
    }

    public Cursor getCursor() {
        return cursor;
    }

    public static class WeightViewHolder extends RecyclerView.ViewHolder {
        final TextView weightText;
        final TextView dateText;
        final ImageButton deleteButton;

        WeightViewHolder(View itemView) {
            super(itemView);
            weightText = itemView.findViewById(R.id.text_weight);
            dateText = itemView.findViewById(R.id.text_date);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
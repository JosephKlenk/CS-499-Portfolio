package com.josephklenk.androidproject;

import android.app.AlertDialog;
import android.widget.ImageButton;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {
    private static final String TAG = "WeightAdapter";
    private Cursor cursor;
    private Context context;
    private DatabaseHelper dbHelper;

    /**
     * Interface for weight update callbacks
     */
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
                    holder.dateText.setText(formatDate(date));

                    holder.deleteButton.setOnClickListener(v -> showDeleteConfirmation(id));
                } else {
                    Log.w(TAG, "Column indices not found properly");
                    showErrorInView(holder);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding weight data at position: " + position, e);
                showErrorInView(holder);
            }
        } else {
            Log.w(TAG, "Cursor is null or cannot move to position: " + position);
            showErrorInView(holder);
        }
    }

    /**
     * Shows delete confirmation dialog with enhanced feedback
     */
    private void showDeleteConfirmation(long weightId) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Weight Entry")
                    .setMessage("Are you sure you want to delete this weight entry? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> handleDelete(weightId))
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete confirmation", e);
            showToast("Failed to show delete confirmation");
        }
    }

    /**
     * Handles weight entry deletion with error handling
     */
    private void handleDelete(long weightId) {
        try {
            dbHelper.deleteWeight(weightId);
            showToast("Weight entry deleted");
            
            if (updateListener != null) {
                updateListener.onWeightUpdated();
            }
            
            Log.d(TAG, "Weight entry deleted successfully: " + weightId);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting weight entry: " + weightId, e);
            showToast("Failed to delete weight entry");
        }
    }

    /**
     * Formats date string for display
     */
    private String formatDate(String date) {
        try {
            // You can enhance this to format dates differently if needed
            return date;
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + date, e);
            return "Invalid date";
        }
    }

    /**
     * Shows error state in view holder
     */
    private void showErrorInView(WeightViewHolder holder) {
        holder.weightText.setText("Error");
        holder.dateText.setText("Failed to load");
        holder.deleteButton.setEnabled(false);
    }

    /**
     * Shows toast message safely
     */
    private void showToast(String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    /**
     * Safely swaps cursor with proper cleanup
     */
    public void swapCursor(Cursor newCursor) {
        try {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            cursor = newCursor;
            notifyDataSetChanged();
            Log.d(TAG, "Cursor swapped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error swapping cursor", e);
        }
    }

    /**
     * Gets current cursor
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Enhanced ViewHolder with error handling
     */
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
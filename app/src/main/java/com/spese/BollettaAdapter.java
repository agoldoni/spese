package com.spese;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spese.db.Bolletta;
import com.spese.db.PurchaseType;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BollettaAdapter extends RecyclerView.Adapter<BollettaAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Bolletta bolletta);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Bolletta bolletta, int position);
    }

    private final List<Bolletta> bollette = new ArrayList<>();
    private final String[] months;
    private final DecimalFormat euroFormat;
    private final Map<String, String> purchaseTypeNames = new HashMap<>();
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public BollettaAdapter(String[] months) {
        this.months = months;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ITALY);
        this.euroFormat = new DecimalFormat("#,##0.00", symbols);
    }

    public void setPurchaseTypes(List<PurchaseType> types) {
        purchaseTypeNames.clear();
        for (PurchaseType pt : types) {
            purchaseTypeNames.put(pt.getId(), pt.getName());
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setBollette(List<Bolletta> items) {
        bollette.clear();
        bollette.addAll(items);
        notifyDataSetChanged();
    }

    public Bolletta getItem(int position) {
        return bollette.get(position);
    }

    public void removeItem(int position) {
        bollette.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bolletta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bolletta b = bollette.get(position);

        String typeName = purchaseTypeNames.get(b.getPurchaseTypeId());
        holder.textType.setText(typeName != null ? typeName : "?");
        holder.textAmount.setText("€ " + euroFormat.format(b.getAmount()));

        String monthName = (b.getMonth() >= 1 && b.getMonth() <= 12)
                ? months[b.getMonth() - 1] : String.valueOf(b.getMonth());
        holder.textPeriod.setText(monthName + " " + b.getYear());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(b);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(b, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return bollette.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textType;
        final TextView textAmount;
        final TextView textPeriod;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textType = itemView.findViewById(R.id.text_tipo);
            textAmount = itemView.findViewById(R.id.text_importo);
            textPeriod = itemView.findViewById(R.id.text_periodo);
        }
    }
}

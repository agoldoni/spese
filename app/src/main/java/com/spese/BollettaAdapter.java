package com.spese;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spese.db.Bolletta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BollettaAdapter extends RecyclerView.Adapter<BollettaAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Bolletta bolletta);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Bolletta bolletta, int position);
    }

    private final List<Bolletta> bollette = new ArrayList<>();
    private final String[] mesi;
    private final DecimalFormat formatoEuro;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public BollettaAdapter(String[] mesi) {
        this.mesi = mesi;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ITALY);
        this.formatoEuro = new DecimalFormat("#,##0.00", symbols);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setBollette(List<Bolletta> nuovaBollette) {
        bollette.clear();
        bollette.addAll(nuovaBollette);
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

        holder.textTipo.setText(b.getTipo());
        holder.textImporto.setText("€ " + formatoEuro.format(b.getImporto()));

        String nomeMese = (b.getMese() >= 1 && b.getMese() <= 12)
                ? mesi[b.getMese() - 1] : String.valueOf(b.getMese());
        holder.textPeriodo.setText(nomeMese + " " + b.getAnno());

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
        final TextView textTipo;
        final TextView textImporto;
        final TextView textPeriodo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTipo = itemView.findViewById(R.id.text_tipo);
            textImporto = itemView.findViewById(R.id.text_importo);
            textPeriodo = itemView.findViewById(R.id.text_periodo);
        }
    }
}

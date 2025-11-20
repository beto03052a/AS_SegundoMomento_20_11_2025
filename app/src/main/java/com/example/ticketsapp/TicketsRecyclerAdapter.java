package com.example.ticketsapp;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TicketsRecyclerAdapter extends RecyclerView.Adapter<TicketsRecyclerAdapter.TicketViewHolder> {

    private List<Ticket> tickets;
    private OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
        void onTicketLongClick(Ticket ticket);
    }

    public TicketsRecyclerAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
        this.tickets = tickets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket t = tickets.get(position);
        holder.bind(t);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    class TicketViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSucursal;
        private TextView tvCategoriaPrioridad;
        private TextView tvEstadoFecha;
        private TextView tvDescripcion;
        private TextView tvTecnico;
        private TextView tvPrioridadBadge;
        private View viewEstadoIndicator;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSucursal = itemView.findViewById(R.id.tvSucursal);
            tvCategoriaPrioridad = itemView.findViewById(R.id.tvCategoriaPrioridad);
            tvEstadoFecha = itemView.findViewById(R.id.tvEstadoFecha);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvTecnico = itemView.findViewById(R.id.tvTecnico);
            tvPrioridadBadge = itemView.findViewById(R.id.tvPrioridadBadge);
            viewEstadoIndicator = itemView.findViewById(R.id.viewEstadoIndicator);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTicketClick(tickets.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTicketLongClick(tickets.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        public void bind(Ticket t) {
            // Sucursal
            tvSucursal.setText(t.getSucursal());

            // Formato mejorado para categoría
            String categoria = t.getCategoria();
            tvCategoriaPrioridad.setText(categoria);

            // Badge de prioridad con colores
            if (tvPrioridadBadge != null && t.getPrioridad() != null) {
                String prioridad = t.getPrioridad().toUpperCase();
                tvPrioridadBadge.setText(prioridad);

                String prioridadLower = t.getPrioridad().toLowerCase();
                if (prioridadLower.contains("alta")) {
                    tvPrioridadBadge.setBackgroundResource(R.drawable.bg_priority_high);
                } else if (prioridadLower.contains("media")) {
                    tvPrioridadBadge.setBackgroundResource(R.drawable.bg_priority_media);
                } else if (prioridadLower.contains("baja")) {
                    tvPrioridadBadge.setBackgroundResource(R.drawable.bg_priority_baja);
                }
            }

            // Estado con indicador de color
            String estado = t.getEstado();
            boolean esCerrado = false;
            if (estado != null) {
                String estadoLower = estado.toLowerCase();
                if (estadoLower.contains("pendiente")) {
                    viewEstadoIndicator.setBackgroundResource(R.drawable.bg_status_pendiente);
                } else if (estadoLower.contains("progreso") || estadoLower.contains("en progreso")) {
                    viewEstadoIndicator.setBackgroundResource(R.drawable.bg_status_progreso);
                } else if (estadoLower.contains("cerrado") || estadoLower.contains("resuelto")) {
                    viewEstadoIndicator.setBackgroundResource(R.drawable.bg_status_cerrado);
                    esCerrado = true;
                }
            }

            // Estado y fecha
            String estadoFecha = estado + " • " + t.getFechaReporte();
            tvEstadoFecha.setText(estadoFecha);

            // Si está cerrado, aplicar estilo tachado y grisado
            if (esCerrado) {
                tvEstadoFecha.setPaintFlags(tvEstadoFecha.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvEstadoFecha.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextTertiary));
                tvSucursal.setPaintFlags(tvSucursal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvSucursal.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextTertiary));
                tvDescripcion.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextTertiary));
            } else {
                tvEstadoFecha.setPaintFlags(tvEstadoFecha.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvEstadoFecha.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextSecondary));
                tvSucursal.setPaintFlags(tvSucursal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvSucursal.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextPrimary));
                tvDescripcion.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextSecondary));
            }

            tvDescripcion.setText(t.getDescripcion());

            String tecnico = t.getTecnicoAsignado();
            if (tecnico == null || tecnico.isEmpty() || tecnico.equalsIgnoreCase("sin asignar")) {
                tvTecnico.setText("Sin asignar");
            } else {
                tvTecnico.setText(tecnico);
            }
        }
    }
}

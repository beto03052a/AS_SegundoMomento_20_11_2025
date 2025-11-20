package com.example.ticketsapp;

import android.content.Context;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.ArrayAdapter;

import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import java.util.List;

public class TicketsAdapter extends ArrayAdapter<Ticket> {

    public TicketsAdapter(@NonNull Context context, @NonNull List<Ticket> objects) {

        super(context, 0, objects);

    }

    @NonNull

    @Override

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {

            convertView = LayoutInflater.from(getContext())

                    .inflate(R.layout.item_ticket, parent, false);

        }

        Ticket t = getItem(position);

        TextView tvSucursal = convertView.findViewById(R.id.tvSucursal);
        TextView tvCategoriaPrioridad = convertView.findViewById(R.id.tvCategoriaPrioridad);
        TextView tvEstadoFecha = convertView.findViewById(R.id.tvEstadoFecha);
        TextView tvDescripcion = convertView.findViewById(R.id.tvDescripcion);
        TextView tvTecnico = convertView.findViewById(R.id.tvTecnico);
        TextView tvPrioridadBadge = convertView.findViewById(R.id.tvPrioridadBadge);

        if (t != null) {
            tvSucursal.setText(t.getSucursal());

            // Formato mejorado para categoría
            String categoria = t.getCategoria();
            tvCategoriaPrioridad.setText(categoria);

            // Badge de prioridad
            if (tvPrioridadBadge != null) {
                String prioridad = t.getPrioridad().toUpperCase();
                tvPrioridadBadge.setText(prioridad);
            }

            tvEstadoFecha.setText(t.getEstado() + " • " + t.getFechaReporte());
            tvDescripcion.setText(t.getDescripcion());

            String tecnico = t.getTecnicoAsignado();
            if (tecnico == null || tecnico.isEmpty() || tecnico.equalsIgnoreCase("sin asignar")) {
                tvTecnico.setText("Sin asignar");
            } else {
                tvTecnico.setText(tecnico);
            }
        }

        return convertView;

    }

}


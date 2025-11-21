package com.example.ticketsapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Maneja el guardado/carga local de tickets para que la app funcione sin conexión.
 */
public class TicketsLocalStorage {

    private static final String PREFS_NAME = "tickets_prefs";
    private static final String KEY_TICKETS_JSON = "tickets_json";

    public static void guardarTickets(Context context, List<Ticket> tickets) {
        if (context == null || tickets == null) return;

        JSONArray array = new JSONArray();
        for (Ticket t : tickets) {
            if (t == null) continue;
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", t.getId());
                obj.put("sucursal", t.getSucursal());
                obj.put("categoria", t.getCategoria());
                obj.put("prioridad", t.getPrioridad());
                obj.put("estado", t.getEstado());
                obj.put("fechaReporte", t.getFechaReporte());
                obj.put("descripcion", t.getDescripcion());
                obj.put("tecnicoAsignado", t.getTecnicoAsignado());
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TICKETS_JSON, array.toString()).apply();
    }

    public static List<Ticket> cargarTickets(Context context) {
        List<Ticket> resultado = new ArrayList<>();
        if (context == null) return resultado;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_TICKETS_JSON, null);
        if (json == null || json.isEmpty()) {
            return resultado;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                // Reutilizamos el parser que ya tienes
                Ticket t = TicketsApiService.parseTicket(obj);
                if (t != null) {
                    resultado.add(t);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultado;
    }
}



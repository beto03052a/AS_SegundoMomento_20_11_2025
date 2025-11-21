package com.example.ticketsapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Administra una cola de operaciones pendientes (CREATE/UPDATE/DELETE)
 * para sincronizar cambios hechos sin conexión cuando vuelva el internet.
 */
public class TicketsSyncManager {

    private static final String PREFS_NAME = "tickets_prefs";
    private static final String KEY_PENDING_OPS = "pending_ops";

    private static final String TYPE_CREATE = "CREATE";
    private static final String TYPE_UPDATE = "UPDATE";
    private static final String TYPE_DELETE = "DELETE";

    // ==== API pública para encolar operaciones ====

    public static void enqueueCreate(Context context, Ticket ticket) {
        enqueueOperation(context, TYPE_CREATE, ticket);
    }

    public static void enqueueUpdate(Context context, Ticket ticket) {
        enqueueOperation(context, TYPE_UPDATE, ticket);
    }

    public static void enqueueDelete(Context context, Ticket ticket) {
        enqueueOperation(context, TYPE_DELETE, ticket);
    }

    // ==== Procesar cola cuando haya internet ====

    public static void sincronizarPendientes(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PENDING_OPS, null);
        if (json == null || json.isEmpty()) {
            return; // nada que hacer
        }

        try {
            JSONArray cola = new JSONArray(json);

            // Cargamos la lista local actual para irla actualizando
            List<Ticket> listaLocal = TicketsLocalStorage.cargarTickets(context);

            for (int i = 0; i < cola.length(); i++) {
                JSONObject op = cola.getJSONObject(i);
                String type = op.optString("type", "");
                JSONObject ticketJson = op.optJSONObject("ticket");
                if (ticketJson == null) continue;

                Ticket ticket = jsonToTicket(ticketJson);
                if (ticket == null) continue;

                try {
                    if (TYPE_CREATE.equals(type)) {
                        // Crear en API
                        Ticket creado = TicketsApiService.crearTicket(
                                new Ticket(ticket.getSucursal(),
                                        ticket.getCategoria(),
                                        ticket.getPrioridad(),
                                        ticket.getEstado(),
                                        ticket.getFechaReporte(),
                                        ticket.getDescripcion(),
                                        ticket.getTecnicoAsignado())
                        );

                        String oldId = ticket.getId();
                        String newId = creado.getId();

                        // Actualizar lista local: reemplazar ticket con id local por el real
                        replaceTicketById(listaLocal, oldId, creado);

                        // Actualizar ids en operaciones futuras de la cola
                        for (int j = i + 1; j < cola.length(); j++) {
                            JSONObject op2 = cola.getJSONObject(j);
                            JSONObject ticketJson2 = op2.optJSONObject("ticket");
                            if (ticketJson2 == null) continue;
                            String id2 = ticketJson2.optString("id", null);
                            if (id2 != null && id2.equals(oldId)) {
                                ticketJson2.put("id", newId);
                            }
                        }

                    } else if (TYPE_UPDATE.equals(type)) {
                        // Actualizar en API
                        Ticket actualizado = TicketsApiService.actualizarTicket(ticket);
                        replaceTicketById(listaLocal, ticket.getId(), actualizado);

                    } else if (TYPE_DELETE.equals(type)) {
                        // Eliminar en API
                        TicketsApiService.eliminarTicket(ticket.getId());
                        removeTicketById(listaLocal, ticket.getId());
                    }

                } catch (IOException | JSONException e) {
                    // Si falla una operación, detenemos la sincronización
                    // para reintentar completa más tarde.
                    e.printStackTrace();
                    break;
                }
            }

            // Guardar lista local actualizada y limpiar la cola
            TicketsLocalStorage.guardarTickets(context, listaLocal);
            prefs.edit().remove(KEY_PENDING_OPS).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ==== Internos ====

    private static void enqueueOperation(Context context, String type, Ticket ticket) {
        if (context == null || ticket == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PENDING_OPS, null);

        JSONArray cola;
        try {
            if (json == null || json.isEmpty()) {
                cola = new JSONArray();
            } else {
                cola = new JSONArray(json);
            }

            JSONObject op = new JSONObject();
            op.put("type", type);
            op.put("ticket", ticketToJson(ticket));
            cola.put(op);

            prefs.edit().putString(KEY_PENDING_OPS, cola.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject ticketToJson(Ticket t) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", t.getId());
        obj.put("sucursal", t.getSucursal());
        obj.put("categoria", t.getCategoria());
        obj.put("prioridad", t.getPrioridad());
        obj.put("estado", t.getEstado());
        obj.put("fechaReporte", t.getFechaReporte());
        obj.put("descripcion", t.getDescripcion());
        obj.put("tecnicoAsignado", t.getTecnicoAsignado());
        return obj;
    }

    private static Ticket jsonToTicket(JSONObject obj) {
        if (obj == null) return null;
        return new Ticket(
                obj.optString("id", null),
                obj.optString("sucursal", ""),
                obj.optString("categoria", ""),
                obj.optString("prioridad", ""),
                obj.optString("estado", ""),
                obj.optString("fechaReporte", ""),
                obj.optString("descripcion", ""),
                obj.optString("tecnicoAsignado", "")
        );
    }

    private static void replaceTicketById(List<Ticket> lista, String id, Ticket nuevo) {
        if (lista == null || id == null) return;
        for (int i = 0; i < lista.size(); i++) {
            Ticket t = lista.get(i);
            if (t.getId() != null && t.getId().equals(id)) {
                lista.set(i, nuevo);
                return;
            }
        }
        // Si no estaba, lo agregamos
        lista.add(nuevo);
    }

    private static void removeTicketById(List<Ticket> lista, String id) {
        if (lista == null || id == null) return;
        List<Ticket> toRemove = new ArrayList<>();
        for (Ticket t : lista) {
            if (t.getId() != null && t.getId().equals(id)) {
                toRemove.add(t);
            }
        }
        lista.removeAll(toRemove);
    }
}



package com.example.ticketsapp;

import org.json.JSONArray;

import org.json.JSONException;

import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.BufferedWriter;

import java.io.IOException;

import java.io.InputStream;

import java.io.InputStreamReader;

import java.io.OutputStream;

import java.io.OutputStreamWriter;

import java.net.HttpURLConnection;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

import java.util.List;

public class TicketsApiService {

    private static final String BASE_URL =

            "https://691f634d31e684d7bfc9962b.mockapi.io/api/v1/tickets";

    private static String leerRespuesta(HttpURLConnection conn) throws IOException {

        int code = conn.getResponseCode();

        InputStream is = (code >= 200 && code < 300)

                ? conn.getInputStream()

                : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(

                new InputStreamReader(is, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();

        String linea;

        while ((linea = reader.readLine()) != null) {

            sb.append(linea);

        }

        reader.close();

        return sb.toString();

    }

    // GET

    public static List<Ticket> obtenerTickets() throws IOException, JSONException {

        URL url = new URL(BASE_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");

        conn.setConnectTimeout(10000);

        conn.setReadTimeout(10000);

        String json = leerRespuesta(conn);

        conn.disconnect();

        List<Ticket> lista = new ArrayList<>();

        JSONArray arr = new JSONArray(json);

        for (int i = 0; i < arr.length(); i++) {

            lista.add(parseTicket(arr.getJSONObject(i)));

        }

        return lista;

    }

    // POST

    public static Ticket crearTicket(Ticket t) throws IOException, JSONException {

        URL url = new URL(BASE_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");

        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        conn.setDoOutput(true);

        JSONObject json = new JSONObject();

        json.put("sucursal", t.getSucursal());

        json.put("categoria", t.getCategoria());

        json.put("prioridad", t.getPrioridad());

        json.put("estado", t.getEstado());

        json.put("fechaReporte", t.getFechaReporte());

        json.put("descripcion", t.getDescripcion());

        json.put("tecnicoAsignado", t.getTecnicoAsignado());

        OutputStream os = conn.getOutputStream();

        BufferedWriter writer = new BufferedWriter(

                new OutputStreamWriter(os, StandardCharsets.UTF_8));

        writer.write(json.toString());

        writer.flush();

        writer.close();

        os.close();

        String resp = leerRespuesta(conn);

        conn.disconnect();

        return parseTicket(new JSONObject(resp));

    }

    // PUT

    public static Ticket actualizarTicket(Ticket t) throws IOException, JSONException {

        URL url = new URL(BASE_URL + "/" + t.getId());

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("PUT");

        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        conn.setDoOutput(true);

        JSONObject json = new JSONObject();

        json.put("sucursal", t.getSucursal());

        json.put("categoria", t.getCategoria());

        json.put("prioridad", t.getPrioridad());

        json.put("estado", t.getEstado());

        json.put("fechaReporte", t.getFechaReporte());

        json.put("descripcion", t.getDescripcion());

        json.put("tecnicoAsignado", t.getTecnicoAsignado());

        OutputStream os = conn.getOutputStream();

        BufferedWriter writer = new BufferedWriter(

                new OutputStreamWriter(os, StandardCharsets.UTF_8));

        writer.write(json.toString());

        writer.flush();

        writer.close();

        os.close();

        String resp = leerRespuesta(conn);

        conn.disconnect();

        return parseTicket(new JSONObject(resp));

    }

    // DELETE

    public static boolean eliminarTicket(String id) throws IOException {

        URL url = new URL(BASE_URL + "/" + id);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("DELETE");

        conn.setConnectTimeout(10000);

        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();

        conn.disconnect();

        return code >= 200 && code < 300;

    }

    // Parse JSON -> Ticket

    public static Ticket parseTicket(JSONObject obj) throws JSONException {

        String id = obj.optString("id", null);

        String sucursal = obj.optString("sucursal", "");

        String categoria = obj.optString("categoria", "");

        String prioridad = obj.optString("prioridad", "");

        String estado = obj.optString("estado", "");

        String fechaReporte = obj.optString("fechaReporte", "");

        String descripcion = obj.optString("descripcion", "");

        String tecnicoAsignado = obj.optString("tecnicoAsignado", "");

        return new Ticket(id, sucursal, categoria, prioridad,

                estado, fechaReporte, descripcion, tecnicoAsignado);

    }

}


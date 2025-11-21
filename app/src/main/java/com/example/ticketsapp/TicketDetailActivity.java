package com.example.ticketsapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TicketDetailActivity extends AppCompatActivity {

    private Ticket ticket;
    private TextView tvSucursal, tvCategoria, tvEstado, tvFecha, tvDescripcion, tvTecnico, tvPrioridadBadge;
    private View viewEstadoIndicator;
    private Button btnEditar, btnCambiarEstado;

    // Para DatePicker
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        String ticketId = getIntent().getStringExtra("ticket_id");

        // Buscar el ticket en la lista (en una app real, lo cargarías desde la API o BD)
        ticket = buscarTicketPorId(ticketId);

        if (ticket == null) {
            Toast.makeText(this, "Ticket no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
        mostrarDatos();
        configurarBotones();
    }

    private Ticket buscarTicketPorId(String id) {
        // Recuperar datos del Intent
        String sucursal = getIntent().getStringExtra("ticket_sucursal");
        String categoria = getIntent().getStringExtra("ticket_categoria");
        String prioridad = getIntent().getStringExtra("ticket_prioridad");
        String estado = getIntent().getStringExtra("ticket_estado");
        String fecha = getIntent().getStringExtra("ticket_fecha");
        String descripcion = getIntent().getStringExtra("ticket_descripcion");
        String tecnico = getIntent().getStringExtra("ticket_tecnico");

        if (sucursal != null) {
            return new Ticket(id, sucursal, categoria, prioridad, estado, fecha, descripcion, tecnico);
        }
        return null;
    }

    private void inicializarVistas() {
        tvSucursal = findViewById(R.id.tvSucursal);
        tvCategoria = findViewById(R.id.tvCategoria);
        tvEstado = findViewById(R.id.tvEstado);
        tvFecha = findViewById(R.id.tvFecha);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvTecnico = findViewById(R.id.tvTecnico);
        tvPrioridadBadge = findViewById(R.id.tvPrioridadBadge);
        viewEstadoIndicator = findViewById(R.id.viewEstadoIndicator);
        btnEditar = findViewById(R.id.btnEditar);
        btnCambiarEstado = findViewById(R.id.btnCambiarEstado);
    }

    private void mostrarDatos() {
        if (ticket == null) return;

        tvSucursal.setText(ticket.getSucursal());

        // Categoría
        String categoria = ticket.getCategoria();
        tvCategoria.setText(categoria);

        // Prioridad
        if (ticket.getPrioridad() != null) {
            String prioridad = ticket.getPrioridad().toUpperCase();
            tvPrioridadBadge.setText(prioridad);

            String prioridadLower = ticket.getPrioridad().toLowerCase();
            if (prioridadLower.contains("alta")) {
                tvPrioridadBadge.setBackgroundResource(R.drawable.bg_priority_high);
            } else if (prioridadLower.contains("media")) {
                tvPrioridadBadge.setBackgroundResource(R.drawable.bg_priority_media);
            } else if (prioridadLower.contains("baja")) {
                tvPrioridadBadge.setBackgroundResource(R.drawable.bg_priority_baja);
            }
        }

        // Estado
        String estado = ticket.getEstado();
        boolean esCerrado = false;
        if (estado != null) {
            tvEstado.setText(estado);
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

        // Aplicar estilo tachado si está cerrado
        if (esCerrado) {
            tvSucursal.setPaintFlags(tvSucursal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvSucursal.setTextColor(getResources().getColor(R.color.colorTextTertiary, null));
            tvDescripcion.setTextColor(getResources().getColor(R.color.colorTextTertiary, null));
        } else {
            tvSucursal.setPaintFlags(tvSucursal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tvSucursal.setTextColor(getResources().getColor(R.color.colorTextPrimary, null));
            tvDescripcion.setTextColor(getResources().getColor(R.color.colorTextPrimary, null));
        }

        tvFecha.setText(ticket.getFechaReporte());
        tvDescripcion.setText(ticket.getDescripcion());

        String tecnico = ticket.getTecnicoAsignado();
        if (tecnico == null || tecnico.isEmpty() || tecnico.equalsIgnoreCase("sin asignar")) {
            tvTecnico.setText("Sin asignar");
        } else {
            tvTecnico.setText(tecnico);
        }
    }

    private void configurarBotones() {
        btnEditar.setOnClickListener(v -> mostrarDialogoEditar());

        btnCambiarEstado.setOnClickListener(v -> mostrarBottomSheetCambiarEstado());
    }

    private void mostrarDialogoEditar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar ticket");

        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_ticket, null);

        EditText etSucursal = vista.findViewById(R.id.etSucursal);
        EditText etCategoria = vista.findViewById(R.id.etCategoria);
        Spinner spinnerPrioridad = vista.findViewById(R.id.spinnerPrioridad);
        Spinner spinnerEstado = vista.findViewById(R.id.spinnerEstado);
        EditText etFecha = vista.findViewById(R.id.etFechaReporte);
        EditText etDescripcion = vista.findViewById(R.id.etDescripcion);
        EditText etTecnico = vista.findViewById(R.id.etTecnico);

        // TextViews de error
        TextView tvErrorSucursal = vista.findViewById(R.id.tvErrorSucursal);
        TextView tvErrorCategoria = vista.findViewById(R.id.tvErrorCategoria);
        TextView tvErrorFecha = vista.findViewById(R.id.tvErrorFecha);

        // Configurar spinners
        ArrayAdapter<CharSequence> adapterPrioridad = ArrayAdapter.createFromResource(
                this, R.array.prioridades, android.R.layout.simple_spinner_item);
        adapterPrioridad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapterPrioridad);

        ArrayAdapter<CharSequence> adapterEstado = ArrayAdapter.createFromResource(
                this, R.array.estados, android.R.layout.simple_spinner_item);
        adapterEstado.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adapterEstado);

        // Configurar DatePicker
        etFecha.setOnClickListener(v -> mostrarDatePicker(etFecha));

        // Llenar campos con datos actuales
        etSucursal.setText(ticket.getSucursal());
        etCategoria.setText(ticket.getCategoria());

        // Seleccionar prioridad
        String[] prioridades = getResources().getStringArray(R.array.prioridades);
        for (int i = 0; i < prioridades.length; i++) {
            if (prioridades[i].equalsIgnoreCase(ticket.getPrioridad())) {
                spinnerPrioridad.setSelection(i);
                break;
            }
        }

        // Seleccionar estado
        String[] estados = getResources().getStringArray(R.array.estados);
        for (int i = 0; i < estados.length; i++) {
            if (estados[i].equalsIgnoreCase(ticket.getEstado()) ||
                    (estados[i].equalsIgnoreCase("Cerrado") && ticket.getEstado().equalsIgnoreCase("Resuelto"))) {
                spinnerEstado.setSelection(i);
                break;
            }
        }

        etFecha.setText(ticket.getFechaReporte());
        etDescripcion.setText(ticket.getDescripcion());
        etTecnico.setText(ticket.getTecnicoAsignado());

        builder.setView(vista);
        builder.setPositiveButton("Guardar", null);
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            Button btnPositivo = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            btnPositivo.setOnClickListener(v -> {
                // Ocultar errores previos
                tvErrorSucursal.setVisibility(View.GONE);
                tvErrorCategoria.setVisibility(View.GONE);
                tvErrorFecha.setVisibility(View.GONE);

                String sucursal = etSucursal.getText().toString().trim();
                String categoria = etCategoria.getText().toString().trim();
                String prioridad = spinnerPrioridad.getSelectedItem().toString();
                String estado = spinnerEstado.getSelectedItem().toString();
                String fecha = etFecha.getText().toString().trim();
                String descripcion = etDescripcion.getText().toString().trim();
                String tecnico = etTecnico.getText().toString().trim();

                // Validación
                boolean valido = true;
                if (sucursal.isEmpty()) {
                    tvErrorSucursal.setVisibility(View.VISIBLE);
                    valido = false;
                }
                if (categoria.isEmpty()) {
                    tvErrorCategoria.setVisibility(View.VISIBLE);
                    valido = false;
                }
                if (fecha.isEmpty()) {
                    tvErrorFecha.setVisibility(View.VISIBLE);
                    valido = false;
                }

                if (!valido) {
                    return;
                }

                if (hayInternet()) {
                    editarTicketApi(sucursal, categoria, prioridad, estado, fecha, descripcion, tecnico, dialog);
                } else {
                    // Edición solo local (offline)
                    editarTicketLocal(sucursal, categoria, prioridad, estado, fecha, descripcion, tecnico, dialog);
                }
            });
        });

        dialog.show();
    }

    private void mostrarDatePicker(EditText etFecha) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    etFecha.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void editarTicketApi(String sucursal, String categoria, String prioridad,
                                 String estado, String fecha, String descripcion,
                                 String tecnico, AlertDialog dialog) {
        new Thread(() -> {
            try {
                ticket.setSucursal(sucursal);
                ticket.setCategoria(categoria);
                ticket.setPrioridad(prioridad);
                ticket.setEstado(estado);
                ticket.setFechaReporte(fecha);
                ticket.setDescripcion(descripcion);
                ticket.setTecnicoAsignado(tecnico);

                Ticket actualizado = TicketsApiService.actualizarTicket(ticket);
                ticket = actualizado;

                runOnUiThread(() -> {
                    mostrarDatos();
                    dialog.dismiss();
                    Toast.makeText(TicketDetailActivity.this,
                            "Ticket actualizado", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(TicketDetailActivity.this,
                                "Error actualizando ticket", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Editar ticket solo en almacenamiento local (offline) y encolar para sincronizar
    private void editarTicketLocal(String sucursal, String categoria, String prioridad,
                                   String estado, String fecha, String descripcion,
                                   String tecnico, AlertDialog dialog) {
        ticket.setSucursal(sucursal);
        ticket.setCategoria(categoria);
        ticket.setPrioridad(prioridad);
        ticket.setEstado(estado);
        ticket.setFechaReporte(fecha);
        ticket.setDescripcion(descripcion);
        ticket.setTecnicoAsignado(tecnico);

        // Actualizar ticket en caché local
        List<Ticket> lista = TicketsLocalStorage.cargarTickets(this);
        for (int i = 0; i < lista.size(); i++) {
            Ticket t = lista.get(i);
            if (t.getId() != null && t.getId().equals(ticket.getId())) {
                lista.set(i, ticket);
                break;
            }
        }
        TicketsLocalStorage.guardarTickets(this, lista);
        TicketsSyncManager.enqueueUpdate(this, ticket);

        mostrarDatos();
        dialog.dismiss();
        Toast.makeText(this,
                "Ticket actualizado (offline, no sincronizado con la API)", Toast.LENGTH_SHORT).show();
    }

    private void mostrarBottomSheetCambiarEstado() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_cambiar_estado, null);
        bottomSheet.setContentView(view);

        Button btnPendiente = view.findViewById(R.id.btnEstadoPendiente);
        Button btnProgreso = view.findViewById(R.id.btnEstadoProgreso);
        Button btnCerrado = view.findViewById(R.id.btnEstadoCerrado);

        btnPendiente.setOnClickListener(v -> {
            cambiarEstado("Pendiente");
            bottomSheet.dismiss();
        });

        btnProgreso.setOnClickListener(v -> {
            cambiarEstado("En progreso");
            bottomSheet.dismiss();
        });

        btnCerrado.setOnClickListener(v -> {
            cambiarEstado("Cerrado");
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    private void cambiarEstado(String nuevoEstado) {
        if (hayInternet()) {
            new Thread(() -> {
                try {
                    ticket.setEstado(nuevoEstado);
                    Ticket actualizado = TicketsApiService.actualizarTicket(ticket);
                    ticket = actualizado;

                    runOnUiThread(() -> {
                        mostrarDatos();
                        Toast.makeText(TicketDetailActivity.this,
                                "Estado actualizado a: " + nuevoEstado, Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(TicketDetailActivity.this,
                                    "Error actualizando estado", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            // Cambio de estado solo local (offline) y encolar para sincronizar
            ticket.setEstado(nuevoEstado);

            List<Ticket> lista = TicketsLocalStorage.cargarTickets(this);
            for (int i = 0; i < lista.size(); i++) {
                Ticket t = lista.get(i);
                if (t.getId() != null && t.getId().equals(ticket.getId())) {
                    lista.set(i, ticket);
                    break;
                }
            }
            TicketsLocalStorage.guardarTickets(this, lista);
            TicketsSyncManager.enqueueUpdate(this, ticket);

            mostrarDatos();
            Toast.makeText(this,
                    "Estado actualizado a: " + nuevoEstado + " (offline, se sincronizará con la API)", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}

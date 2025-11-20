package com.example.ticketsapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewTickets;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnAgregar;
    private Spinner spinnerFiltroEstado;
    private Spinner spinnerOrdenar;

    // TextViews de estadísticas
    private TextView tvTotal, tvPendientes, tvProgreso, tvCerrados;

    // Lista completa y lista filtrada
    private List<Ticket> listaTicketsCompleta = new ArrayList<>();
    private List<Ticket> listaTicketsFiltrada = new ArrayList<>();

    private TicketsRecyclerAdapter adapter;

    // Filtros y ordenamiento
    private String filtroEstadoActual = "Todos";
    private String ordenamientoActual = "Más recientes";

    // Para DatePicker
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        recyclerViewTickets = findViewById(R.id.recyclerViewTickets);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnAgregar = findViewById(R.id.btnAgregar);
        spinnerFiltroEstado = findViewById(R.id.spinnerFiltroEstado);
        spinnerOrdenar = findViewById(R.id.spinnerOrdenar);

        // Inicializar TextViews de estadísticas
        tvTotal = findViewById(R.id.tvTotal);
        tvPendientes = findViewById(R.id.tvPendientes);
        tvProgreso = findViewById(R.id.tvProgreso);
        tvCerrados = findViewById(R.id.tvCerrados);

        // Configurar RecyclerView
        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TicketsRecyclerAdapter(listaTicketsFiltrada, new TicketsRecyclerAdapter.OnTicketClickListener() {
            @Override
            public void onTicketClick(Ticket ticket) {
                // Abrir TicketDetailActivity
                Intent intent = new Intent(MainActivity.this, TicketDetailActivity.class);
                intent.putExtra("ticket_id", ticket.getId());
                intent.putExtra("ticket_sucursal", ticket.getSucursal());
                intent.putExtra("ticket_categoria", ticket.getCategoria());
                intent.putExtra("ticket_prioridad", ticket.getPrioridad());
                intent.putExtra("ticket_estado", ticket.getEstado());
                intent.putExtra("ticket_fecha", ticket.getFechaReporte());
                intent.putExtra("ticket_descripcion", ticket.getDescripcion());
                intent.putExtra("ticket_tecnico", ticket.getTecnicoAsignado());
                startActivity(intent);
            }

            @Override
            public void onTicketLongClick(Ticket ticket) {
                confirmarEliminar(ticket);
            }
        });
        recyclerViewTickets.setAdapter(adapter);

        // Configurar SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            cargarTickets();
        });
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.colorPrimary, null),
                getResources().getColor(R.color.colorAccent, null)
        );

        // Configurar ItemTouchHelper para gestos swipe
        configurarSwipeGestures();

        // Configurar spinners
        configurarSpinners();

        btnAgregar.setOnClickListener(v -> mostrarDialogoTicket(null));

        cargarTickets();
    }

    private void configurarSwipeGestures() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Ticket ticket = listaTicketsFiltrada.get(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe derecha → Marcar como Cerrado
                    marcarComoCerrado(ticket);
                } else if (direction == ItemTouchHelper.LEFT) {
                    // Swipe izquierda → Eliminar
                    confirmarEliminarSwipe(ticket, position);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewTickets);
    }

    private void marcarComoCerrado(Ticket ticket) {
        if (!hayInternet()) {
            Toast.makeText(this, "Se requiere internet para actualizar", Toast.LENGTH_SHORT).show();
            aplicarFiltrosYOrdenamiento();
            return;
        }

        new Thread(() -> {
            try {
                ticket.setEstado("Cerrado");
                Ticket actualizado = TicketsApiService.actualizarTicket(ticket);

                int index = -1;
                for (int i = 0; i < listaTicketsCompleta.size(); i++) {
                    if (listaTicketsCompleta.get(i).getId().equals(actualizado.getId())) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    listaTicketsCompleta.set(index, actualizado);
                }

                runOnUiThread(() -> {
                    actualizarEstadisticas();
                    aplicarFiltrosYOrdenamiento();
                    Toast.makeText(MainActivity.this, "Ticket marcado como Cerrado", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    aplicarFiltrosYOrdenamiento();
                    Toast.makeText(MainActivity.this, "Error actualizando ticket", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void confirmarEliminarSwipe(Ticket ticket, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar ticket")
                .setMessage("¿Seguro que quieres eliminar este ticket?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTicketApi(ticket))
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    aplicarFiltrosYOrdenamiento();
                })
                .show();
    }

    private void configurarSpinners() {
        // Spinner de filtro por estado
        ArrayAdapter<CharSequence> adapterFiltro = ArrayAdapter.createFromResource(
                this,
                R.array.filtros_estado,
                android.R.layout.simple_spinner_item
        );
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroEstado.setAdapter(adapterFiltro);

        spinnerFiltroEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] filtros = getResources().getStringArray(R.array.filtros_estado);
                filtroEstadoActual = filtros[position];
                aplicarFiltrosYOrdenamiento();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner de ordenamiento
        ArrayAdapter<CharSequence> adapterOrdenar = ArrayAdapter.createFromResource(
                this,
                R.array.ordenamientos,
                android.R.layout.simple_spinner_item
        );
        adapterOrdenar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrdenar.setAdapter(adapterOrdenar);

        spinnerOrdenar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] ordenamientos = getResources().getStringArray(R.array.ordenamientos);
                ordenamientoActual = ordenamientos[position];
                aplicarFiltrosYOrdenamiento();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void aplicarFiltrosYOrdenamiento() {
        // Filtrar
        listaTicketsFiltrada.clear();

        if (filtroEstadoActual.equals("Todos")) {
            listaTicketsFiltrada.addAll(listaTicketsCompleta);
        } else {
            for (Ticket t : listaTicketsCompleta) {
                String estado = t.getEstado();
                if (estado != null) {
                    String estadoLower = estado.toLowerCase();
                    if (filtroEstadoActual.equals("Pendiente") && estadoLower.contains("pendiente")) {
                        listaTicketsFiltrada.add(t);
                    } else if (filtroEstadoActual.equals("En progreso") &&
                            (estadoLower.contains("progreso") || estadoLower.contains("en progreso"))) {
                        listaTicketsFiltrada.add(t);
                    } else if (filtroEstadoActual.equals("Cerrado") &&
                            (estadoLower.contains("cerrado") || estadoLower.contains("resuelto"))) {
                        listaTicketsFiltrada.add(t);
                    }
                }
            }
        }

        // Ordenar
        if (ordenamientoActual.equals("Más recientes")) {
            // Mantener orden de inserción (más recientes primero)
            Collections.reverse(listaTicketsFiltrada);
        } else if (ordenamientoActual.equals("Más antiguos")) {
            // Orden normal
        } else if (ordenamientoActual.equals("Prioridad alta primero")) {
            Collections.sort(listaTicketsFiltrada, (t1, t2) -> {
                int prioridad1 = obtenerValorPrioridad(t1.getPrioridad());
                int prioridad2 = obtenerValorPrioridad(t2.getPrioridad());
                return Integer.compare(prioridad2, prioridad1);
            });
        }

        adapter.notifyDataSetChanged();
    }

    private int obtenerValorPrioridad(String prioridad) {
        if (prioridad == null) return 0;
        String p = prioridad.toLowerCase();
        if (p.contains("alta")) return 3;
        if (p.contains("media")) return 2;
        if (p.contains("baja")) return 1;
        return 0;
    }

    private void actualizarEstadisticas() {
        int total = listaTicketsCompleta.size();
        int pendientes = 0;
        int enProgreso = 0;
        int cerrados = 0;

        for (Ticket t : listaTicketsCompleta) {
            String estado = t.getEstado();
            if (estado != null) {
                String estadoLower = estado.toLowerCase();
                if (estadoLower.contains("pendiente")) {
                    pendientes++;
                } else if (estadoLower.contains("progreso") || estadoLower.contains("en progreso")) {
                    enProgreso++;
                } else if (estadoLower.contains("cerrado") || estadoLower.contains("resuelto")) {
                    cerrados++;
                }
            }
        }

        tvTotal.setText(String.valueOf(total));
        tvPendientes.setText(String.valueOf(pendientes));
        tvProgreso.setText(String.valueOf(enProgreso));
        tvCerrados.setText(String.valueOf(cerrados));
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void cargarTickets() {
        if (!hayInternet()) {
            Toast.makeText(this, "Sin conexión. No se puede cargar la API.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                List<Ticket> desdeApi = TicketsApiService.obtenerTickets();

                listaTicketsCompleta.clear();
                listaTicketsCompleta.addAll(desdeApi);

                runOnUiThread(() -> {
                    actualizarEstadisticas();
                    aplicarFiltrosYOrdenamiento();
                    swipeRefreshLayout.setRefreshing(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this,
                            "Error cargando tickets", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificar si viene de editar desde TicketDetailActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("editar_ticket_id")) {
            String ticketId = intent.getStringExtra("editar_ticket_id");
            Ticket ticket = buscarTicketPorId(ticketId);
            if (ticket != null) {
                mostrarDialogoTicket(ticket);
            }
            // Limpiar el extra para evitar que se abra cada vez
            intent.removeExtra("editar_ticket_id");
        } else {
            // Recargar cuando vuelve de TicketDetailActivity
            if (hayInternet()) {
                cargarTickets();
            }
        }
    }

    private Ticket buscarTicketPorId(String id) {
        for (Ticket t : listaTicketsCompleta) {
            if (t.getId() != null && t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    private void mostrarDialogoTicket(Ticket ticketExistente) {
        boolean esEdicion = ticketExistente != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(esEdicion ? "Editar ticket" : "Nuevo ticket");

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

        // Llenar campos si es edición
        if (esEdicion) {
            etSucursal.setText(ticketExistente.getSucursal());
            etCategoria.setText(ticketExistente.getCategoria());

            // Seleccionar prioridad
            String[] prioridades = getResources().getStringArray(R.array.prioridades);
            for (int i = 0; i < prioridades.length; i++) {
                if (prioridades[i].equalsIgnoreCase(ticketExistente.getPrioridad())) {
                    spinnerPrioridad.setSelection(i);
                    break;
                }
            }

            // Seleccionar estado
            String[] estados = getResources().getStringArray(R.array.estados);
            for (int i = 0; i < estados.length; i++) {
                if (estados[i].equalsIgnoreCase(ticketExistente.getEstado()) ||
                        (estados[i].equalsIgnoreCase("Cerrado") && ticketExistente.getEstado().equalsIgnoreCase("Resuelto"))) {
                    spinnerEstado.setSelection(i);
                    break;
                }
            }

            etFecha.setText(ticketExistente.getFechaReporte());
            etDescripcion.setText(ticketExistente.getDescripcion());
            etTecnico.setText(ticketExistente.getTecnicoAsignado());
        }

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

                if (!hayInternet()) {
                    Toast.makeText(MainActivity.this,
                            "Se requiere internet para guardar en la API", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (esEdicion) {
                    editarTicketApi(ticketExistente, sucursal, categoria, prioridad,
                            estado, fecha, descripcion, tecnico, dialog);
                } else {
                    crearTicketApi(sucursal, categoria, prioridad,
                            estado, fecha, descripcion, tecnico, dialog);
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

    private void crearTicketApi(String sucursal, String categoria, String prioridad,
                                String estado, String fecha, String descripcion,
                                String tecnico, AlertDialog dialog) {
        new Thread(() -> {
            try {
                Ticket nuevo = new Ticket(sucursal, categoria, prioridad,
                        estado, fecha, descripcion, tecnico);
                Ticket creado = TicketsApiService.crearTicket(nuevo);

                listaTicketsCompleta.add(0, creado);

                runOnUiThread(() -> {
                    actualizarEstadisticas();
                    aplicarFiltrosYOrdenamiento();
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this,
                            "Ticket creado", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Error creando ticket", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void editarTicketApi(Ticket original, String sucursal, String categoria,
                                 String prioridad, String estado, String fecha,
                                 String descripcion, String tecnico, AlertDialog dialog) {
        new Thread(() -> {
            try {
                original.setSucursal(sucursal);
                original.setCategoria(categoria);
                original.setPrioridad(prioridad);
                original.setEstado(estado);
                original.setFechaReporte(fecha);
                original.setDescripcion(descripcion);
                original.setTecnicoAsignado(tecnico);

                Ticket actualizado = TicketsApiService.actualizarTicket(original);

                int index = -1;
                for (int i = 0; i < listaTicketsCompleta.size(); i++) {
                    if (listaTicketsCompleta.get(i).getId().equals(actualizado.getId())) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    listaTicketsCompleta.set(index, actualizado);
                }

                runOnUiThread(() -> {
                    actualizarEstadisticas();
                    aplicarFiltrosYOrdenamiento();
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this,
                            "Ticket actualizado", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Error actualizando ticket", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void confirmarEliminar(Ticket t) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar ticket")
                .setMessage("¿Seguro que quieres eliminar este ticket?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTicketApi(t))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTicketApi(Ticket t) {
        if (!hayInternet()) {
            Toast.makeText(this,
                    "Se requiere internet para eliminar en la API", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                boolean ok = TicketsApiService.eliminarTicket(t.getId());

                if (ok) {
                    listaTicketsCompleta.remove(t);

                    runOnUiThread(() -> {
                        actualizarEstadisticas();
                        aplicarFiltrosYOrdenamiento();
                        Toast.makeText(MainActivity.this,
                                "Ticket eliminado", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Error eliminando en la API", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Error eliminando ticket", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}

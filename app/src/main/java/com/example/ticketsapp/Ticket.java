package com.example.ticketsapp;

public class Ticket {

    private String id;

    private String sucursal;

    private String categoria;

    private String prioridad;

    private String estado;

    private String fechaReporte;

    private String descripcion;

    private String tecnicoAsignado;

    public Ticket(String id, String sucursal, String categoria, String prioridad,

                  String estado, String fechaReporte, String descripcion,

                  String tecnicoAsignado) {

        this.id = id;

        this.sucursal = sucursal;

        this.categoria = categoria;

        this.prioridad = prioridad;

        this.estado = estado;

        this.fechaReporte = fechaReporte;

        this.descripcion = descripcion;

        this.tecnicoAsignado = tecnicoAsignado;

    }

    // para crear antes del POST

    public Ticket(String sucursal, String categoria, String prioridad,

                  String estado, String fechaReporte, String descripcion,

                  String tecnicoAsignado) {

        this(null, sucursal, categoria, prioridad, estado, fechaReporte, descripcion, tecnicoAsignado);

    }

    public String getId() { return id; }

    public String getSucursal() { return sucursal; }

    public String getCategoria() { return categoria; }

    public String getPrioridad() { return prioridad; }

    public String getEstado() { return estado; }

    public String getFechaReporte() { return fechaReporte; }

    public String getDescripcion() { return descripcion; }

    public String getTecnicoAsignado() { return tecnicoAsignado; }

    public void setId(String id) { this.id = id; }

    public void setSucursal(String sucursal) { this.sucursal = sucursal; }

    public void setCategoria(String categoria) { this.categoria = categoria; }

    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public void setEstado(String estado) { this.estado = estado; }

    public void setFechaReporte(String fechaReporte) { this.fechaReporte = fechaReporte; }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public void setTecnicoAsignado(String tecnicoAsignado) { this.tecnicoAsignado = tecnicoAsignado; }

}


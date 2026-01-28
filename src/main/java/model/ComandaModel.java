package model;

import java.time.LocalDateTime;

public class ComandaModel {
    private Integer id;

    private String clienteId; // opcional
    private String nomeCliente; // opcional (walk-in)
    private String mesa; // opcional
    private String status; // aberta | pendente | fechada | cancelada

    private double totalBruto;
    private double desconto;
    private double acrescimo;
    private double totalLiquido;
    private double totalPago;

    private String observacoes;

    private LocalDateTime criadoEm;
    private String criadoPor;
    private LocalDateTime fechadoEm;
    private String fechadoPor;
    private Integer tempoPermanenciaMin;
    private LocalDateTime canceladoEm;
    private String canceladoPor;

    public ComandaModel() {
    }

    public double getSaldo() {
        return Math.max(0.0, getTotalLiquido() - getTotalPago());
    }

    // Getters/Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getMesa() {
        return mesa;
    }

    public void setMesa(String mesa) {
        this.mesa = mesa;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalBruto() {
        return totalBruto;
    }

    public void setTotalBruto(double totalBruto) {
        this.totalBruto = totalBruto;
    }

    public double getDesconto() {
        return desconto;
    }

    public void setDesconto(double desconto) {
        this.desconto = desconto;
    }

    public double getAcrescimo() {
        return acrescimo;
    }

    public void setAcrescimo(double acrescimo) {
        this.acrescimo = acrescimo;
    }

    public double getTotalLiquido() {
        return totalLiquido;
    }

    public void setTotalLiquido(double totalLiquido) {
        this.totalLiquido = totalLiquido;
    }

    public double getTotalPago() {
        return totalPago;
    }

    public void setTotalPago(double totalPago) {
        this.totalPago = totalPago;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public LocalDateTime getFechadoEm() {
        return fechadoEm;
    }

    public void setFechadoEm(LocalDateTime fechadoEm) {
        this.fechadoEm = fechadoEm;
    }

    public String getFechadoPor() {
        return fechadoPor;
    }

    public void setFechadoPor(String fechadoPor) {
        this.fechadoPor = fechadoPor;
    }

    public Integer getTempoPermanenciaMin() {
        return tempoPermanenciaMin;
    }

    public void setTempoPermanenciaMin(Integer tempoPermanenciaMin) {
        this.tempoPermanenciaMin = tempoPermanenciaMin;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public void setCanceladoEm(LocalDateTime canceladoEm) {
        this.canceladoEm = canceladoEm;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public void setCanceladoPor(String canceladoPor) {
        this.canceladoPor = canceladoPor;
    }
}

package org.example.models;

public class CarteiraCriptoAtivos {
    private int idCarteiraCriptoAtivos;
    private int idCarteira;
    private int idAtivo;
    private double quantidade;

    public CarteiraCriptoAtivos(int idCarteiraCriptoAtivos, int idCarteira, int idAtivo, double quantidade) {
        this.idCarteiraCriptoAtivos = idCarteiraCriptoAtivos;
        this.idCarteira = idCarteira;
        this.idAtivo = idAtivo;
        this.quantidade = quantidade;
    }

    public int getIdCarteiraCriptoAtivos() {
        return idCarteiraCriptoAtivos;
    }

    public void setIdCarteiraCriptoAtivos(int idCarteiraCriptoAtivos) {
        this.idCarteiraCriptoAtivos = idCarteiraCriptoAtivos;
    }

    public int getIdCarteira() {
        return idCarteira;
    }

    public void setIdCarteira(int idCarteira) {
        this.idCarteira = idCarteira;
    }

    public int getIdAtivo() {
        return idAtivo;
    }

    public void setIdAtivo(int idAtivo) {
        this.idAtivo = idAtivo;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }
}
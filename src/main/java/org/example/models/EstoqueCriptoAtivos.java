package org.example.models;


public class EstoqueCriptoAtivos {
    private int idEstoqueCriptoAtivos;
    private int idEstoque;
    private int idAtivo;
    private double quantidadeDisponivel;

    public EstoqueCriptoAtivos(int idEstoqueCriptoAtivos, int idEstoque, int idAtivo, double quantidadeDisponivel) {
        this.idEstoqueCriptoAtivos = idEstoqueCriptoAtivos;
        this.idEstoque = idEstoque;
        this.idAtivo = idAtivo;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public int getIdEstoqueCriptoAtivos() {
        return idEstoqueCriptoAtivos;
    }

    public void setIdEstoqueCriptoAtivos(int idEstoqueCriptoAtivos) {
        this.idEstoqueCriptoAtivos = idEstoqueCriptoAtivos;
    }

    public int getIdEstoque() {
        return idEstoque;
    }

    public void setIdEstoque(int idEstoque) {
        this.idEstoque = idEstoque;
    }

    public int getIdAtivo() {
        return idAtivo;
    }

    public void setIdAtivo(int idAtivo) {
        this.idAtivo = idAtivo;
    }

    public double getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public void setQuantidadeDisponivel(double quantidadeDisponivel) {
        this.quantidadeDisponivel = quantidadeDisponivel;
    }
}
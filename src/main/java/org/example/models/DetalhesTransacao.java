package org.example.models;

public class DetalhesTransacao {
    private int idDetalhesTransacao;
    private int idTransacao;
    private double taxa;
    private String observacoes;

    public DetalhesTransacao(int idDetalhesTransacao, int idTransacao, double taxa, String observacoes) {
        this.idDetalhesTransacao = idDetalhesTransacao;
        this.idTransacao = idTransacao;
        this.taxa = taxa;
        this.observacoes = observacoes;
    }

    public int getIdDetalhesTransacao() {
        return idDetalhesTransacao;
    }

    public void setIdDetalhesTransacao(int idDetalhesTransacao) {
        this.idDetalhesTransacao = idDetalhesTransacao;
    }

    public int getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(int idTransacao) {
        this.idTransacao = idTransacao;
    }

    public double getTaxa() {
        return taxa;
    }

    public void setTaxa(double taxa) {
        this.taxa = taxa;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
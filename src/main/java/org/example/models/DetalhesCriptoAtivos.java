package org.example.models;

public class DetalhesCriptoAtivos {
    private int idDetalhes;
    private int idAtivo;
    private String descricao;
    private double variacao;

    public DetalhesCriptoAtivos(int idDetalhes, int idAtivo, String descricao, double variacao) {
        this.idDetalhes = idDetalhes;
        this.idAtivo = idAtivo;
        this.descricao = descricao;
        this.variacao = variacao;
    }

    public int getIdDetalhes() {
        return idDetalhes;
    }

    public void setIdDetalhes(int idDetalhes) {
        this.idDetalhes = idDetalhes;
    }

    public int getIdAtivo() {
        return idAtivo;
    }

    public void setIdAtivo(int idAtivo) {
        this.idAtivo = idAtivo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getVariacao() {
        return variacao;
    }

    public void setVariacao(double variacao) {
        this.variacao = variacao;
    }
}
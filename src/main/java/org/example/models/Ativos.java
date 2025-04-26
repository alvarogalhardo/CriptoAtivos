package org.example.models;

public abstract class Ativos {
    protected String idAtivo;
    protected String nome;
    protected double valorAtual;

    public Ativos(String idAtivo, String nome, double valorAtual) {
        this.idAtivo = idAtivo;
        this.nome = nome;
        this.valorAtual = valorAtual;
    }


    public abstract void calcularVariacaoDiaria();


    public String getIdAtivo() {
        return idAtivo;
    }

    public void setIdAtivo(String idAtivo) {
        this.idAtivo = idAtivo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getValorAtual() {
        return valorAtual;
    }

    public void setValorAtual(double valorAtual) {
        this.valorAtual = valorAtual;
    }
}


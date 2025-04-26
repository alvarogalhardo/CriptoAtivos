package org.example.models;

import java.util.Date;

public class Operacao {
    protected Usuario usuario;
    protected double quantidade;
    protected String descricao;
    protected Date data = new Date();


    public Operacao(Usuario usuario, double quantidade, String descricao) {
        this.usuario = usuario;
        this.quantidade = quantidade;
        this.descricao = descricao;
        this.data = data;
    }


    public void executarOperacao() {
        System.out.println("Operação genérica executada.");
    }


    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }
}

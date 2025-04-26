package org.example.models;
import java.util.Date;

public class CriptoAtivo extends Ativos {
    private double variacaoDiaria;

    public CriptoAtivo(String idAtivo, String nome, double valorAtual, double variacaoDiaria) {
        super(idAtivo, nome, valorAtual);
        this.variacaoDiaria = variacaoDiaria;
    }

    @Override
    public void calcularVariacaoDiaria() {

        System.out.println("Variação diária calculada para o criptoativo " + nome);
    }


    public double getVariacaoDiaria() {
        return variacaoDiaria;
    }

    public void setVariacaoDiaria(double variacaoDiaria) {
        this.variacaoDiaria = variacaoDiaria;
    }
}

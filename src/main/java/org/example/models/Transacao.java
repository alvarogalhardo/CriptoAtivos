package org.example.models;

import org.example.enums.TipoTransacao;

import java.util.Date;

public class Transacao extends Operacao {
    private CriptoAtivo criptoAtivo;
    private TipoTransacao tipo;

    public Transacao(Usuario usuario, CriptoAtivo criptoAtivo, double quantidade, TipoTransacao tipo) {
        super(usuario, quantidade, tipo.name());  // Chama o construtor da classe Operacao
        this.criptoAtivo = criptoAtivo;
        this.tipo = tipo;
    }

    @Override
    public void executarOperacao() {
        if (this.tipo == TipoTransacao.compra) {
            System.out.println("Compra de " + quantidade + " unidades de " + criptoAtivo.getNome() + " realizada.");
        } else if (this.tipo == TipoTransacao.venda) {
            System.out.println("Venda de " + quantidade + " unidades de " + criptoAtivo.getNome() + " realizada.");
        }
    }

    // Getters e Setters para os atributos específicos
    public CriptoAtivo getCriptoAtivo() {
        return criptoAtivo;
    }

    public void setCriptoAtivo(CriptoAtivo criptoAtivo) {
        this.criptoAtivo = criptoAtivo;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }
}

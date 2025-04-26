package org.example.models;

import org.example.enums.TipoTransacao;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Carteira {
    private int idCarteira;
    private int idUsuario;
    private HashMap<CriptoAtivo, Double> criptoAtivos;
    private ArrayList<Transacao> transacoes;
    final Usuario usuario;
    private double saldo;

    public Carteira(Usuario usuario) {
        this.criptoAtivos = new HashMap<CriptoAtivo, Double>();
        this.transacoes = new ArrayList<>();
        this.usuario = usuario;
        this.idUsuario = usuario.getIdUsuario();
    }

    public int getIdCarteira() {
        return idCarteira;
    }

    public void setIdCarteira(int idCarteira) {
        this.idCarteira = idCarteira;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public ArrayList<Transacao> getTransacoes() {
        return transacoes;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public HashMap<CriptoAtivo, Double> getCriptoAtivos() {
        return criptoAtivos;
    }

    public double adicionaSaldo(double valor) {
        saldo += valor;
        return saldo;
    }

    public void compraCriptoAtivo(CriptoAtivo criptoAtivo, double quantidade) {
        if (quantidade * criptoAtivo.getValorAtual() > saldo ) {
            return;
        }
        Date data = new Date();
        Transacao transacao = new Transacao(usuario, criptoAtivo, quantidade, TipoTransacao.compra);
        saldo -= quantidade * criptoAtivo.getValorAtual();
        transacoes.add(transacao);
        criptoAtivos.merge(criptoAtivo, quantidade, Double::sum);
    }

    public void vendeCriptoAtivo(CriptoAtivo criptoAtivo, double quantidade) {
        if (!criptoAtivos.containsKey(criptoAtivo)){
            return;
        }
        double valorPossuido = criptoAtivos.get(criptoAtivo);
        if (quantidade > valorPossuido){
            return;
        }
        valorPossuido -= quantidade;
        if (valorPossuido == 0.0) {
            criptoAtivos.remove(criptoAtivo);
        }
        Date data = new Date();
        Transacao transacao = new Transacao(usuario, criptoAtivo, quantidade, TipoTransacao.venda);
        transacoes.add(transacao);
        saldo += quantidade * criptoAtivo.getValorAtual();
    }
}
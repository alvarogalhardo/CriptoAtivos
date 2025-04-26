package org.example.models;


public class Usuario {
    private int idUsuario;
    private String nome;
    private String email;
    private String senha;
    private boolean autenticacao2FA;
    private String cpf;

    public Usuario(String nome, String email, String senha, boolean autenticacao2FA, String cpf) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.autenticacao2FA = autenticacao2FA;
        this.cpf = cpf;
    }

    public Usuario(int idUsuario, String nome, String email, String senha, boolean autenticacao2FA, String cpf) {
        this.idUsuario = idUsuario;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.autenticacao2FA = autenticacao2FA;
        this.cpf = cpf;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public boolean isAutenticacao2FA() { return autenticacao2FA; }
    public String getCpf() { return cpf; }

    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setAutenticacao2FA(boolean autenticacao2FA) { this.autenticacao2FA = autenticacao2FA; }
    public void setCpf(String cpf) { this.cpf = cpf; }
}

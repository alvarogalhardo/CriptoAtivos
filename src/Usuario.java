public class Usuario {
    private String nome;
    private String email;
    private String senha;
    private boolean autenticacao2FA;
    private String cpf;

    public Usuario() {

    }

    public Usuario(String nome, String email, String senha, boolean autenticacao2FA, String cpf) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.autenticacao2FA = autenticacao2FA;
        this.cpf = cpf;
    }


    public String getNome() {

        return nome;
    }

    public void setNome(String nome) {

        this.nome = nome;
    }

    public String getEmail() {

        return email;
    }

    public void setEmail(String email) {

        this.email = email;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public boolean isAutenticacao2FA() {
        return autenticacao2FA;
    }

    public void setAutenticacao2FA(boolean autenticacao2FA) {
        this.autenticacao2FA = autenticacao2FA;
    }

    public String getCpf() {
        return cpf;
    }
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void autenticar() {

    }
}

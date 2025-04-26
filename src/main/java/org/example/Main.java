package org.example;

import org.example.dao.CarteiraDAO;
import org.example.dao.CriptoAtivoDAO;
import org.example.dao.TransacaoDAO;
import org.example.dao.UsuarioDAO;
import org.example.enums.TipoTransacao;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import org.example.models.Carteira;
import org.example.models.CriptoAtivo;
import org.example.models.Transacao;
import org.example.models.Usuario;
import org.example.dao.CriptoAtivoDAO;
import org.example.dao.TransacaoDAO;
import org.example.dao.UsuarioDAO;
import org.example.enums.TipoTransacao;
import org.example.models.Carteira;
import org.example.models.CriptoAtivo;
import org.example.models.Transacao;
import org.example.models.Usuario;

public class Main {
    private static Scanner leitor = new Scanner(System.in);
    private static final org.example.dao.UsuarioDAO usuarioDAO = new org.example.dao.UsuarioDAO();
    private static final org.example.dao.CarteiraDAO carteiraDAO = new org.example.dao.CarteiraDAO();
    private static final org.example.dao.CriptoAtivoDAO criptoAtivoDAO = new org.example.dao.CriptoAtivoDAO();
    private static final org.example.dao.TransacaoDAO transacaoDAO = new org.example.dao.TransacaoDAO();
    private static org.example.models.Usuario usuario;
    private static org.example.models.Carteira carteira;
    private static ArrayList<org.example.models.CriptoAtivo> ativos = new ArrayList<>();
    private static ArrayList<org.example.models.Transacao> transacoes = new ArrayList<>();

    public static void main(String[] args) {
        // Primeiro, executar os testes
        executarTestes();

        // Depois, iniciar o menu interativo
        iniciarMenu();
    }

    private static void executarTestes() {
        System.out.println("\n=== Executando Testes ===");

        // Teste UsuarioDAO
        System.out.println("\n=== Testando UsuarioDAO ===");

        // Criar e inserir usuário
        org.example.models.Usuario novoUsuario = new org.example.models.Usuario("João Silva", "joao@email.com", "senha123", true, "123.456.789-00");
        usuarioDAO.inserirUsuario(novoUsuario);

        // Buscar usuário
        Usuario usuarioEncontrado = usuarioDAO.buscarUsuarioPorEmail("joao@email.com");
        if (usuarioEncontrado != null) {
            System.out.println("Usuário encontrado: " + usuarioEncontrado.getNome());
        }

        // Listar todos os usuários
        List<org.example.models.Usuario> usuarios = usuarioDAO.listarUsuarios();
        System.out.println("\nLista de usuários:");
        for (org.example.models.Usuario u : usuarios) {
            System.out.println("- " + u.getNome());
        }

        // Teste CriptoAtivoDAO
        System.out.println("\n=== Testando CriptoAtivoDAO ===");

        // Criar e inserir criptoativo
        org.example.models.CriptoAtivo novoCriptoAtivo = new org.example.models.CriptoAtivo("BTC", "Bitcoin", 50000.0, 2.5);
        criptoAtivoDAO.inserirCriptoAtivo(novoCriptoAtivo);

        // Buscar criptoativo
        org.example.models.CriptoAtivo criptoAtivoEncontrado = criptoAtivoDAO.buscarCriptoAtivoPorId("BTC");
        if (criptoAtivoEncontrado != null) {
            System.out.println("Criptoativo encontrado: " + criptoAtivoEncontrado.getNome());
        }

        // Listar todos os criptoativos
        List<org.example.models.CriptoAtivo> criptoAtivos = criptoAtivoDAO.listarCriptoAtivos();
        System.out.println("\nLista de criptoativos:");
        for (org.example.models.CriptoAtivo ca : criptoAtivos) {
            System.out.println("- " + ca.getNome() + " (R$ " + ca.getValorAtual() + ")");
        }

        // Teste CarteiraDAO
        System.out.println("\n=== Testando CarteiraDAO ===");

        // Criar e inserir carteira
        org.example.models.Carteira novaCarteira = new org.example.models.Carteira(usuarioEncontrado);
        novaCarteira.adicionaSaldo(10000.0);
        carteiraDAO.inserirCarteira(novaCarteira);

        // Buscar carteira
        org.example.models.Carteira carteiraEncontrada = carteiraDAO.buscarCarteiraPorId(novaCarteira.getIdCarteira());
        if (carteiraEncontrada != null) {
            System.out.println("Carteira encontrada com saldo: R$ " + carteiraEncontrada.getSaldo());
        }

        // Listar todas as carteiras
        List<org.example.models.Carteira> carteiras = carteiraDAO.listarCarteiras();
        System.out.println("\nLista de carteiras:");
        for (org.example.models.Carteira c : carteiras) {
            System.out.println("- Carteira do usuário: " + c.getUsuario().getNome() +
                    " (Saldo: R$ " + c.getSaldo() + ")");
        }

        // Teste TransacaoDAO
        System.out.println("\n=== Testando TransacaoDAO ===");

        // Criar e inserir transação
        org.example.models.Transacao novaTransacao = new org.example.models.Transacao(
                usuarioEncontrado,
                criptoAtivoEncontrado,
                0.5,
                org.example.enums.TipoTransacao.compra
        );
        transacaoDAO.inserirTransacao(novaTransacao);

        // Listar transações por usuário
        List<org.example.models.Transacao> transacoesUsuario = transacaoDAO.listarTransacoesPorUsuario(usuarioEncontrado.getIdUsuario());
        System.out.println("\nTransações do usuário:");
        for (org.example.models.Transacao t : transacoesUsuario) {
            System.out.println("- " + t.getTipo() + " de " + t.getQuantidade() +
                    " " + t.getCriptoAtivo().getNome());
        }

        // Listar transações por criptoativo
        List<org.example.models.Transacao> transacoesCripto = transacaoDAO.listarTransacoesPorCriptoAtivo("BTC");
        System.out.println("\nTransações do criptoativo:");
        for (org.example.models.Transacao t : transacoesCripto) {
            System.out.println("- " + t.getTipo() + " de " + t.getQuantidade() +
                    " por " + t.getUsuario().getNome());
        }
    }

    private static void iniciarMenu() {
        int op;
        do {
            System.out.println("\n===== MENU =====");
            System.out.println("1 - Cadastrar Usuário");
            System.out.println("2 - Exibir informações do Usuário");
            System.out.println("3 - Adicionar Saldo");
            System.out.println("4 - Exibir Saldo");
            System.out.println("5 - Comprar Criptoativo");
            System.out.println("6 - Vender Criptoativo");
            System.out.println("7 - Excluir Usuário");
            System.out.println("8 - Listar Todos os Usuários");
            System.out.println("9 - Fechar o programa");
            System.out.print("Escolha uma opção: ");
            op = leitor.nextInt();
            leitor.nextLine();

            switch (op) {
                case 1:
                    cadastrarUsuario();
                    break;
                case 2:
                    exibirInformacoesUsuario();
                    break;
                case 3:
                    adicionarSaldo();
                    break;
                case 4:
                    exibirSaldo();
                    break;
                case 5:
                    comprarCriptoativo();
                    break;
                case 6:
                    venderCriptoativo();
                    break;
                case 7:
                    excluirUsuario();
                    break;
                case 8:
                    listarUsuarios();
                    break;
                case 9:
                    System.out.println("Finalizando o sistema!");
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        } while (op != 9);

        leitor.close();
    }

    private static void cadastrarUsuario() {
        try {
            System.out.print("Informe o nome do usuário: ");
            String nome = leitor.nextLine();
            System.out.print("Informe o e-mail do usuário: ");
            String email = leitor.nextLine();
            System.out.print("Informe a senha do usuário: ");
            String senha = leitor.nextLine().trim();
            if (senha.isEmpty()) {
                System.out.println("ERRO: Senha não pode ser vazia! Tente novamente.");
                return;
            }
            System.out.print("Informe o CPF do usuário: ");
            String cpf = leitor.nextLine();
            System.out.print("Autenticação 2FA (1 para ativado, 0 para desativado): ");
            boolean autenticacao2FA = leitor.nextInt() == 1;
            leitor.nextLine();

            usuario = new org.example.models.Usuario(nome, email, senha, autenticacao2FA, cpf);
            usuarioDAO.inserirUsuario(usuario);
            carteira = new org.example.models.Carteira(usuario);
            carteiraDAO.inserirCarteira(carteira);
            System.out.println("Usuário cadastrado com sucesso!");
        } catch (InputMismatchException erro) {
            System.out.println("Erro: Entrada inválida.");
            leitor.nextLine();
        }
    }

    private static void exibirInformacoesUsuario() {
        System.out.print("Digite o e-mail do usuário: ");
        String email = leitor.nextLine();
        usuario = usuarioDAO.buscarUsuarioPorEmail(email);

        if (usuario != null) {
            System.out.println("\n=== Informações do Usuário ===");
            System.out.println("Nome: " + usuario.getNome());
            System.out.println("E-mail: " + usuario.getEmail());
            System.out.println("CPF: " + usuario.getCpf());
            System.out.println("Autenticação 2FA: " + (usuario.isAutenticacao2FA() ? "Ativada" : "Desativada"));

            // Buscar e exibir informações da carteira
            carteira = carteiraDAO.buscarCarteiraPorId(usuario.getIdUsuario());
            if (carteira != null) {
                System.out.println("\n=== Informações da Carteira ===");
                System.out.println("Saldo: R$ " + carteira.getSaldo());
                System.out.println("Criptoativos:");
                for (org.example.models.CriptoAtivo cripto : carteira.getCriptoAtivos().keySet()) {
                    System.out.println("- " + cripto.getNome() + ": " + carteira.getCriptoAtivos().get(cripto));
                }
            }
        } else {
            System.out.println("Usuário não encontrado.");
        }
    }

    private static void adicionarSaldo() {
        if (carteira == null) {
            System.out.println("Nenhum usuário logado. Cadastre ou consulte um usuário primeiro.");
            return;
        }
        System.out.print("Informe o valor a ser adicionado ao saldo: ");
        double valor = leitor.nextDouble();
        carteira.adicionaSaldo(valor);
        carteiraDAO.atualizarCarteira(carteira);
        System.out.println("Saldo adicionado com sucesso.");
    }

    private static void exibirSaldo() {
        if (carteira == null) {
            System.out.println("Nenhum usuário logado. Cadastre ou consulte um usuário primeiro.");
            return;
        }
        System.out.println("Saldo atual: R$" + carteira.getSaldo());
    }

    private static void comprarCriptoativo() {
        if (carteira == null) {
            System.out.println("Nenhum usuário logado. Cadastre ou consulte um usuário primeiro.");
            return;
        }
        System.out.print("Informe o nome do criptoativo: ");
        String nomeCripto = leitor.next();
        System.out.print("Informe o valor atual do criptoativo: ");
        double valorCripto = leitor.nextDouble();
        System.out.print("Informe a quantidade a ser comprada: ");
        double quantidadeCompra = leitor.nextDouble();
        System.out.print("Informe um ID para o ativo: ");
        String idAtivo = leitor.next();

        org.example.models.CriptoAtivo criptoAtivoCompra = new org.example.models.CriptoAtivo(idAtivo, nomeCripto, valorCripto, 0);
        criptoAtivoDAO.inserirCriptoAtivo(criptoAtivoCompra);
        carteira.compraCriptoAtivo(criptoAtivoCompra, quantidadeCompra);
        carteiraDAO.atualizarCarteira(carteira);

        // Registrar a transação
        org.example.models.Transacao transacao = new org.example.models.Transacao(usuario, criptoAtivoCompra, quantidadeCompra, org.example.enums.TipoTransacao.compra);
        transacaoDAO.inserirTransacao(transacao);

        System.out.println("Compra realizada com sucesso.");
    }

    private static void venderCriptoativo() {
        if (carteira == null) {
            System.out.println("Nenhum usuário logado. Cadastre ou consulte um usuário primeiro.");
            return;
        }
        System.out.print("Informe o nome do criptoativo: ");
        String nomeCriptoVenda = leitor.next();
        boolean ativoEncontrado = false;
        org.example.models.CriptoAtivo ativo = null;

        for (org.example.models.CriptoAtivo cripto : carteira.getCriptoAtivos().keySet()) {
            if (cripto.getNome().equalsIgnoreCase(nomeCriptoVenda)) {
                ativo = cripto;
                ativoEncontrado = true;
                break;
            }
        }

        if (!ativoEncontrado) {
            System.out.println("Ativo não encontrado.");
            return;
        }

        System.out.print("Informe a quantidade a ser vendida: ");
        double quantidadeVenda = leitor.nextDouble();
        carteira.vendeCriptoAtivo(ativo, quantidadeVenda);
        carteiraDAO.atualizarCarteira(carteira);

        // Registrar a transação
        org.example.models.Transacao transacao = new org.example.models.Transacao(usuario, ativo, quantidadeVenda, TipoTransacao.venda);
        transacaoDAO.inserirTransacao(transacao);

        System.out.println("Venda realizada com sucesso.");
    }

    private static void excluirUsuario() {
        System.out.print("Digite o e-mail do usuário que deseja excluir: ");
        String email = leitor.nextLine();
        usuarioDAO.excluirUsuario(email);
    }

    private static void listarUsuarios() {
        List<org.example.models.Usuario> usuarios = usuarioDAO.listarUsuarios();
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário encontrado.");
        } else {
            System.out.println("\n=== Lista de Usuários ===");
            for (org.example.models.Usuario u : usuarios) {
                System.out.println("Nome: " + u.getNome() + ", Email: " + u.getEmail() + ", CPF: " + u.getCpf());
            }
        }
    }

    public static ArrayList<org.example.models.Transacao> getTransacoes() {
        return transacoes;
    }

    public static void setTransacoes(ArrayList<org.example.models.Transacao> transacoes) {
        Main.transacoes = transacoes;
    }
}

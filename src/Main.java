import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    private static Scanner leitor = new Scanner(System.in);
    private static Usuario usuario = new Usuario();
    private static Carteira carteira = new Carteira(usuario);
    private static ArrayList<CriptoAtivo> ativos = new ArrayList<CriptoAtivo>();
    private static ArrayList<Transacao> transacaos = new ArrayList<Transacao>();

    public static void main(String[] args) {
        int op;

        do {
            System.out.println("Escolha:");
            System.out.println("1 - Cadastrar/Alterar Usuário");
            System.out.println("2 - Exibir informações do Usuário");
            System.out.println("3 - Adicionar Saldo");
            System.out.println("4 - Exibir Saldo");
            System.out.println("5 - Comprar Criptoativo");
            System.out.println("6 - Vender Criptoativo");
            System.out.println("7 - Fechar o programa");
            op = leitor.nextInt();
            switch(op) {
                case 1:
                    cadastrarAlterarUsuario();
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
                    System.out.println("Finalizando o sistema!");
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        } while (op != 7);

        leitor.close();
    }

    private static void cadastrarAlterarUsuario() {
        try {
            System.out.println("Informe o nome do usuário:");
            usuario.setNome(leitor.next());
            System.out.println("Informe o e-mail do usuário:");
            usuario.setEmail(leitor.next());
            System.out.println("Informe a senha do usuário");
            usuario.setSenha(leitor.next());
            System.out.println("Informe o CPF do usuário");
            usuario.setCpf(leitor.next());
        } catch(InputMismatchException erro) {
            System.out.println("Informação inserida não é válida.");
        } catch (NullPointerException erro) {
            System.out.println("Ponteiro não foi capaz de alocar atributo ao objeto.");
        }
    }

    private static void exibirInformacoesUsuario() {
        if (usuario.getNome() != null)
            System.out.println("O nome do usuário é: " + usuario.getNome() + " . Seu endereço de e-mail: " + usuario.getEmail() + " . CPF de número: " + usuario.getCpf() + ".");
        else
            System.out.println("Não há informações cadastradas.");
    }

    private static void adicionarSaldo() {
        System.out.println("Informe o valor a ser adicionado ao saldo:");
        double valor = leitor.nextDouble();
        carteira.adicionaSaldo(valor);
        System.out.println("Saldo adicionado com sucesso.");
    }

    private static void exibirSaldo() {
        System.out.println("O saldo atual é: " + carteira.getSaldo());
    }

    private static void comprarCriptoativo() {
        System.out.println("Informe o nome do criptoativo:");
        String nomeCripto = leitor.next();
        System.out.println("Informe o valor atual do criptoativo:");
        double valorCripto = leitor.nextDouble();
        System.out.println("Informe a quantidade a ser comprada:");
        double quantidadeCompra = leitor.nextDouble();
        System.out.println(("Informe um Id para o ativo"));
        String idAtivo = leitor.next();
        CriptoAtivo criptoAtivoCompra = new CriptoAtivo(idAtivo, nomeCripto, valorCripto, 0);
        Transacao transacao =   new Transacao(usuario,criptoAtivoCompra, quantidadeCompra, TipoTransacao.compra);
        ativos.add(criptoAtivoCompra);
        carteira.compraCriptoAtivo(criptoAtivoCompra, quantidadeCompra);
        System.out.println("Compra realizada com sucesso.");
    }

    private static void venderCriptoativo() {
        System.out.println("Informe o nome do criptoativo:");
        String nomeCriptoVenda = leitor.next();
        boolean ativoEncontrado = false;
        CriptoAtivo ativo = null;
        for (int i = 0; i < ativos.size(); i++) {
            ativo = ativos.get(i);
            if (ativo.getNome().equals(nomeCriptoVenda)) {
                System.out.println("Ativo encontrado.");
                ativoEncontrado = true;
                break;
            }
        }
        if (!ativoEncontrado) {
            System.out.println("Ativo não encontrado");
            return;
        }
        System.out.println("Informe a quantidade a ser vendida:");
        double quantidadeVenda = leitor.nextDouble();
        carteira.vendeCriptoAtivo(ativo, quantidadeVenda);
        System.out.println("Venda realizada com sucesso.");
    }
}
package org.example.dao;

import org.example.config.ConexaoOracle;
import org.example.models.Carteira;
import org.example.models.CriptoAtivo;
import org.example.models.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CarteiraDAO {

    public void inserirCarteira(Carteira carteira) {
        String sql = "INSERT INTO Carteira (idUsuario, saldo) VALUES (?, ?)";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, carteira.getUsuario().getIdUsuario());
            stmt.setDouble(2, carteira.getSaldo());

            stmt.executeUpdate();

            // Recupera o ID gerado
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    carteira.setIdCarteira(generatedKeys.getInt(1));
                }
            }

            System.out.println("Carteira inserida com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir carteira: " + e.getMessage());
        }
    }

    public Carteira buscarCarteiraPorId(int idCarteira) {
        String sql = "SELECT * FROM Carteira WHERE idCarteira = ?";
        Carteira carteiraEncontrada = null;

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idCarteira);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Primeiro, buscar o usuário associado
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                Usuario usuario = usuarioDAO.buscarUsuarioPorId(rs.getInt("idUsuario"));

                if (usuario != null) {
                    carteiraEncontrada = new Carteira(usuario);
                    carteiraEncontrada.setIdCarteira(rs.getInt("idCarteira"));
                    carteiraEncontrada.setSaldo(rs.getDouble("saldo"));

                    // Carregar criptoativos da carteira
                    carregarCriptoAtivosDaCarteira(carteiraEncontrada);
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar carteira: " + e.getMessage());
        }

        return carteiraEncontrada;
    }

    private void carregarCriptoAtivosDaCarteira(Carteira carteira) {
        String sql = "SELECT ca.*, cc.quantidade FROM CarteiraCriptoAtivos cc " +
                "JOIN CriptoAtivo ca ON cc.idAtivo = ca.idAtivo " +
                "WHERE cc.idCarteira = ?";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, carteira.getIdCarteira());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                CriptoAtivo criptoAtivo = new CriptoAtivo(
                        rs.getString("idAtivo"),
                        rs.getString("nome"),
                        rs.getDouble("valorAtual"),
                        rs.getDouble("variacaoDiaria")
                );
                carteira.getCriptoAtivos().put(criptoAtivo, rs.getDouble("quantidade"));
            }

        } catch (SQLException e) {
            System.err.println("Erro ao carregar criptoativos da carteira: " + e.getMessage());
        }
    }

    public void atualizarCarteira(Carteira carteira) {
        String sql = "UPDATE Carteira SET saldo = ? WHERE idCarteira = ?";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setDouble(1, carteira.getSaldo());
            stmt.setInt(2, carteira.getIdCarteira());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Carteira atualizada com sucesso!");
            } else {
                System.out.println("Nenhuma carteira encontrada com esse ID.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar carteira: " + e.getMessage());
        }
    }

    public void excluirCarteira(int idCarteira) {
        String sql = "DELETE FROM Carteira WHERE idCarteira = ?";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idCarteira);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Carteira excluída com sucesso!");
            } else {
                System.out.println("Nenhuma carteira encontrada com esse ID.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao excluir carteira: " + e.getMessage());
        }
    }

    public List<Carteira> listarCarteiras() {
        List<Carteira> carteiras = new ArrayList<>();
        String sql = "SELECT * FROM Carteira";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                Usuario usuario = usuarioDAO.buscarUsuarioPorId(rs.getInt("idUsuario"));

                if (usuario != null) {
                    Carteira carteira = new Carteira(usuario);
                    carteira.setIdCarteira(rs.getInt("idCarteira"));
                    carteira.setSaldo(rs.getDouble("saldo"));

                    // Carregar criptoativos da carteira
                    carregarCriptoAtivosDaCarteira(carteira);

                    carteiras.add(carteira);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar carteiras: " + e.getMessage());
        }
        return carteiras;
    }
}
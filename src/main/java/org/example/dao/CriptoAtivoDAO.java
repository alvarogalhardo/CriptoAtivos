package org.example.dao;

import org.example.config.ConexaoOracle;
import org.example.models.CriptoAtivo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CriptoAtivoDAO {

    public void inserirCriptoAtivo(CriptoAtivo criptoAtivo) {
        String sql = "INSERT INTO CriptoAtivo (idAtivo, nome, valorAtual, variacaoDiaria) VALUES (?, ?, ?, ?)";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, criptoAtivo.getIdAtivo());
            stmt.setString(2, criptoAtivo.getNome());
            stmt.setDouble(3, criptoAtivo.getValorAtual());
            stmt.setDouble(4, criptoAtivo.getVariacaoDiaria());

            stmt.executeUpdate();
            System.out.println("Criptoativo inserido com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir criptoativo: " + e.getMessage());
        }
    }

    public CriptoAtivo buscarCriptoAtivoPorId(String idAtivo) {
        String sql = "SELECT * FROM CriptoAtivo WHERE idAtivo = ?";
        CriptoAtivo criptoAtivoEncontrado = null;

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, idAtivo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                criptoAtivoEncontrado = new CriptoAtivo(
                        rs.getString("idAtivo"),
                        rs.getString("nome"),
                        rs.getDouble("valorAtual"),
                        rs.getDouble("variacaoDiaria")
                );
                System.out.println("Criptoativo encontrado: " + criptoAtivoEncontrado.getNome());
            } else {
                System.out.println("Nenhum criptoativo encontrado com esse ID.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar criptoativo: " + e.getMessage());
        }

        return criptoAtivoEncontrado;
    }

    public void atualizarCriptoAtivo(CriptoAtivo criptoAtivo) {
        String sql = "UPDATE CriptoAtivo SET nome = ?, valorAtual = ?, variacaoDiaria = ? WHERE idAtivo = ?";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, criptoAtivo.getNome());
            stmt.setDouble(2, criptoAtivo.getValorAtual());
            stmt.setDouble(3, criptoAtivo.getVariacaoDiaria());
            stmt.setString(4, criptoAtivo.getIdAtivo());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Criptoativo atualizado com sucesso!");
            } else {
                System.out.println("Nenhum criptoativo encontrado com esse ID.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar criptoativo: " + e.getMessage());
        }
    }

    public void excluirCriptoAtivo(String idAtivo) {
        String sql = "DELETE FROM CriptoAtivo WHERE idAtivo = ?";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, idAtivo);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Criptoativo excluído com sucesso!");
            } else {
                System.out.println("Nenhum criptoativo encontrado com esse ID.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao excluir criptoativo: " + e.getMessage());
        }
    }

    public List<CriptoAtivo> listarCriptoAtivos() {
        List<CriptoAtivo> criptoAtivos = new ArrayList<>();
        String sql = "SELECT * FROM CriptoAtivo";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                CriptoAtivo criptoAtivo = new CriptoAtivo(
                        rs.getString("idAtivo"),
                        rs.getString("nome"),
                        rs.getDouble("valorAtual"),
                        rs.getDouble("variacaoDiaria")
                );
                criptoAtivos.add(criptoAtivo);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar criptoativos: " + e.getMessage());
        }
        return criptoAtivos;
    }
}
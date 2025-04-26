package org.example.dao;

import org.example.config.ConexaoOracle;
import org.example.enums.TipoTransacao;
import org.example.models.CriptoAtivo;
import org.example.models.Transacao;
import org.example.models.Usuario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransacaoDAO {

    public void inserirTransacao(Transacao transacao) {
        String sql = "INSERT INTO Transacao (idUsuario, idAtivo, quantidade, tipo, data) VALUES (?, ?, ?, ?, ?)";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, transacao.getUsuario().getIdUsuario());
            stmt.setString(2, transacao.getCriptoAtivo().getIdAtivo());
            stmt.setDouble(3, transacao.getQuantidade());
            stmt.setString(4, transacao.getTipo().name());
            stmt.setTimestamp(5, new Timestamp(transacao.getData().getTime()));

            stmt.executeUpdate();
            System.out.println("Transação inserida com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir transação: " + e.getMessage());
        }
    }

    public Transacao buscarTransacaoPorId(int idTransacao) {
        String sql = "SELECT * FROM Transacao WHERE idTransacao = ?";
        Transacao transacaoEncontrada = null;

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idTransacao);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Buscar usuário e criptoativo associados
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                CriptoAtivoDAO criptoAtivoDAO = new CriptoAtivoDAO();

                Usuario usuario = usuarioDAO.buscarUsuarioPorId(rs.getInt("idUsuario"));
                CriptoAtivo criptoAtivo = criptoAtivoDAO.buscarCriptoAtivoPorId(rs.getString("idAtivo"));

                if (usuario != null && criptoAtivo != null) {
                    transacaoEncontrada = new Transacao(
                            usuario,
                            criptoAtivo,
                            rs.getDouble("quantidade"),
                            TipoTransacao.valueOf(rs.getString("tipo"))
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar transação: " + e.getMessage());
        }

        return transacaoEncontrada;
    }

    public List<Transacao> listarTransacoesPorUsuario(int idUsuario) {
        List<Transacao> transacoes = new ArrayList<>();
        String sql = "SELECT * FROM Transacao WHERE idUsuario = ? ORDER BY data DESC";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                CriptoAtivoDAO criptoAtivoDAO = new CriptoAtivoDAO();

                Usuario usuario = usuarioDAO.buscarUsuarioPorId(rs.getInt("idUsuario"));
                CriptoAtivo criptoAtivo = criptoAtivoDAO.buscarCriptoAtivoPorId(rs.getString("idAtivo"));

                if (usuario != null && criptoAtivo != null) {
                    Transacao transacao = new Transacao(
                            usuario,
                            criptoAtivo,
                            rs.getDouble("quantidade"),
                            TipoTransacao.valueOf(rs.getString("tipo"))
                    );
                    transacao.setData(new Date(rs.getTimestamp("data").getTime()));
                    transacoes.add(transacao);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar transações: " + e.getMessage());
        }
        return transacoes;
    }

    public List<Transacao> listarTransacoesPorCriptoAtivo(String idAtivo) {
        List<Transacao> transacoes = new ArrayList<>();
        String sql = "SELECT * FROM Transacao WHERE idAtivo = ? ORDER BY data DESC";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, idAtivo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                CriptoAtivoDAO criptoAtivoDAO = new CriptoAtivoDAO();

                Usuario usuario = usuarioDAO.buscarUsuarioPorId(rs.getInt("idUsuario"));
                CriptoAtivo criptoAtivo = criptoAtivoDAO.buscarCriptoAtivoPorId(rs.getString("idAtivo"));

                if (usuario != null && criptoAtivo != null) {
                    Transacao transacao = new Transacao(
                            usuario,
                            criptoAtivo,
                            rs.getDouble("quantidade"),
                            TipoTransacao.valueOf(rs.getString("tipo"))
                    );
                    transacao.setData(new Date(rs.getTimestamp("data").getTime()));
                    transacoes.add(transacao);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar transações: " + e.getMessage());
        }
        return transacoes;
    }
}
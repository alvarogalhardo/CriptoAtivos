package org.example.dao;

import org.example.config.ConexaoOracle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.example.models.Carteira;
import org.example.models.Usuario;

public class UsuarioDAO {

    public void inserirUsuario(Usuario usuario) {

    }

    public Usuario buscarUsuarioPorId(int idUsuario) {
        String sql = "SELECT * FROM Usuario WHERE idUsuario = ?";
        Usuario usuarioEncontrado = null;

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                usuarioEncontrado = new Usuario(
                        rs.getInt("idUsuario"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha"),
                        rs.getInt("autenticacao2FA") == 1,
                        rs.getString("cpf")
                );
                System.out.println("Usuário encontrado: " + usuarioEncontrado.getNome());
            } else {
                System.out.println("Nenhum usuário encontrado com esse ID.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }

        return usuarioEncontrado;
    }

    public Usuario buscarUsuarioPorEmail(String email) {
        String sql = "SELECT * FROM Usuario WHERE email = ?";
        Usuario usuarioEncontrado = null;

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                usuarioEncontrado = new Usuario(
                        rs.getInt("idUsuario"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha"),
                        rs.getInt("autenticacao2FA") == 1,
                        rs.getString("cpf")
                );
                System.out.println("Usuário encontrado: " + usuarioEncontrado.getNome());
            } else {
                System.out.println("Nenhum usuário encontrado com esse e-mail.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }

        return usuarioEncontrado;
    }

    public void excluirUsuario(String email) {
        String sql = "DELETE FROM Usuario WHERE email = ?";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Usuário excluído com sucesso!");
            } else {
                System.out.println("Nenhum usuário encontrado com esse e-mail.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao excluir usuário: " + e.getMessage());
        }
    }

    public List<Usuario> listarUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM Usuario";

        try (Connection conexao = ConexaoOracle.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Usuario usuario = new Usuario(
                        rs.getInt("idUsuario"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha"),
                        rs.getInt("autenticacao2FA") == 1,
                        rs.getString("cpf")
                );
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar usuários: " + e.getMessage());
        }
        return usuarios;
    }

}

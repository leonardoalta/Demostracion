package connectionFramework.dao;

import connectionFramework.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonaDao {

    public String create(String nombre, int edad) throws SQLException {
        String sql = "INSERT INTO persona(nombre, edad) VALUES (?, ?)";

        try (Connection c = ConnectionPool.get().acquire();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setInt(2, edad);

            int rows = ps.executeUpdate();
            return rows > 0 ? "OK|Registro insertado" : "ERROR|No se insertó el registro";
        }
    }

    public String read(int id) throws SQLException {
        String sql = "SELECT id, nombre, edad FROM persona WHERE id = ?";

        try (Connection c = ConnectionPool.get().acquire();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "OK|" + rs.getInt("id") + "|" + rs.getString("nombre") + "|" + rs.getInt("edad");
                }
                return "ERROR|No encontrado";
            }
        }
    }

    public String update(int id, String nombre, int edad) throws SQLException {
        String sql = "UPDATE persona SET nombre = ?, edad = ? WHERE id = ?";

        try (Connection c = ConnectionPool.get().acquire();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setInt(2, edad);
            ps.setInt(3, id);

            int rows = ps.executeUpdate();
            return rows > 0 ? "OK|Registro actualizado" : "ERROR|No encontrado";
        }
    }

    public String delete(int id) throws SQLException {
        String sql = "DELETE FROM persona WHERE id = ?";

        try (Connection c = ConnectionPool.get().acquire();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            return rows > 0 ? "OK|Registro eliminado" : "ERROR|No encontrado";
        }
    }

    public String list() throws SQLException {
        String sql = "SELECT id, nombre, edad FROM persona ORDER BY id";

        StringBuilder sb = new StringBuilder("OK|");

        try (Connection c = ConnectionPool.get().acquire();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    sb.append(";");
                }
                sb.append(rs.getInt("id"))
                        .append(",")
                        .append(rs.getString("nombre"))
                        .append(",")
                        .append(rs.getInt("edad"));
                first = false;
            }
        }

        return sb.toString();
    }
}
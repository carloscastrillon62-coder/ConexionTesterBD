package conexion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) de la tabla productos.
 *
 * Reúne en una sola clase todas las operaciones CRUD con la base de datos:
 *  - Create: crear()
 *  - Read:   listar(), buscar(), listarCategorias()
 *  - Update: actualizar()
 *  - Delete: eliminar()
 *
 * Así la ventana (GestionProductos) no escribe SQL: solo llama a estos métodos.
 * Cada método abre su propia conexión con try-with-resources, de modo que la
 * conexión y las sentencias se cierran siempre, aunque ocurra un error.
 * Todas las consultas usan PreparedStatement con parámetros (?), lo que evita
 * errores con comillas y la inyección de SQL.
 */
public class ProductoDAO {

    private static final String SELECT_PRODUCTOS =
            "SELECT p.id, p.codigo, p.nombre, p.id_categoria, c.nombre AS categoria, p.precio, p.cantidad "
            + "FROM productos p JOIN categorias c ON c.id = p.id_categoria ";

    private final String url;

    /** @param url URL JDBC de la base de datos, por ejemplo jdbc:sqlite:inventario.db */
    public ProductoDAO(String url) {
        this.url = url;
    }

    /** Abre una conexión y activa las llaves foráneas (en SQLite vienen apagadas). */
    private Connection conectar() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conexion;
    }

    // ------------------------------------------------------------------
    // Estructura de la base de datos
    // ------------------------------------------------------------------

    /** Indica si ya existen las tablas categorias y productos. */
    public boolean existenTablas() throws SQLException {
        try (Connection conexion = conectar()) {
            DatabaseMetaData datos = conexion.getMetaData();
            return existeTabla(datos, "categorias") && existeTabla(datos, "productos");
        }
    }

    private static boolean existeTabla(DatabaseMetaData datos, String tabla) throws SQLException {
        try (ResultSet rs = datos.getTables(null, null, tabla, null)) {
            return rs.next();
        }
    }

    /**
     * Ejecuta un archivo .sql: quita los comentarios (--) y ejecuta cada
     * sentencia separada por punto y coma. Se usa para crear y poblar la base
     * de datos con los scripts de la carpeta sql.
     *
     * @return número de sentencias ejecutadas
     */
    public int ejecutarScript(Path archivo) throws IOException, SQLException {
        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            int comentario = linea.indexOf("--");
            sinComentarios.append(comentario >= 0 ? linea.substring(0, comentario) : linea).append('\n');
        }
        int ejecutadas = 0;
        try (Connection conexion = conectar(); Statement st = conexion.createStatement()) {
            for (String sentencia : sinComentarios.toString().split(";")) {
                if (!sentencia.isBlank()) {
                    st.execute(sentencia.trim());
                    ejecutadas++;
                }
            }
        }
        return ejecutadas;
    }

    // ------------------------------------------------------------------
    // READ (consultar)
    // ------------------------------------------------------------------

    /** Devuelve las categorías ordenadas por nombre, para el JComboBox. */
    public List<Categoria> listarCategorias() throws SQLException {
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conexion = conectar();
             PreparedStatement ps = conexion.prepareStatement("SELECT id, nombre FROM categorias ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categorias.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        }
        return categorias;
    }

    /** Devuelve todos los productos ordenados por código. */
    public List<Producto> listar() throws SQLException {
        return buscar("");
    }

    /** Busca productos cuyo código, nombre o categoría contengan el texto dado. */
    public List<Producto> buscar(String texto) throws SQLException {
        String sql = SELECT_PRODUCTOS
                + "WHERE p.codigo LIKE ? OR p.nombre LIKE ? OR c.nombre LIKE ? ORDER BY p.codigo";
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        List<Producto> productos = new ArrayList<>();
        try (Connection conexion = conectar(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(new Producto(
                            rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"),
                            rs.getInt("id_categoria"), rs.getString("categoria"),
                            rs.getDouble("precio"), rs.getInt("cantidad")));
                }
            }
        }
        return productos;
    }

    // ------------------------------------------------------------------
    // CREATE (crear)
    // ------------------------------------------------------------------

    /** Inserta un producto nuevo. */
    public void crear(Producto p) throws SQLException {
        validar(p);
        String sql = "INSERT INTO productos (codigo, nombre, id_categoria, precio, cantidad) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexion = conectar(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, p.codigo().trim());
            ps.setString(2, p.nombre().trim());
            ps.setInt(3, p.idCategoria());
            ps.setDouble(4, p.precio());
            ps.setInt(5, p.cantidad());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // UPDATE (actualizar)
    // ------------------------------------------------------------------

    /** Actualiza los datos del producto con el id dado. @return true si se actualizó */
    public boolean actualizar(Producto p) throws SQLException {
        validar(p);
        String sql = "UPDATE productos SET codigo = ?, nombre = ?, id_categoria = ?, precio = ?, cantidad = ? WHERE id = ?";
        try (Connection conexion = conectar(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, p.codigo().trim());
            ps.setString(2, p.nombre().trim());
            ps.setInt(3, p.idCategoria());
            ps.setDouble(4, p.precio());
            ps.setInt(5, p.cantidad());
            ps.setInt(6, p.id());
            return ps.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------------
    // DELETE (eliminar)
    // ------------------------------------------------------------------

    /** Elimina el producto con el id dado. @return true si se eliminó */
    public boolean eliminar(int id) throws SQLException {
        try (Connection conexion = conectar();
             PreparedStatement ps = conexion.prepareStatement("DELETE FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------------
    // Validación de datos antes de enviarlos a la base de datos
    // ------------------------------------------------------------------

    /** Lanza IllegalArgumentException con un mensaje claro si algún dato no es válido. */
    static void validar(Producto p) {
        if (p.codigo() == null || p.codigo().isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio.");
        }
        if (p.nombre() == null || p.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (p.idCategoria() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una categoría.");
        }
        if (p.precio() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (p.cantidad() < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
    }

    /** Traduce los errores más comunes de la base de datos a un mensaje claro para el usuario. */
    static String mensajeAmigable(SQLException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("UNIQUE") && m.contains("codigo")) {
            return "Ya existe un producto con ese código.";
        }
        if (m.contains("FOREIGN KEY")) {
            return "La categoría seleccionada no existe.";
        }
        if (m.contains("no such table")) {
            return "La base de datos no tiene las tablas. Use el botón \"Crear BD con scripts\".";
        }
        return "Error de base de datos: " + m;
    }
}

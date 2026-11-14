package conexion;

/**
 * Producto del inventario (una fila de la tabla productos).
 *
 * Es un record: un objeto inmutable que solo guarda datos. Java genera
 * automáticamente el constructor y los métodos de acceso (codigo(), nombre(), ...).
 *
 * @param id          llave primaria (0 si el producto todavía no se ha guardado)
 * @param codigo      código único del producto, por ejemplo CEL-001
 * @param nombre      nombre o descripción del producto
 * @param idCategoria llave foránea hacia la tabla categorias
 * @param categoria   nombre de la categoría (se obtiene con un JOIN al consultar)
 * @param precio      precio unitario en pesos
 * @param cantidad    unidades disponibles en inventario
 */
public record Producto(int id, String codigo, String nombre, int idCategoria,
                       String categoria, double precio, int cantidad) {

    /** Crea un producto nuevo, sin id, para insertarlo en la base de datos. */
    public static Producto nuevo(String codigo, String nombre, int idCategoria, double precio, int cantidad) {
        return new Producto(0, codigo, nombre, idCategoria, null, precio, cantidad);
    }
}

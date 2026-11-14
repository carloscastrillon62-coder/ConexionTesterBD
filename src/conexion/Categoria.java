package conexion;

/**
 * Categoría de productos (una fila de la tabla categorias).
 *
 * @param id     llave primaria
 * @param nombre nombre de la categoría, por ejemplo Celulares
 */
public record Categoria(int id, String nombre) {

    /** El JComboBox muestra lo que devuelve toString(): solo el nombre. */
    @Override
    public String toString() {
        return nombre;
    }
}

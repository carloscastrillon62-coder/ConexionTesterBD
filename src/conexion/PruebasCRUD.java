package conexion;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas de consola (sin frameworks) del CRUD de productos.
 * Crean una base de datos temporal con los scripts de la carpeta sql y
 * comprueban cada operación: crear, consultar, actualizar y eliminar.
 *
 * Uso: java -cp "out;lib/sqlite-jdbc-3.42.0.1.jar" conexion.PruebasCRUD   (Windows)
 */
public class PruebasCRUD {

    /** Una prueba que puede lanzar excepciones (a diferencia de Supplier). */
    @FunctionalInterface
    interface Prueba {
        boolean ejecutar() throws Exception;
    }

    private static final String ARCHIVO = "prueba_crud.db";

    public static void main(String[] args) throws Exception {
        new File(ARCHIVO).delete();
        ProductoDAO dao = new ProductoDAO("jdbc:sqlite:" + ARCHIVO);
        List<Boolean> resultados = new ArrayList<>();

        resultados.add(prueba("1. Los scripts crean 5 categorias y 15 productos", () -> {
            dao.ejecutarScript(Path.of("sql", "01_crear_tablas.sql"));
            dao.ejecutarScript(Path.of("sql", "02_poblar_datos.sql"));
            return dao.existenTablas() && dao.listarCategorias().size() == 5 && dao.listar().size() == 15;
        }));
        resultados.add(prueba("2. READ: buscar 'JBL' encuentra 2 productos", () ->
                dao.buscar("JBL").size() == 2));
        resultados.add(prueba("3. CREATE: guardar un producto nuevo", () -> {
            dao.crear(Producto.nuevo("ACC-004", "Cable HDMI 2 m", 4, 29900, 30));
            List<Producto> encontrados = dao.buscar("ACC-004");
            return dao.listar().size() == 16 && encontrados.size() == 1
                    && encontrados.get(0).categoria().equals("Accesorios");
        }));
        resultados.add(prueba("4. CREATE: el codigo repetido se rechaza", () -> {
            try {
                dao.crear(Producto.nuevo("ACC-004", "Otro cable", 4, 10000, 1));
                return false;
            } catch (SQLException e) {
                return ProductoDAO.mensajeAmigable(e).contains("Ya existe");
            }
        }));
        resultados.add(prueba("5. UPDATE: cambiar precio y cantidad", () -> {
            Producto p = dao.buscar("ACC-004").get(0);
            dao.actualizar(new Producto(p.id(), p.codigo(), p.nombre(), p.idCategoria(), null, 34900, 12));
            Producto actualizado = dao.buscar("ACC-004").get(0);
            return actualizado.precio() == 34900 && actualizado.cantidad() == 12;
        }));
        resultados.add(prueba("6. DELETE: eliminar el producto", () -> {
            Producto p = dao.buscar("ACC-004").get(0);
            return dao.eliminar(p.id()) && dao.buscar("ACC-004").isEmpty() && dao.listar().size() == 15;
        }));
        resultados.add(prueba("7. Validacion: precio negativo no se guarda", () -> {
            try {
                dao.crear(Producto.nuevo("X-1", "Prueba", 1, -5, 1));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("precio");
            }
        }));
        resultados.add(prueba("8. El precio se lee como 1599900, 1.599.900 o $ 1.599.900", () ->
                GestionProductos.convertirNumero("1599900", "precio") == 1599900
                        && GestionProductos.convertirNumero("1.599.900", "precio") == 1599900
                        && GestionProductos.convertirNumero("$ 1.599.900", "precio") == 1599900));

        new File(ARCHIVO).delete();
        long aprobadas = resultados.stream().filter(Boolean::booleanValue).count();
        System.out.printf("%nPruebas CRUD aprobadas: %d de %d%n", aprobadas, resultados.size());
    }

    private static boolean prueba(String nombre, Prueba prueba) {
        boolean ok;
        try {
            ok = prueba.ejecutar();
        } catch (Exception e) {
            System.out.println("    Error: " + e.getMessage());
            ok = false;
        }
        System.out.printf("[%s] %s%n", ok ? "OK" : "FALLA", nombre);
        return ok;
    }
}

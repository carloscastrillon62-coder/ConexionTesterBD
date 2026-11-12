package conexion;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

/**
 * Pruebas de consola (sin frameworks) de la lógica de ConexionTester.
 * Cubren la conexión exitosa y cada tipo de error que la aplicación debe manejar.
 *
 * Uso: java -cp "out;lib/sqlite-jdbc-3.42.0.1.jar" conexion.PruebasConexion   (Windows)
 */
public class PruebasConexion {

    public static void main(String[] args) {
        char[] contrasena = "secreta".toCharArray();

        List<Boolean> resultados = List.of(
                prueba("1. SQLite valida -> conexion exitosa", () -> {
                    ConexionTester.Resultado r = ConexionTester.probarConexion(
                            "jdbc:sqlite:prueba_inventario.db", "", new char[0]);
                    return r.exitoso() && r.mensaje().startsWith("CONEXIÓN EXITOSA");
                }),
                prueba("2. MySQL sin driver -> ClassNotFoundException", () -> {
                    ConexionTester.Resultado r = ConexionTester.probarConexion(
                            "jdbc:mysql://localhost:3306/inventario", "root", new char[0]);
                    return !r.exitoso() && r.mensaje().contains("DRIVER NO ENCONTRADO");
                }),
                prueba("3. SQLite en carpeta inexistente -> SQLException", () -> {
                    ConexionTester.Resultado r = ConexionTester.probarConexion(
                            "jdbc:sqlite:/carpeta/que/no/existe/inventario.db", "", new char[0]);
                    return !r.exitoso() && r.mensaje().contains("Mensaje técnico");
                }),
                prueba("4. Motor sin driver registrado -> SQLException", () -> {
                    ConexionTester.Resultado r = ConexionTester.probarConexion(
                            "jdbc:oracle:thin:@localhost:1521:xe", "admin", new char[0]);
                    return !r.exitoso() && r.mensaje().contains("No suitable driver");
                }),
                prueba("5. URL vacia -> mensaje de validacion", () -> {
                    ConexionTester.Resultado r = ConexionTester.probarConexion("   ", "", new char[0]);
                    return !r.exitoso() && r.mensaje().contains("Debe escribir la URL");
                }),
                prueba("6. La contrasena se borra de memoria al terminar", () -> {
                    ConexionTester.probarConexion("jdbc:sqlite:prueba_inventario.db", "", contrasena);
                    return new String(contrasena).chars().allMatch(c -> c == 0);
                }));

        new File("prueba_inventario.db").delete();
        long aprobadas = resultados.stream().filter(Boolean::booleanValue).count();
        System.out.printf("%nPruebas aprobadas: %d de %d%n", aprobadas, resultados.size());
    }

    private static boolean prueba(String nombre, Supplier<Boolean> condicion) {
        boolean ok = condicion.get();
        System.out.printf("[%s] %s%n", ok ? "OK" : "FALLA", nombre);
        return ok;
    }
}

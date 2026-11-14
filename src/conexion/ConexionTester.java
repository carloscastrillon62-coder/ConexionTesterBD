package conexion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * EA3 - Incorporando UI y BD.
 *
 * Ventana de escritorio (Swing) que permite ingresar la configuración de una
 * base de datos (URL, usuario y contraseña) y validar la conexión con JDBC,
 * mostrando en pantalla el resultado o el error técnico que se produzca.
 *
 * Organización de la clase:
 *  1. Atributos: componentes de la interfaz (encapsulados como private final).
 *  2. Constructor y construirPanelPrincipal(): arman la interfaz gráfica.
 *  3. actionPerformed(): evento del botón "Probar conexión" (ActionListener).
 *  4. probarConexion(): lógica JDBC con try-catch y try-with-resources.
 *  5. mostrarResultado(): presenta el mensaje en verde (éxito) o rojo (error).
 *
 * Cuando la conexión es exitosa se habilita el botón "Abrir inventario (CRUD)",
 * que abre la ventana GestionProductos con la misma URL para crear, consultar,
 * actualizar y eliminar productos de la base de datos.
 */
public final class ConexionTester extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Color VERDE = new Color(0, 128, 0);
    private static final Color ROJO = new Color(190, 0, 0);
    private static final Color GRIS = new Color(90, 90, 90);
    private static final int TIEMPO_ESPERA_SEGUNDOS = 5;

    /** Clase del driver JDBC según el prefijo de la URL. Mapa inmutable. */
    private static final Map<String, String> DRIVERS = Map.of(
            "jdbc:sqlite:", "org.sqlite.JDBC",
            "jdbc:mysql:", "com.mysql.cj.jdbc.Driver",
            "jdbc:mariadb:", "org.mariadb.jdbc.Driver",
            "jdbc:postgresql:", "org.postgresql.Driver",
            "jdbc:sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

    // ------------------------------------------------------------------
    // 1. Componentes de la interfaz (encapsulamiento: privados y finales)
    // ------------------------------------------------------------------
    private final JTextField campoUrl = new JTextField("jdbc:sqlite:inventario.db", 32);
    private final JTextField campoUsuario = new JTextField(32);
    private final JPasswordField campoContrasena = new JPasswordField(32);
    private final JButton botonProbar = new JButton("Probar conexión");
    private final JButton botonInventario = new JButton("Abrir inventario (CRUD)");
    private final JTextArea areaResultado = new JTextArea(9, 50);

    /** URL de la última conexión exitosa; es la que usa la ventana del inventario. */
    private String urlConectada;

    /** Resultado inmutable de una prueba de conexión. */
    record Resultado(boolean exitoso, String mensaje) {
        static Resultado exito(String mensaje) {
            return new Resultado(true, mensaje);
        }

        static Resultado error(String mensaje) {
            return new Resultado(false, mensaje);
        }
    }

    // ------------------------------------------------------------------
    // 2. Construcción de la interfaz gráfica
    // ------------------------------------------------------------------
    public ConexionTester() {
        super("EA3 - Probador de conexión a bases de datos (JDBC)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(construirPanelPrincipal());
        botonProbar.addActionListener(this);           // el JFrame escucha el clic del botón
        botonInventario.setEnabled(false);              // se habilita solo si la conexión es exitosa
        botonInventario.addActionListener(e -> new GestionProductos(urlConectada).setVisible(true));
        getRootPane().setDefaultButton(botonProbar);    // Enter también prueba la conexión
        pack();
        setLocationRelativeTo(null);                    // centrar en la pantalla
    }

    /** Panel principal con BorderLayout: formulario arriba y resultado en el centro. */
    private JPanel construirPanelPrincipal() {
        // Formulario: GridLayout de 3 filas x 2 columnas (etiqueta | campo)
        JPanel formulario = new JPanel(new GridLayout(3, 2, 8, 8));
        formulario.add(new JLabel("URL de conexión:"));
        formulario.add(campoUrl);
        formulario.add(new JLabel("Usuario (vacío si es SQLite):"));
        formulario.add(campoUsuario);
        formulario.add(new JLabel("Contraseña (vacía si es SQLite):"));
        formulario.add(campoContrasena);

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBoton.add(botonInventario);
        panelBoton.add(botonProbar);

        JPanel parteSuperior = new JPanel(new BorderLayout(8, 8));
        parteSuperior.add(formulario, BorderLayout.CENTER);
        parteSuperior.add(panelBoton, BorderLayout.SOUTH);

        // Área de resultados dentro de un JScrollPane
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        areaResultado.setForeground(GRIS);
        areaResultado.setText("Escriba los datos de conexión y presione \"Probar conexión\".");
        JScrollPane scroll = new JScrollPane(areaResultado);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado de la prueba"));

        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        principal.add(parteSuperior, BorderLayout.NORTH);
        principal.add(scroll, BorderLayout.CENTER);
        return principal;
    }

    // ------------------------------------------------------------------
    // 3. Evento del botón "Probar conexión" (ActionListener)
    // ------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent evento) {
        String url = campoUrl.getText();
        String usuario = campoUsuario.getText().trim();
        char[] contrasena = campoContrasena.getPassword();

        botonProbar.setEnabled(false);
        botonInventario.setEnabled(false);
        areaResultado.setForeground(GRIS);
        areaResultado.setText("Probando conexión...");

        // La conexión se intenta en un hilo aparte (SwingWorker) para que la
        // ventana no se congele mientras el servidor responde.
        new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() {
                return probarConexion(url, usuario, contrasena);
            }

            @Override
            protected void done() {
                try {
                    Resultado resultado = get();
                    mostrarResultado(resultado);
                    if (resultado.exitoso()) {
                        urlConectada = url.trim();
                        botonInventario.setEnabled(true);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    mostrarResultado(Resultado.error("La prueba fue interrumpida."));
                } catch (ExecutionException ex) {
                    mostrarResultado(Resultado.error("Error inesperado: " + ex.getCause()));
                } finally {
                    botonProbar.setEnabled(true);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // 4. Lógica de conexión JDBC
    // ------------------------------------------------------------------
    /**
     * Intenta abrir una conexión con los datos dados y la cierra de inmediato.
     *
     * @return Resultado con el mensaje de éxito o el mensaje técnico del error
     */
    static Resultado probarConexion(String url, String usuario, char[] contrasena) {
        if (url == null || url.isBlank()) {
            return Resultado.error("FALLO DE CONEXIÓN\n\n"
                    + "Debe escribir la URL de conexión (ejemplo: jdbc:sqlite:inventario.db).");
        }
        String urlLimpia = url.trim();
        long inicio = System.nanoTime();

        try {
            cargarDriver(urlLimpia);                           // puede lanzar ClassNotFoundException
            DriverManager.setLoginTimeout(TIEMPO_ESPERA_SEGUNDOS);

            // try-with-resources: la conexión se cierra sola al terminar el bloque,
            // tanto si todo sale bien como si ocurre una excepción.
            try (Connection conexion = abrirConexion(urlLimpia, usuario, contrasena)) {
                DatabaseMetaData datos = conexion.getMetaData();
                long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
                return Resultado.exito(String.format(
                        "CONEXIÓN EXITOSA%n%n"
                                + "Base de datos: %s %s%n"
                                + "Driver:        %s %s%n"
                                + "URL:           %s%n"
                                + "Tiempo:        %d ms%n%n"
                                + "La conexión se cerró correctamente (try-with-resources).",
                        datos.getDatabaseProductName(), datos.getDatabaseProductVersion(),
                        datos.getDriverName(), datos.getDriverVersion(),
                        urlLimpia, milisegundos));
            }
        } catch (ClassNotFoundException e) {
            return Resultado.error("FALLO DE CONEXIÓN: DRIVER NO ENCONTRADO\n\n"
                    + "Clase del driver: " + e.getMessage() + "\n"
                    + "Agregue el archivo .jar del driver a la carpeta lib y al classpath.");
        } catch (NoClassDefFoundError e) {
            return Resultado.error("FALLO DE CONEXIÓN: DRIVER INCOMPLETO\n\n"
                    + "El driver necesita otra librería que no está en el classpath: " + e.getMessage());
        } catch (SQLException e) {
            return Resultado.error("FALLO DE CONEXIÓN\n\n"
                    + "Mensaje técnico: " + e.getMessage() + "\n"
                    + "SQLState: " + e.getSQLState() + "   |   Código de error: " + e.getErrorCode());
        } finally {
            Arrays.fill(contrasena, '\0');                     // borrar la contraseña de memoria
        }
    }

    /** Carga la clase del driver según la URL; si el .jar no está, lanza ClassNotFoundException. */
    private static void cargarDriver(String url) throws ClassNotFoundException {
        for (Map.Entry<String, String> driver : DRIVERS.entrySet()) {
            if (url.startsWith(driver.getKey())) {
                Class.forName(driver.getValue());
                return;
            }
        }
        // URL de un motor no listado: DriverManager intentará encontrar un driver
        // y, si no hay ninguno, lanzará SQLException ("No suitable driver").
    }

    /** Abre la conexión con DriverManager (sin usuario/contraseña si están vacíos, como en SQLite). */
    private static Connection abrirConexion(String url, String usuario, char[] contrasena) throws SQLException {
        if (usuario == null || usuario.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, usuario, new String(contrasena));
    }

    // ------------------------------------------------------------------
    // 5. Presentación del resultado
    // ------------------------------------------------------------------
    /** Muestra el mensaje en verde si la conexión fue exitosa o en rojo si falló. */
    private void mostrarResultado(Resultado resultado) {
        areaResultado.setForeground(resultado.exitoso() ? VERDE : ROJO);
        areaResultado.setText(resultado.mensaje());
        areaResultado.setCaretPosition(0);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Si no se puede usar el estilo del sistema, Swing usa el estilo por defecto.
        }
        SwingUtilities.invokeLater(() -> new ConexionTester().setVisible(true));
    }
}

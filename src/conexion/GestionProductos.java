package conexion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana de gestión del inventario: CRUD de la tabla productos.
 *
 * Se abre desde ConexionTester cuando la conexión es exitosa y recibe la URL
 * de la base de datos. Toda la comunicación con la base de datos la hace
 * ProductoDAO; esta clase solo se encarga de la interfaz:
 *  - Arriba:  búsqueda y botón para crear la base de datos con los scripts.
 *  - Centro:  JTable con los productos (Read).
 *  - Abajo:   formulario y botones Nuevo, Guardar (Create), Actualizar (Update)
 *             y Eliminar (Delete), más una barra con el resumen y los mensajes.
 */
public final class GestionProductos extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color VERDE = new Color(0, 128, 0);
    private static final Color ROJO = new Color(190, 0, 0);
    private static final Path SCRIPT_CREAR = Path.of("sql", "01_crear_tablas.sql");
    private static final Path SCRIPT_POBLAR = Path.of("sql", "02_poblar_datos.sql");
    private static final NumberFormat PESOS = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    static {
        PESOS.setMaximumFractionDigits(0);
    }

    private final transient ProductoDAO dao;

    // Búsqueda
    private final JTextField campoBuscar = new JTextField(22);

    // Tabla (Read)
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] {"ID", "Código", "Nombre", "Categoría", "Precio", "Cantidad"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;                                   // la tabla solo muestra datos
        }
    };
    private final JTable tabla = new JTable(modelo);

    // Formulario
    private final JTextField campoCodigo = new JTextField(12);
    private final JTextField campoNombre = new JTextField(25);
    private final JComboBox<Categoria> comboCategoria = new JComboBox<>();
    private final JTextField campoPrecio = new JTextField(12);
    private final JTextField campoCantidad = new JTextField(12);

    // Mensajes
    private final JLabel etiquetaResumen = new JLabel(" ");
    private final JLabel etiquetaMensaje = new JLabel(" ");

    /** id del producto seleccionado en la tabla (0 = ninguno). */
    private int idSeleccionado;

    public GestionProductos(String url) {
        super("EA3 - Gestión de inventario (CRUD) - " + url);
        this.dao = new ProductoDAO(url);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);  // cierra solo esta ventana
        setContentPane(construirPanel());
        pack();
        setLocationRelativeTo(null);
        cargarDatosIniciales();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------
    private JPanel construirPanel() {
        // Arriba: búsqueda
        JButton botonBuscar = new JButton("Buscar");
        JButton botonTodos = new JButton("Mostrar todos");
        JButton botonScripts = new JButton("Crear BD con scripts");
        botonBuscar.addActionListener(this::buscar);
        campoBuscar.addActionListener(this::buscar);       // Enter en el campo también busca
        botonTodos.addActionListener(e -> {
            campoBuscar.setText("");
            recargarTabla();
        });
        botonScripts.addActionListener(this::crearBaseDatos);

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelBusqueda.add(new JLabel("Buscar (código, nombre o categoría):"));
        panelBusqueda.add(campoBuscar);
        panelBusqueda.add(botonBuscar);
        panelBusqueda.add(botonTodos);
        panelBusqueda.add(botonScripts);

        // Centro: tabla de productos
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(22);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionEnFormulario();
            }
        });
        int[] anchos = {40, 80, 280, 110, 110, 70};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(4).setCellRenderer(derecha);
        tabla.getColumnModel().getColumn(5).setCellRenderer(derecha);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Productos del inventario"));
        scrollTabla.setPreferredSize(new java.awt.Dimension(760, 330));

        // Abajo: formulario. Etiquetas a la izquierda y campos a la derecha,
        // cada columna con un GridLayout de 5 filas para que queden alineadas.
        JPanel etiquetas = new JPanel(new GridLayout(5, 1, 0, 6));
        JPanel campos = new JPanel(new GridLayout(5, 1, 0, 6));
        etiquetas.add(new JLabel("Código:"));
        campos.add(campoCodigo);
        etiquetas.add(new JLabel("Nombre:"));
        campos.add(campoNombre);
        etiquetas.add(new JLabel("Categoría:"));
        campos.add(comboCategoria);
        etiquetas.add(new JLabel("Precio (pesos):"));
        campos.add(campoPrecio);
        etiquetas.add(new JLabel("Cantidad:"));
        campos.add(campoCantidad);
        JPanel formulario = new JPanel(new BorderLayout(10, 0));
        formulario.add(etiquetas, BorderLayout.WEST);
        formulario.add(campos, BorderLayout.CENTER);
        formulario.setBorder(BorderFactory.createTitledBorder("Datos del producto"));

        JButton botonNuevo = new JButton("Nuevo");
        JButton botonGuardar = new JButton("Guardar");
        JButton botonActualizar = new JButton("Actualizar");
        JButton botonEliminar = new JButton("Eliminar");
        botonNuevo.addActionListener(e -> limpiarFormulario());
        botonGuardar.addActionListener(this::guardar);
        botonActualizar.addActionListener(this::actualizar);
        botonEliminar.addActionListener(this::eliminar);
        JPanel botones = new JPanel(new GridLayout(4, 1, 6, 6));
        botones.add(botonNuevo);
        botones.add(botonGuardar);
        botones.add(botonActualizar);
        botones.add(botonEliminar);
        JPanel panelBotones = new JPanel(new BorderLayout());
        panelBotones.add(botones, BorderLayout.NORTH);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel parteInferior = new JPanel(new BorderLayout(10, 0));
        parteInferior.add(formulario, BorderLayout.CENTER);
        parteInferior.add(panelBotones, BorderLayout.EAST);

        etiquetaMensaje.setFont(etiquetaMensaje.getFont().deriveFont(Font.BOLD));
        JPanel barraEstado = new JPanel(new GridLayout(2, 1));
        barraEstado.add(etiquetaResumen);
        barraEstado.add(etiquetaMensaje);

        JPanel sur = new JPanel(new BorderLayout(0, 6));
        sur.add(parteInferior, BorderLayout.CENTER);
        sur.add(barraEstado, BorderLayout.SOUTH);

        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        principal.add(panelBusqueda, BorderLayout.NORTH);
        principal.add(scrollTabla, BorderLayout.CENTER);
        principal.add(sur, BorderLayout.SOUTH);
        return principal;
    }

    // ------------------------------------------------------------------
    // Carga de datos (Read)
    // ------------------------------------------------------------------
    private void cargarDatosIniciales() {
        try {
            if (!dao.existenTablas()) {
                mostrarError("La base de datos está vacía. Presione \"Crear BD con scripts\" para crear las tablas y los datos.");
                return;
            }
            cargarCategorias();
            recargarTabla();
        } catch (SQLException e) {
            mostrarError(ProductoDAO.mensajeAmigable(e));
        }
    }

    private void cargarCategorias() throws SQLException {
        comboCategoria.removeAllItems();
        for (Categoria c : dao.listarCategorias()) {
            comboCategoria.addItem(c);
        }
    }

    private void recargarTabla() {
        mostrarProductos(campoBuscar.getText());
    }

    private void buscar(ActionEvent e) {
        mostrarProductos(campoBuscar.getText());
    }

    /** Consulta los productos que coinciden con el texto y los muestra en la tabla. */
    private void mostrarProductos(String texto) {
        try {
            List<Producto> productos = dao.buscar(texto);
            modelo.setRowCount(0);
            int unidades = 0;
            double valorTotal = 0;
            for (Producto p : productos) {
                modelo.addRow(new Object[] {p.id(), p.codigo(), p.nombre(), p.categoria(),
                        PESOS.format(p.precio()), p.cantidad()});
                unidades += p.cantidad();
                valorTotal += p.precio() * p.cantidad();
            }
            etiquetaResumen.setText(String.format("Productos: %d   |   Unidades en inventario: %d   |   Valor del inventario: %s",
                    productos.size(), unidades, PESOS.format(valorTotal)));
            if (texto != null && !texto.isBlank()) {
                mostrarExito("Búsqueda \"" + texto.trim() + "\": " + productos.size() + " producto(s) encontrado(s).");
            }
        } catch (SQLException e) {
            mostrarError(ProductoDAO.mensajeAmigable(e));
        }
    }

    private void cargarSeleccionEnFormulario() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idSeleccionado = (Integer) modelo.getValueAt(fila, 0);
        campoCodigo.setText((String) modelo.getValueAt(fila, 1));
        campoNombre.setText((String) modelo.getValueAt(fila, 2));
        String categoria = (String) modelo.getValueAt(fila, 3);
        for (int i = 0; i < comboCategoria.getItemCount(); i++) {
            if (comboCategoria.getItemAt(i).nombre().equals(categoria)) {
                comboCategoria.setSelectedIndex(i);
            }
        }
        campoPrecio.setText(((String) modelo.getValueAt(fila, 4)).replaceAll("[^0-9]", ""));
        campoCantidad.setText(String.valueOf(modelo.getValueAt(fila, 5)));
        mostrarExito("Producto " + campoCodigo.getText() + " seleccionado. Puede actualizarlo o eliminarlo.");
    }

    // ------------------------------------------------------------------
    // Create, Update y Delete
    // ------------------------------------------------------------------
    private void guardar(ActionEvent e) {
        try {
            Producto p = leerFormulario(0);
            dao.crear(p);
            recargarTabla();
            limpiarFormulario();
            mostrarExito("Producto " + p.codigo() + " guardado correctamente.");
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ProductoDAO.mensajeAmigable(ex));
        }
    }

    private void actualizar(ActionEvent e) {
        if (idSeleccionado == 0) {
            mostrarError("Seleccione en la tabla el producto que desea actualizar.");
            return;
        }
        try {
            Producto p = leerFormulario(idSeleccionado);
            dao.actualizar(p);
            recargarTabla();
            limpiarFormulario();
            mostrarExito("Producto " + p.codigo() + " actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ProductoDAO.mensajeAmigable(ex));
        }
    }

    private void eliminar(ActionEvent e) {
        if (idSeleccionado == 0) {
            mostrarError("Seleccione en la tabla el producto que desea eliminar.");
            return;
        }
        String codigo = campoCodigo.getText();
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Desea eliminar el producto " + codigo + "?\nEsta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.eliminar(idSeleccionado);
            recargarTabla();
            limpiarFormulario();
            mostrarExito("Producto " + codigo + " eliminado correctamente.");
        } catch (SQLException ex) {
            mostrarError(ProductoDAO.mensajeAmigable(ex));
        }
    }

    /** Ejecuta los dos scripts de la carpeta sql: crea las tablas y las llena con datos de ejemplo. */
    private void crearBaseDatos(ActionEvent e) {
        if (!Files.exists(SCRIPT_CREAR) || !Files.exists(SCRIPT_POBLAR)) {
            mostrarError("No se encontró la carpeta sql con los scripts. Ejecute la aplicación desde la carpeta del proyecto.");
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Se crearán de nuevo las tablas y se cargarán los datos de ejemplo.\n"
                        + "Los datos actuales se borrarán. ¿Desea continuar?",
                "Crear base de datos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int sentencias = dao.ejecutarScript(SCRIPT_CREAR) + dao.ejecutarScript(SCRIPT_POBLAR);
            cargarCategorias();
            campoBuscar.setText("");
            recargarTabla();
            limpiarFormulario();
            mostrarExito("Base de datos creada: se ejecutaron " + sentencias + " sentencias de los scripts SQL.");
        } catch (IOException ex) {
            mostrarError("No se pudo leer el script: " + ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ProductoDAO.mensajeAmigable(ex));
        }
    }

    // ------------------------------------------------------------------
    // Utilidades del formulario
    // ------------------------------------------------------------------
    /** Convierte los campos del formulario en un Producto; lanza IllegalArgumentException si hay errores. */
    private Producto leerFormulario(int id) {
        Categoria categoria = (Categoria) comboCategoria.getSelectedItem();
        Producto p = new Producto(id, campoCodigo.getText().trim(), campoNombre.getText().trim(),
                categoria == null ? 0 : categoria.id(), null,
                convertirNumero(campoPrecio.getText(), "precio"),
                (int) convertirNumero(campoCantidad.getText(), "cantidad"));
        ProductoDAO.validar(p);
        return p;
    }

    /** Acepta números escritos como 1599900, 1.599.900 o $ 1.599.900. */
    static double convertirNumero(String texto, String campo) {
        String limpio = texto == null ? "" : texto.replace("$", "").replace(".", "").replace(" ", "").replace(",", ".");
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un número.");
        }
    }

    private void limpiarFormulario() {
        idSeleccionado = 0;
        tabla.clearSelection();
        campoCodigo.setText("");
        campoNombre.setText("");
        campoPrecio.setText("");
        campoCantidad.setText("");
        if (comboCategoria.getItemCount() > 0) {
            comboCategoria.setSelectedIndex(0);
        }
        campoCodigo.requestFocusInWindow();
    }

    private void mostrarExito(String mensaje) {
        etiquetaMensaje.setForeground(VERDE);
        etiquetaMensaje.setText(mensaje);
    }

    private void mostrarError(String mensaje) {
        etiquetaMensaje.setForeground(ROJO);
        etiquetaMensaje.setText(mensaje);
    }
}

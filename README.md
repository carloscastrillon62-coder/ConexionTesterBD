# ConexionTesterBD — Probador de conexión a bases de datos (Swing + JDBC)

Aplicación de escritorio en **Java (Swing)** que permite ingresar la configuración
de una base de datos (URL, usuario y contraseña) y **validar la conexión con JDBC**,
mostrando el resultado en **verde** (conexión exitosa) o en **rojo** (fallo de conexión)
junto con el mensaje técnico del error.

Actividad EA3 — Unidad 3: Desarrollo de interfaces gráficas y conexión a bases de datos.
Programación Orientada a Objetos II — Grupo 9.

## Requisitos

- JDK 17 o superior.
- Driver de SQLite incluido en `lib/sqlite-jdbc-3.42.0.1.jar` (no es un framework, es el
  driver JDBC que permite a Java comunicarse con SQLite).

## Estructura

```
ConexionTesterBD/
├── src/conexion/
│   ├── ConexionTester.java   JFrame con la interfaz y toda la lógica de conexión
│   └── PruebasConexion.java  6 pruebas de consola (sin frameworks)
├── lib/
│   └── sqlite-jdbc-3.42.0.1.jar
├── compilar.bat  ejecutar.bat  pruebas.bat
└── README.md
```

## Compilar y ejecutar (Windows)

Con doble clic en `compilar.bat` y luego en `ejecutar.bat`, o desde la terminal:

```powershell
javac -encoding UTF-8 -d out src\conexion\*.java
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.ConexionTester
```

Pruebas:

```powershell
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.PruebasConexion
```

En Linux o macOS cambie `;` por `:` en el classpath y `\` por `/` en las rutas.

## Casos para probar en la ventana

| URL | Usuario | Resultado esperado |
|---|---|---|
| `jdbc:sqlite:inventario.db` | (vacío) | Verde: conexión exitosa |
| `jdbc:mysql://localhost:3306/inventario` | root | Rojo: driver no encontrado (`ClassNotFoundException`) |
| `jdbc:sqlite:carpeta_inexistente/inventario.db` | (vacío) | Rojo: fallo de conexión (`SQLException`) |
| `jdbc:oracle:thin:@localhost:1521:xe` | admin | Rojo: "No suitable driver" (`SQLException`) |

La URL de SQLite crea el archivo `inventario.db` en la carpeta del proyecto si no existe.

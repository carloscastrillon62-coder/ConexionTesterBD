@echo off
echo === Pruebas de conexion ===
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.PruebasConexion
echo.
echo === Pruebas del CRUD ===
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.PruebasCRUD
pause

-- =====================================================================
--  Script 1: creación de la base de datos "inventario"
--  Motor: SQLite 3
--  EA3 - Incorporando UI y BD - Programación Orientada a Objetos II - Grupo 9
--
--  En SQLite la base de datos es un archivo. No se usa CREATE DATABASE:
--  el archivo inventario.db se crea solo al conectarse con la URL
--  jdbc:sqlite:inventario.db (desde la aplicación o desde DBeaver).
--
--  Este script borra las tablas si ya existen y las crea de nuevo vacías.
-- =====================================================================

PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;

-- Categorías de los productos (Celulares, Portátiles, etc.)
CREATE TABLE categorias (
    id      INTEGER      PRIMARY KEY AUTOINCREMENT,
    nombre  VARCHAR(50)  NOT NULL UNIQUE
);

-- Productos del inventario. Cada producto pertenece a una categoría.
CREATE TABLE productos (
    id            INTEGER        PRIMARY KEY AUTOINCREMENT,
    codigo        VARCHAR(20)    NOT NULL UNIQUE,
    nombre        VARCHAR(100)   NOT NULL,
    id_categoria  INTEGER        NOT NULL,
    precio        DECIMAL(12, 2) NOT NULL CHECK (precio >= 0),
    cantidad      INTEGER        NOT NULL CHECK (cantidad >= 0),
    FOREIGN KEY (id_categoria) REFERENCES categorias (id)
);

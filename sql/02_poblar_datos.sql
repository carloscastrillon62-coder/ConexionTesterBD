-- =====================================================================
--  Script 2: poblado de la base de datos "inventario"
--  Motor: SQLite 3
--  EA3 - Incorporando UI y BD - Programación Orientada a Objetos II - Grupo 9
--
--  Ejecutar después de 01_crear_tablas.sql.
--  Inserta 5 categorías y 15 productos de ejemplo (precios en pesos colombianos).
-- =====================================================================

PRAGMA foreign_keys = ON;

INSERT INTO categorias (nombre) VALUES
    ('Celulares'),
    ('Portátiles'),
    ('Tabletas'),
    ('Accesorios'),
    ('Audio');

INSERT INTO productos (codigo, nombre, id_categoria, precio, cantidad) VALUES
    ('CEL-001', 'Samsung Galaxy A55 128 GB',       1, 1599900, 12),
    ('CEL-002', 'Xiaomi Redmi Note 13 256 GB',     1,  999900, 20),
    ('CEL-003', 'Motorola Moto G84 256 GB',        1,  899900, 15),
    ('CEL-004', 'iPhone 15 128 GB',                1, 3899900,  5),
    ('POR-001', 'Lenovo IdeaPad Slim 3 Ryzen 5',   2, 2299900,  8),
    ('POR-002', 'HP 15 Intel Core i5 16 GB',       2, 2599900,  6),
    ('POR-003', 'Asus Vivobook 15 Core i7',        2, 3199900,  4),
    ('TAB-001', 'Samsung Galaxy Tab A9+ 64 GB',    3,  899900, 10),
    ('TAB-002', 'iPad 10.ª generación 64 GB',      3, 1999900,  3),
    ('ACC-001', 'Cargador USB-C 25 W',             4,   79900, 40),
    ('ACC-002', 'Mouse inalámbrico Logitech M170', 4,   59900, 35),
    ('ACC-003', 'Funda antigolpes Galaxy A55',     4,   39900, 25),
    ('AUD-001', 'Audífonos JBL Tune 520BT',        5,  219900, 18),
    ('AUD-002', 'Parlante JBL Go 3',               5,  169900, 22),
    ('AUD-003', 'Audífonos Xiaomi Redmi Buds 5',   5,  149900,  0);

-- Verificación: productos con el nombre de su categoría
SELECT p.codigo, p.nombre, c.nombre AS categoria, p.precio, p.cantidad
FROM productos p
JOIN categorias c ON c.id = p.id_categoria
ORDER BY p.codigo;

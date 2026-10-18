ALTER TABLE users ALTER COLUMN name TYPE varchar(120) COLLATE "pt-BR-x-icu";

ALTER TABLE categories ALTER COLUMN name TYPE varchar(60) COLLATE "pt-BR-x-icu";

ALTER TABLE products ALTER COLUMN name TYPE varchar(80) COLLATE "pt-BR-x-icu";

ALTER TABLE order_items ALTER COLUMN product_name TYPE varchar(80) COLLATE "pt-BR-x-icu";

CREATE TABLE categories (
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id                UUID          NOT NULL,
    sku               VARCHAR(64)   NOT NULL,
    name              VARCHAR(200)  NOT NULL,
    description       VARCHAR(2000),
    price             NUMERIC(19,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    category_id       UUID,
    status            VARCHAR(20)   NOT NULL,
    stock_quantity    INTEGER       NOT NULL,
    reserved_quantity INTEGER       NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_created_at ON products (created_at DESC);

DROP TABLE IF EXISTS zh_property;

CREATE TABLE zh_property (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    city VARCHAR(128) NOT NULL,
    region VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    main_layout VARCHAR(256),
    avg_price DECIMAL(18, 2),
    address VARCHAR(512),
    longitude DECIMAL(12, 8) NOT NULL,
    latitude DECIMAL(12, 8) NOT NULL,
    data_update_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

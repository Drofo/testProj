ALTER TABLE restaurants
    ADD COLUMN owner_id BIGINT NOT NULL,
ADD CONSTRAINT fk_restaurant_owner FOREIGN KEY (owner_id) REFERENCES users(id);

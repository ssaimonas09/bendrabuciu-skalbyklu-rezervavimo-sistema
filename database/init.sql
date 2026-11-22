CREATE TABLE dormitories (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    reservation_minutes INTEGER NOT NULL DEFAULT 60,
    opens_at TIME NOT NULL DEFAULT '07:00',
    closes_at TIME NOT NULL DEFAULT '23:00'
);

CREATE TABLE users (
    dormitory_id INTEGER NOT NULL REFERENCES dormitories(id),
    id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (dormitory_id, id)
);

CREATE TABLE washing_machines (
    dormitory_id INTEGER NOT NULL REFERENCES dormitories(id),
    id INTEGER NOT NULL,
    PRIMARY KEY (dormitory_id, id)
);

CREATE SEQUENCE reservation_id_seq START WITH 1;

CREATE TABLE reservations (
    id INTEGER PRIMARY KEY DEFAULT nextval('reservation_id_seq'),
    dormitory_id INTEGER NOT NULL REFERENCES dormitories(id),
    user_id INTEGER NOT NULL,
    washing_machine_id INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    FOREIGN KEY (dormitory_id, user_id) REFERENCES users(dormitory_id, id),
    FOREIGN KEY (dormitory_id, washing_machine_id)
        REFERENCES washing_machines(dormitory_id, id),
    CHECK (end_time > start_time)
);

INSERT INTO dormitories (id, name) VALUES
    (1, 'Bendrabutis A'), (2, 'Bendrabutis B');

INSERT INTO users (dormitory_id, id, name) VALUES
    (1, 101, 'Studentas A'), (2, 101, 'Studentas B');

INSERT INTO washing_machines (dormitory_id, id) VALUES
    (1, 2), (1, 3), (2, 2), (2, 3);

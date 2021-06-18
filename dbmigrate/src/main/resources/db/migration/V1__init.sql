CREATE TABLE flashcards_account (
    id uuid PRIMARY KEY,
    user_id uuid UNIQUE NOT NULL
);

CREATE TABLE card_source (
    id uuid PRIMARY KEY,
    owner_id uuid REFERENCES flashcards_account(id) NOT NULL,
    name text NOT NULL,
    type text NOT NULL
);
CREATE INDEX card_source_owner_id_idx ON card_source(owner_id);

CREATE TABLE deck (
    id uuid PRIMARY KEY,
    owner_id uuid REFERENCES flashcards_account(id) NOT NULL,
    name text NOT NULL
);
CREATE INDEX deck_owner_id_idx ON deck(owner_id);

CREATE TABLE deck_card_source (
    deck_id uuid REFERENCES deck(id) NOT NULL,
    source_id uuid REFERENCES card_source(id) NOT NULL,
    PRIMARY KEY(deck_id, source_id)
);

CREATE TABLE custom_card_source_cards (
    id uuid PRIMARY KEY,
    source_id uuid REFERENCES card_source(id) UNIQUE NOT NULL,
    version integer NOT NULL,
    contents jsonb NOT NULL
);

CREATE TABLE srs_system (
    id uuid PRIMARY KEY,
    name text UNIQUE NOT NULL,
    version integer NOT NULL,
    stages jsonb NOT NULL
);
INSERT INTO srs_system(id, name, version, stages) VALUES
('68c9ed88-ce50-11eb-b8bc-0242ac130003', 'default', 1, '[0,14400,28800,82800,169200,601200,1206000,2588400,10364400]'::jsonb);

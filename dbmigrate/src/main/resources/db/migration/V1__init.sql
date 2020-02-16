CREATE TABLE flashcards (
    id uuid PRIMARY KEY,
    user_id uuid UNIQUE NOT NULL,
    version integer NOT NULL,
    contents jsonb NOT NULL
);

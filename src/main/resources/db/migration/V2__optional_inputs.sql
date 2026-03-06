-- Optional inputs for model embeddings (inputs the model can use but does not require)
CREATE TABLE model_embedding_optional_inputs
(
    model_embedding_id uuid NOT NULL REFERENCES model_embeddings (id) ON DELETE CASCADE,
    kind               text NOT NULL
);

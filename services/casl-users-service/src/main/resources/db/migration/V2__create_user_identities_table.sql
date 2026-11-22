CREATE TABLE user_identities (
    id       UUID         NOT NULL,
    user_id  UUID         NOT NULL,
    provider VARCHAR(40)  NOT NULL,
    subject  VARCHAR(100) NOT NULL,
    CONSTRAINT pk_user_identities PRIMARY KEY (id),
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, subject),
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_identities_user_id ON user_identities (user_id);

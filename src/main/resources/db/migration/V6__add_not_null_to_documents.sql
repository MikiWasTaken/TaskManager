ALTER TABLE documents
    ALTER COLUMN project_id SET NOT NULL,
    ALTER COLUMN owner_id SET NOT NULL;

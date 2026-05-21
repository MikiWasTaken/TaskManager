-- mock data for demo
-- all password are "password"
-- BCrypt(10) hash:   $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

-- Quick reference — IDs used below:
--   Alice  (ADMIN)          67d0ae7b-06a6-43d7-99a4-90933f024a39
--   Bob    (USER)           4ef21d5e-17d4-4b0b-be0d-c5888b19aacc
--   Carol  (USER)           4c0f7f22-fedc-44f1-afb6-75a469ba45a9
--   Dave   (USER, not member of any project) 7b0bd519-2b30-41a3-b7cf-3ed18f01959b
--   Eve    (USER, inactive) 0b38e3fb-e347-472b-ba77-dd97eac252f9

--   Apollo (ACTIVE project) 7be1ce72-9da1-435c-84ba-d61d9e732970  owner: Alice
--   Beta   (COMPLETE)       3360799c-d9f5-4d0b-ac3c-977cc9393f09  owner: Bob
--   Gamma  (soft-deleted)   0a5a7f17-c24d-49af-84aa-bb933c1fd799  owner: Alice


INSERT INTO users (id, email, password, first_name, last_name, role, active, created_at)
VALUES
    ('67d0ae7b-06a6-43d7-99a4-90933f024a39',
     'alice@example.com',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     'Alice', 'Admin', 'ADMIN', true, '2026-01-01 09:00:00'),

    ('4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     'bob@example.com',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     'Bob', 'Builder', 'USER', true, '2026-01-02 09:00:00'),

    ('4c0f7f22-fedc-44f1-afb6-75a469ba45a9',
     'carol@example.com',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     'Carol', 'Coder', 'USER', true, '2026-01-03 09:00:00'),

    -- Dave is active but not a member of any project (useful for "outsider" test cases)
    ('7b0bd519-2b30-41a3-b7cf-3ed18f01959b',
     'dave@example.com',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     'Dave', 'Designer', 'USER', true, '2026-01-04 09:00:00'),

    -- Eve is deactivated — login attempt returns 401
    ('0b38e3fb-e347-472b-ba77-dd97eac252f9',
     'eve@example.com',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     'Eve', 'Emerita', 'USER', false, '2026-01-05 09:00:00');


INSERT INTO projects (id, name, description, status, owner_id, created_at, updated_at, deleted_at)
VALUES
    ('7be1ce72-9da1-435c-84ba-d61d9e732970',
     'Apollo', 'Backend rewrite project', 'ACTIVE',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-01-10 10:00:00', '2026-01-10 10:00:00', NULL),

    ('3360799c-d9f5-4d0b-ac3c-977cc9393f09',
     'Beta', 'Mobile app launch', 'COMPLETE',
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     '2026-01-15 10:00:00', '2026-04-01 16:00:00', NULL),

    ('0a5a7f17-c24d-49af-84aa-bb933c1fd799',
     'Gamma', 'Archived infrastructure work', 'ACTIVE',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-02-01 10:00:00', '2026-03-01 12:00:00', '2026-03-01 12:00:00');


-- Apollo: Alice (owner), Bob, Carol
-- Beta:   Bob (owner), Carol
-- Gamma:  Alice (owner) — deleted, kept for admin task-listing demo

INSERT INTO project_members (project_id, user_id)
VALUES
    ('7be1ce72-9da1-435c-84ba-d61d9e732970', '67d0ae7b-06a6-43d7-99a4-90933f024a39'),
    ('7be1ce72-9da1-435c-84ba-d61d9e732970', '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc'),
    ('7be1ce72-9da1-435c-84ba-d61d9e732970', '4c0f7f22-fedc-44f1-afb6-75a469ba45a9'),
    ('3360799c-d9f5-4d0b-ac3c-977cc9393f09', '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc'),
    ('3360799c-d9f5-4d0b-ac3c-977cc9393f09', '4c0f7f22-fedc-44f1-afb6-75a469ba45a9'),
    ('0a5a7f17-c24d-49af-84aa-bb933c1fd799', '67d0ae7b-06a6-43d7-99a4-90933f024a39');


-- ─── TASKS ───────────────────────────────────────────────────────────────────
INSERT INTO tasks (id, title, description, priority, status, deadline,
                   project_id, assigned_to, created_by, created_at, updated_at, deleted_at)
VALUES
    ('eb5a0b7b-bc12-4fa2-842d-0abd8189c058',
     'Set up CI pipeline',
     'Configure GitHub Actions for automated builds and deployments',
     'HIGH', 'TODO', '2026-06-01 12:00:00',
     '7be1ce72-9da1-435c-84ba-d61d9e732970',
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-01-11 09:00:00', '2026-01-11 09:00:00', NULL),

    ('45437dd2-4dd4-45de-b5ff-76b61b3a0479',
     'Design database schema',
     'Create ERD and write all Flyway migrations',
     'MEDIUM', 'IN_PROGRESS', '2026-05-15 12:00:00',
     '7be1ce72-9da1-435c-84ba-d61d9e732970',
     '4c0f7f22-fedc-44f1-afb6-75a469ba45a9',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-01-12 09:00:00', '2026-02-01 10:00:00', NULL),

    ('c3f6f87b-fced-40d4-9e02-c7cccf2836c0',
     'Write unit tests',
     'Cover all service-layer methods with JUnit tests',
     'LOW', 'DONE', NULL,
     '7be1ce72-9da1-435c-84ba-d61d9e732970',
     NULL,
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     '2026-01-15 09:00:00', '2026-03-10 11:00:00', NULL),

    -- Apollo / soft-deleted — visible only to admins in task list
    ('7c3f1525-4049-4a3a-b93b-e55a30b2ee01',
     'Refactor auth module',
     'Outdated code — replaced by new JWT implementation',
     'MEDIUM', 'TODO', NULL,
     '7be1ce72-9da1-435c-84ba-d61d9e732970',
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-02-01 09:00:00', '2026-02-01 09:00:00', '2026-03-01 08:00:00'),

    ('be78caa8-835a-43a6-a054-8809ef4895cb',
     'Deploy to production',
     'Final deployment to AWS — run smoke tests after',
     'HIGH', 'DONE', NULL,
     '3360799c-d9f5-4d0b-ac3c-977cc9393f09',
     '4c0f7f22-fedc-44f1-afb6-75a469ba45a9',
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     '2026-02-10 09:00:00', '2026-04-01 15:00:00', NULL),

    ('faa26561-cb3d-4190-a479-748950bb770f',
     'Initial server setup',
     'Provision EC2 instances and configure VPC',
     'HIGH', 'TODO', NULL,
     '0a5a7f17-c24d-49af-84aa-bb933c1fd799',
     NULL,
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-02-02 09:00:00', '2026-02-02 09:00:00', NULL);


-- metadata only. download will fail unless the matching object exists in the bucket
INSERT INTO documents (id, name, type, size, object_key, project_id, owner_id, uploaded_at)
VALUES
    ('26b3326a-c541-43c1-b855-25931b4d6d60',
     'requirements.pdf', 'application/pdf', 204800,
     '7be1ce72-9da1-435c-84ba-d61d9e732970/26b3326a-requirements.pdf',
     '7be1ce72-9da1-435c-84ba-d61d9e732970',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     '2026-01-20 14:00:00'),

    ('c73e9516-2a68-4e7a-82da-4cfd647bbdf9',
     'sprint-plan.xlsx',
     'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
     51200,
     '7be1ce72-9da1-435c-84ba-d61d9e732970/c73e9516-sprint-plan.xlsx',
     '7be1ce72-9da1-435c-84ba-d61d9e732970',
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     '2026-01-22 11:30:00');


INSERT INTO audit_logs (id, action, performed_by, entity_type, entity_id, details, created_at)
VALUES
    ('e47d5afb-9704-403c-89fa-2759f050eea7',
     'USER_REGISTER', 'alice@example.com', 'User',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     'New user registered', '2026-01-01 09:00:01'),

    ('7ef73f11-af34-4c99-9bf5-b4ada3adec85',
     'USER_LOGIN', 'alice@example.com', 'User',
     '67d0ae7b-06a6-43d7-99a4-90933f024a39',
     'Login attempt', '2026-01-10 08:55:00'),

    ('b23ab896-b3d8-4873-8d3f-e0a52944aff8',
     'USER_LOGIN', 'bob@example.com', 'User',
     '4ef21d5e-17d4-4b0b-be0d-c5888b19aacc',
     'Login attempt', '2026-01-10 09:05:00'),

    ('ff94282e-0a81-4ad2-aa1a-cdd5728e3711',
     'PROJECT_DELETE', 'alice@example.com', 'Project',
     '0a5a7f17-c24d-49af-84aa-bb933c1fd799',
     'Soft deleted project: Gamma', '2026-03-01 12:00:00'),

    ('70de5a53-061e-4064-aedf-6379ab4b793a',
     'DOCUMENT_UPLOAD', 'alice@example.com', 'Document',
     '26b3326a-c541-43c1-b855-25931b4d6d60',
     'File: requirements.pdf', '2026-01-20 14:00:00'),

    ('5089ae22-de61-4295-b1d6-38a2b200f7c0',
     'DOCUMENT_UPLOAD', 'bob@example.com', 'Document',
     'c73e9516-2a68-4e7a-82da-4cfd647bbdf9',
     'File: sprint-plan.xlsx', '2026-01-22 11:30:00');

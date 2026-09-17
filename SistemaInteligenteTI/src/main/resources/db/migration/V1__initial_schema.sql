-- Esquema inicial del MVP de gestión de incidentes de TI.
-- Implementa el modelo de datos de specs/001-incidentes-mvp/plan.md (sección 2),
-- que a su vez cubre RF-1 a RF-14 de specs/001-incidentes-mvp/spec.md.
--
-- Los catálogos se guardan como VARCHAR con CHECK en lugar de tipos ENUM de PostgreSQL:
-- se mapean directamente a los enums de Java y ampliarlos más adelante es una migración
-- normal en vez de un ALTER TYPE.

-- Búsqueda semántica del histórico (RF-11, RF-13).
CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------------------------------------------------------
-- users — supervisores y técnicos (RF-1, RF-2)
-- Sin contraseña: el MVP no autentica, el usuario activo se elige de una lista.
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    full_name  VARCHAR(120) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    area       VARCHAR(30),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT users_username_unique UNIQUE (username),
    CONSTRAINT users_role_valid CHECK (role IN ('SUPERVISOR', 'TECHNICIAN')),
    CONSTRAINT users_area_valid CHECK (
        area IS NULL OR area IN ('INFRASTRUCTURE', 'APPLICATIONS', 'DATABASE', 'SECURITY', 'USER_SUPPORT')
    ),
    -- Un técnico pertenece exactamente a un área; un supervisor no tiene área (RF-2).
    CONSTRAINT users_area_matches_role CHECK (
        (role = 'TECHNICIAN' AND area IS NOT NULL)
        OR (role = 'SUPERVISOR' AND area IS NULL)
    )
);

COMMENT ON TABLE users IS 'Supervisores y técnicos. El área del técnico decide qué incidentes puede recibir (RF-2, RF-7).';

-- ---------------------------------------------------------------------------
-- incidents — ciclo completo del incidente (RF-3 a RF-14)
-- ---------------------------------------------------------------------------
CREATE TABLE incidents (
    id                    BIGSERIAL    PRIMARY KEY,
    title                 VARCHAR(150) NOT NULL,
    description           TEXT         NOT NULL,
    status                VARCHAR(30)  NOT NULL,
    classification_status VARCHAR(25)  NOT NULL,
    type                  VARCHAR(20),
    priority              VARCHAR(10),
    area                  VARCHAR(30),
    created_by            BIGINT       NOT NULL,
    assigned_to           BIGINT,
    solution              TEXT,
    closure_reason        VARCHAR(25),
    indexed               BOOLEAN      NOT NULL DEFAULT FALSE,
    analysis_started_at   TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    assigned_at           TIMESTAMPTZ,
    started_at            TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    version               BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT incidents_created_by_fk  FOREIGN KEY (created_by)  REFERENCES users (id),
    CONSTRAINT incidents_assigned_to_fk FOREIGN KEY (assigned_to) REFERENCES users (id),

    CONSTRAINT incidents_status_valid CHECK (
        status IN ('REGISTERED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED_WITHOUT_SOLUTION')
    ),
    CONSTRAINT incidents_classification_status_valid CHECK (
        classification_status IN ('PENDING', 'AI_CLASSIFIED', 'MANUALLY_CORRECTED')
    ),
    CONSTRAINT incidents_type_valid CHECK (
        type IS NULL OR type IN ('BUG', 'PERFORMANCE', 'ACCESS', 'HARDWARE', 'NETWORK', 'CONFIGURATION')
    ),
    CONSTRAINT incidents_priority_valid CHECK (
        priority IS NULL OR priority IN ('HIGH', 'MEDIUM', 'LOW')
    ),
    CONSTRAINT incidents_area_valid CHECK (
        area IS NULL OR area IN ('INFRASTRUCTURE', 'APPLICATIONS', 'DATABASE', 'SECURITY', 'USER_SUPPORT')
    ),
    CONSTRAINT incidents_closure_reason_valid CHECK (
        closure_reason IS NULL OR closure_reason IN ('DUPLICATE', 'NOT_REPRODUCIBLE', 'DISCARDED')
    ),

    -- La clasificación es completa o no existe: nunca a medias (RF-4, RF-8).
    CONSTRAINT incidents_classification_complete CHECK (
        (classification_status = 'PENDING'
            AND type IS NULL AND priority IS NULL AND area IS NULL)
        OR (classification_status <> 'PENDING'
            AND type IS NOT NULL AND priority IS NOT NULL AND area IS NOT NULL)
    ),

    -- Registrado no tiene técnico; cualquier otro estado sí lo tiene (RF-6, RF-7).
    CONSTRAINT incidents_assignment_matches_status CHECK (
        (status = 'REGISTERED' AND assigned_to IS NULL)
        OR (status <> 'REGISTERED' AND assigned_to IS NOT NULL)
    ),

    -- Resuelto exige solución con la longitud mínima y fecha de cierre (RF-12).
    CONSTRAINT incidents_resolution_complete CHECK (
        status <> 'RESOLVED'
        OR (solution IS NOT NULL AND char_length(solution) >= 30 AND closed_at IS NOT NULL)
    ),

    -- Cerrado sin solución exige motivo y fecha de cierre, y no exige solución (RF-14).
    CONSTRAINT incidents_closure_complete CHECK (
        status <> 'CLOSED_WITHOUT_SOLUTION'
        OR (closure_reason IS NOT NULL AND closed_at IS NOT NULL)
    ),

    -- Solo un incidente resuelto puede formar parte del histórico (RF-13, RF-14).
    CONSTRAINT incidents_indexed_only_when_resolved CHECK (
        indexed = FALSE OR status = 'RESOLVED'
    )
);

COMMENT ON TABLE incidents IS 'Incidentes y su ciclo completo: registro, clasificación, asignación, trabajo y cierre (RF-3 a RF-14).';
COMMENT ON COLUMN incidents.indexed IS 'TRUE cuando el incidente resuelto ya está en el histórico consultable por el RAG (RF-13).';
COMMENT ON COLUMN incidents.analysis_started_at IS 'Instante del análisis en curso; impide lanzar dos análisis a la vez (RF-11).';
COMMENT ON COLUMN incidents.version IS 'Bloqueo optimista para asignaciones y cierres concurrentes.';

-- Filtros del listado por rol, prioridad y estado (RF-8).
CREATE INDEX idx_incidents_status                ON incidents (status);
CREATE INDEX idx_incidents_priority              ON incidents (priority);
CREATE INDEX idx_incidents_area                  ON incidents (area);
CREATE INDEX idx_incidents_assigned_to           ON incidents (assigned_to);
CREATE INDEX idx_incidents_classification_status ON incidents (classification_status);

-- ---------------------------------------------------------------------------
-- incident_events — traza de quién hace qué y cuándo (RNF-6)
-- ---------------------------------------------------------------------------
CREATE TABLE incident_events (
    id           BIGSERIAL   PRIMARY KEY,
    incident_id  BIGINT      NOT NULL,
    action       VARCHAR(40) NOT NULL,
    performed_by BIGINT,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    detail       JSONB,

    CONSTRAINT incident_events_incident_fk FOREIGN KEY (incident_id) REFERENCES incidents (id) ON DELETE CASCADE,
    CONSTRAINT incident_events_user_fk     FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT incident_events_action_valid CHECK (
        action IN (
            'CREATED', 'AI_CLASSIFIED', 'CLASSIFICATION_FAILED', 'CLASSIFICATION_RETRIED',
            'MANUALLY_CLASSIFIED', 'ASSIGNED', 'REASSIGNED', 'UNASSIGNED_BY_AREA_CHANGE',
            'STARTED', 'ANALYSIS_REQUESTED', 'ANALYSIS_FAILED', 'RESOLVED',
            'CLOSED_WITHOUT_SOLUTION', 'INDEXED', 'INDEXING_FAILED'
        )
    )
);

COMMENT ON TABLE incident_events IS 'Traza de acciones sobre cada incidente (RNF-6).';
COMMENT ON COLUMN incident_events.performed_by IS 'Nulo cuando la acción la ejecuta el sistema y no una persona.';

CREATE INDEX idx_incident_events_incident ON incident_events (incident_id, performed_at);

-- ---------------------------------------------------------------------------
-- incident_embeddings — histórico vectorial (RF-11, RF-13)
-- Tabla propiedad del servicio Python: el backend nunca consulta vectores.
-- ---------------------------------------------------------------------------
CREATE TABLE incident_embeddings (
    incident_id BIGINT      PRIMARY KEY,
    embedding   VECTOR(384) NOT NULL,
    model_name  VARCHAR(80) NOT NULL,
    indexed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT incident_embeddings_incident_fk FOREIGN KEY (incident_id) REFERENCES incidents (id) ON DELETE CASCADE
);

COMMENT ON TABLE incident_embeddings IS 'Un vector por incidente resuelto e indexado; lo escribe y consulta solo el servicio de IA (RF-13).';
COMMENT ON COLUMN incident_embeddings.embedding IS 'Dimensión 384, atada al modelo paraphrase-multilingual-MiniLM-L12-v2 de sentence-transformers: cambiarlo obliga a migrar y reindexar.';

-- Búsqueda de los casos más parecidos por distancia coseno (RF-11).
-- HNSW requiere pgvector 0.5 o superior. Con una versión anterior, sustituir por:
--   CREATE INDEX idx_incident_embeddings_vector ON incident_embeddings
--       USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_incident_embeddings_vector ON incident_embeddings
    USING hnsw (embedding vector_cosine_ops);

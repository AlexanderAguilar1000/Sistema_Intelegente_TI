-- Supervisor que realizó la asignación del incidente (RF-7, T-15).
ALTER TABLE incidents
    ADD COLUMN assigned_by BIGINT,
    ADD CONSTRAINT incidents_assigned_by_fk FOREIGN KEY (assigned_by) REFERENCES users (id);

COMMENT ON COLUMN incidents.assigned_by IS 'Supervisor que asignó el incidente al técnico actual (RF-7).';

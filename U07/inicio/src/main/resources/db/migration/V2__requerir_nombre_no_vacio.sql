ALTER TABLE alumnos ALTER COLUMN nombre SET NOT NULL;
ALTER TABLE alumnos ADD CONSTRAINT ck_alumnos_nombre_no_vacio CHECK (TRIM(nombre) <> '');
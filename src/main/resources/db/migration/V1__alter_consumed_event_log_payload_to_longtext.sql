-- Ajustar tipos de columnas LOB para evitar truncamiento en MySQL
-- Esta migración convierte columnas a LONGTEXT

-- ALTER TABLE consumed_event_log
 --    MODIFY COLUMN payload_json LONGTEXT NULL,
 --   MODIFY COLUMN ack_last_error LONGTEXT NULL;


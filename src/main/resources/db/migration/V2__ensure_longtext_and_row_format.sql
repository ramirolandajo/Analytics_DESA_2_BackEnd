-- Asegurar LONGTEXT y formato de fila apropiado para payloads grandes
ALTER TABLE consumed_event_log ROW_FORMAT=DYNAMIC;
ALTER TABLE consumed_event_log
    MODIFY COLUMN payload_json LONGTEXT NULL,
    MODIFY COLUMN ack_last_error LONGTEXT NULL;


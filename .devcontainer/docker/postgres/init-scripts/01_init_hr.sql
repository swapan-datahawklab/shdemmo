-- Create HR schema
CREATE SCHEMA hr;

-- Set search path
SET search_path TO hr;

-- Create sequences
CREATE SEQUENCE locations_seq
    START WITH 3300
    INCREMENT BY 100
    MAXVALUE 9900
    NO CYCLE;

CREATE SEQUENCE departments_seq
    START WITH 280
    INCREMENT BY 10
    MAXVALUE 9990
    NO CYCLE;

CREATE SEQUENCE employees_seq
    START WITH 207
    INCREMENT BY 1
    NO CYCLE;
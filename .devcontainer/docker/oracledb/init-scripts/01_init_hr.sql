-- Create HR user and grant necessary privileges
ALTER SESSION SET CONTAINER = FREEPDB1;

-- Create HR user if it doesn't exist
CREATE USER hr IDENTIFIED BY hr;

-- Grant necessary privileges to HR user
GRANT CREATE SESSION TO hr;
GRANT CREATE TABLE TO hr;
GRANT CREATE VIEW TO hr;
GRANT CREATE SEQUENCE TO hr;
GRANT CREATE PROCEDURE TO hr;
GRANT UNLIMITED TABLESPACE TO hr; 
-- Enable HR schema and load sample data
ALTER SESSION SET CONTAINER = XEPDB1;
ALTER PLUGGABLE DATABASE XEPDB1 SAVE STATE;
@?/demo/schema/human_resources/hr_main.sql HR HR USERS TEMP /tmp oracle_sample_hr_ 
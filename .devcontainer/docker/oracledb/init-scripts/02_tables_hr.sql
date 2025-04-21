-- Connect as HR user and set container
ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = HR;

-- Create base tables without constraints or indexes
CREATE TABLE regions (
    region_id NUMBER,
    region_name VARCHAR2(25)
);

CREATE TABLE countries (
    country_id CHAR(2),
    country_name VARCHAR2(40),
    region_id NUMBER
);

CREATE TABLE locations (
    location_id NUMBER,
    street_address VARCHAR2(40),
    postal_code VARCHAR2(12),
    city VARCHAR2(30),
    state_province VARCHAR2(25),
    country_id CHAR(2)
);

CREATE TABLE departments (
    department_id NUMBER,
    department_name VARCHAR2(30),
    manager_id NUMBER,
    location_id NUMBER
);

CREATE TABLE jobs (
    job_id VARCHAR2(10),
    job_title VARCHAR2(35),
    min_salary NUMBER,
    max_salary NUMBER
);

CREATE TABLE employees (
    employee_id NUMBER,
    first_name VARCHAR2(20),
    last_name VARCHAR2(25),
    email VARCHAR2(25),
    phone_number VARCHAR2(20),
    hire_date DATE,
    job_id VARCHAR2(10),
    salary NUMBER,
    commission_pct NUMBER,
    manager_id NUMBER,
    department_id NUMBER
);

CREATE TABLE job_history (
    employee_id NUMBER,
    start_date DATE,
    end_date DATE,
    job_id VARCHAR2(10),
    department_id NUMBER
); 
-- HR Schema Setup Script
-- This script creates the HR schema, tables, and populates them with sample data

-- Drop existing schema and objects if they exist
DROP USER hr CASCADE;

-- Create HR user and grant necessary privileges
CREATE USER hr IDENTIFIED BY hr;
GRANT CONNECT, RESOURCE TO hr;
ALTER USER hr QUOTA UNLIMITED ON USERS;

-- Connect as HR user
CONNECT hr/hr;

-- Create tables
CREATE TABLE regions (
    region_id NUMBER GENERATED ALWAYS AS IDENTITY,
    region_name VARCHAR2(25),
    CONSTRAINT reg_id_pk PRIMARY KEY (region_id)
);

CREATE TABLE countries (
    country_id CHAR(2),
    country_name VARCHAR2(40),
    region_id NUMBER,
    CONSTRAINT country_c_id_pk PRIMARY KEY (country_id),
    CONSTRAINT countr_reg_fk FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

CREATE TABLE locations (
    location_id NUMBER GENERATED ALWAYS AS IDENTITY,
    street_address VARCHAR2(40),
    postal_code VARCHAR2(12),
    city VARCHAR2(30) NOT NULL,
    state_province VARCHAR2(25),
    country_id CHAR(2),
    CONSTRAINT loc_id_pk PRIMARY KEY (location_id),
    CONSTRAINT loc_c_id_fk FOREIGN KEY (country_id) REFERENCES countries(country_id)
);

CREATE TABLE departments (
    department_id NUMBER GENERATED ALWAYS AS IDENTITY,
    department_name VARCHAR2(30) NOT NULL,
    manager_id NUMBER,
    location_id NUMBER,
    CONSTRAINT dept_id_pk PRIMARY KEY (department_id),
    CONSTRAINT dept_loc_fk FOREIGN KEY (location_id) REFERENCES locations(location_id)
);

CREATE TABLE jobs (
    job_id VARCHAR2(10),
    job_title VARCHAR2(35) NOT NULL,
    min_salary NUMBER,
    max_salary NUMBER,
    CONSTRAINT job_id_pk PRIMARY KEY (job_id)
);

CREATE TABLE employees (
    employee_id NUMBER GENERATED ALWAYS AS IDENTITY,
    first_name VARCHAR2(20),
    last_name VARCHAR2(25) NOT NULL,
    email VARCHAR2(25) NOT NULL,
    phone_number VARCHAR2(20),
    hire_date DATE NOT NULL,
    job_id VARCHAR2(10) NOT NULL,
    salary NUMBER(8,2),
    commission_pct NUMBER(2,2),
    manager_id NUMBER,
    department_id NUMBER,
    CONSTRAINT emp_emp_id_pk PRIMARY KEY (employee_id),
    CONSTRAINT emp_email_uk UNIQUE (email),
    CONSTRAINT emp_job_fk FOREIGN KEY (job_id) REFERENCES jobs(job_id),
    CONSTRAINT emp_manager_fk FOREIGN KEY (manager_id) REFERENCES employees(employee_id),
    CONSTRAINT emp_dept_fk FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

CREATE TABLE job_history (
    employee_id NUMBER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    job_id VARCHAR2(10) NOT NULL,
    department_id NUMBER,
    CONSTRAINT jhist_emp_id_st_date_pk PRIMARY KEY (employee_id, start_date),
    CONSTRAINT jhist_job_fk FOREIGN KEY (job_id) REFERENCES jobs(job_id),
    CONSTRAINT jhist_emp_fk FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    CONSTRAINT jhist_dept_fk FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- Populate regions
INSERT INTO regions (region_name) VALUES ('Europe');
INSERT INTO regions (region_name) VALUES ('Americas');
INSERT INTO regions (region_name) VALUES ('Asia');
INSERT INTO regions (region_name) VALUES ('Middle East and Africa');

-- Populate countries
INSERT INTO countries VALUES ('IT', 'Italy', 1);
INSERT INTO countries VALUES ('JP', 'Japan', 3);
INSERT INTO countries VALUES ('US', 'United States of America', 2);
INSERT INTO countries VALUES ('CA', 'Canada', 2);
INSERT INTO countries VALUES ('CN', 'China', 3);
INSERT INTO countries VALUES ('IN', 'India', 3);
INSERT INTO countries VALUES ('AU', 'Australia', 3);
INSERT INTO countries VALUES ('ZW', 'Zimbabwe', 4);
INSERT INTO countries VALUES ('SG', 'Singapore', 3);
INSERT INTO countries VALUES ('UK', 'United Kingdom', 1);
INSERT INTO countries VALUES ('FR', 'France', 1);
INSERT INTO countries VALUES ('DE', 'Germany', 1);
INSERT INTO countries VALUES ('ZM', 'Zambia', 4);
INSERT INTO countries VALUES ('EG', 'Egypt', 4);
INSERT INTO countries VALUES ('BR', 'Brazil', 2);
INSERT INTO countries VALUES ('CH', 'Switzerland', 1);
INSERT INTO countries VALUES ('NL', 'Netherlands', 1);
INSERT INTO countries VALUES ('MX', 'Mexico', 2);
INSERT INTO countries VALUES ('KW', 'Kuwait', 4);
INSERT INTO countries VALUES ('IL', 'Israel', 4);
INSERT INTO countries VALUES ('DK', 'Denmark', 1);
INSERT INTO countries VALUES ('HK', 'HongKong', 3);
INSERT INTO countries VALUES ('NG', 'Nigeria', 4);
INSERT INTO countries VALUES ('AR', 'Argentina', 2);
INSERT INTO countries VALUES ('BE', 'Belgium', 1);

-- Populate locations
INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('1297 Via Cola di Rie', '00989', 'Roma', NULL, 'IT');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('93091 Calle della Testa', '10934', 'Venice', NULL, 'IT');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('2017 Shinjuku-ku', '1689', 'Tokyo', 'Tokyo Prefecture', 'JP');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('9450 Kamiya-cho', '6823', 'Hiroshima', NULL, 'JP');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('2014 Jabberwocky Rd', '26192', 'Southlake', 'Texas', 'US');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('2011 Interiors Blvd', '99236', 'South San Francisco', 'California', 'US');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('2007 Zagora St', '50090', 'South Brunswick', 'New Jersey', 'US');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('2004 Charade Rd', '98199', 'Seattle', 'Washington', 'US');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('147 Spadina Ave', 'M5V 2L7', 'Toronto', 'Ontario', 'CA');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('6092 Boxwood St', 'YSW 9T2', 'Whitehorse', 'Yukon', 'CA');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('40-5-12 Laogianggen', '190518', 'Beijing', NULL, 'CN');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('1298 Vileparle (E)', '490231', 'Bombay', 'Maharashtra', 'IN');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('12-98 Victoria Street', '2901', 'Sydney', 'New South Wales', 'AU');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('198 Clementi North', '540198', 'Singapore', NULL, 'SG');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('8204 Arthur St', NULL, 'London', NULL, 'UK');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('Magdalen Centre, The Oxford Science Park', 'OX9 9ZB', 'Oxford', 'Oxford', 'UK');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('9702 Chester Road', '09629850293', 'Stretford', 'Manchester', 'UK');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('Schwanthalerstr. 7031', '80925', 'Munich', 'Bavaria', 'DE');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('Rua Frei Caneca 1360', '01307-002', 'Sao Paulo', 'Sao Paulo', 'BR');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('20 Rue des Corps-Saints', '1730', 'Geneva', 'Geneve', 'CH');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('Murtenstrasse 921', '3095', 'Bern', 'BE', 'CH');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('Pieter Breughelstraat 837', '3029SK', 'Utrecht', 'Utrecht', 'NL');

INSERT INTO locations (street_address, postal_code, city, state_province, country_id)
VALUES ('Mariano Escobedo 9991', '11932', 'Mexico City', 'Distrito Federal', 'MX');

-- Populate departments
INSERT INTO departments (department_name, location_id)
VALUES ('Administration', 1700);

INSERT INTO departments (department_name, location_id)
VALUES ('Marketing', 1800);

INSERT INTO departments (department_name, location_id)
VALUES ('Purchasing', 1700);

INSERT INTO departments (department_name, location_id)
VALUES ('Human Resources', 2400);

INSERT INTO departments (department_name, location_id)
VALUES ('Shipping', 1500);

INSERT INTO departments (department_name, location_id)
VALUES ('IT', 1400);

INSERT INTO departments (department_name, location_id)
VALUES ('Public Relations', 2700);

INSERT INTO departments (department_name, location_id)
VALUES ('Sales', 2500);

INSERT INTO departments (department_name, location_id)
VALUES ('Executive', 1700);

INSERT INTO departments (department_name, location_id)
VALUES ('Finance', 1700);

INSERT INTO departments (department_name, location_id)
VALUES ('Accounting', 1700);

-- Populate jobs
INSERT INTO jobs VALUES ('AD_PRES', 'President', 20000, 40000);
INSERT INTO jobs VALUES ('AD_VP', 'Administration Vice President', 15000, 30000);
INSERT INTO jobs VALUES ('AD_ASST', 'Administration Assistant', 3000, 6000);
INSERT INTO jobs VALUES ('FI_MGR', 'Finance Manager', 8200, 16000);
INSERT INTO jobs VALUES ('FI_ACCOUNT', 'Accountant', 4200, 9000);
INSERT INTO jobs VALUES ('AC_MGR', 'Accounting Manager', 8200, 16000);
INSERT INTO jobs VALUES ('AC_ACCOUNT', 'Public Accountant', 4200, 9000);
INSERT INTO jobs VALUES ('SA_MAN', 'Sales Manager', 10000, 20000);
INSERT INTO jobs VALUES ('SA_REP', 'Sales Representative', 6000, 12000);
INSERT INTO jobs VALUES ('PU_MAN', 'Purchasing Manager', 8000, 15000);
INSERT INTO jobs VALUES ('PU_CLERK', 'Purchasing Clerk', 2500, 5500);
INSERT INTO jobs VALUES ('ST_MAN', 'Stock Manager', 5500, 8500);
INSERT INTO jobs VALUES ('ST_CLERK', 'Stock Clerk', 2000, 5000);
INSERT INTO jobs VALUES ('SH_CLERK', 'Shipping Clerk', 2500, 5500);
INSERT INTO jobs VALUES ('IT_PROG', 'Programmer', 4000, 10000);
INSERT INTO jobs VALUES ('MK_MAN', 'Marketing Manager', 9000, 15000);
INSERT INTO jobs VALUES ('MK_REP', 'Marketing Representative', 4000, 9000);
INSERT INTO jobs VALUES ('HR_REP', 'Human Resources Representative', 4000, 9000);
INSERT INTO jobs VALUES ('PR_REP', 'Public Relations Representative', 4500, 10500);

-- Populate employees
INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Steven', 'King', 'SKING', '515.123.4567', TO_DATE('17-JUN-1987', 'DD-MON-YYYY'), 'AD_PRES', 24000, NULL, NULL, 90);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', TO_DATE('21-SEP-1989', 'DD-MON-YYYY'), 'AD_VP', 17000, NULL, 100, 90);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Lex', 'De Haan', 'LDEHAAN', '515.123.4569', TO_DATE('13-JAN-1993', 'DD-MON-YYYY'), 'AD_VP', 17000, NULL, 100, 90);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', TO_DATE('03-JAN-1990', 'DD-MON-YYYY'), 'IT_PROG', 9000, NULL, 102, 60);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Bruce', 'Ernst', 'BERNST', '590.423.4568', TO_DATE('21-MAY-1991', 'DD-MON-YYYY'), 'IT_PROG', 6000, NULL, 103, 60);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('David', 'Austin', 'DAUSTIN', '590.423.4569', TO_DATE('25-JUN-1997', 'DD-MON-YYYY'), 'IT_PROG', 4800, NULL, 103, 60);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Valli', 'Pataballa', 'VPATABAL', '590.423.4560', TO_DATE('05-FEB-1998', 'DD-MON-YYYY'), 'IT_PROG', 4800, NULL, 103, 60);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Diana', 'Lorentz', 'DLORENTZ', '590.423.5567', TO_DATE('07-FEB-1999', 'DD-MON-YYYY'), 'IT_PROG', 4200, NULL, 103, 60);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Nancy', 'Greenberg', 'NGREENBE', '515.124.4569', TO_DATE('17-AUG-1994', 'DD-MON-YYYY'), 'FI_MGR', 12000, NULL, 101, 100);

INSERT INTO employees (first_name, last_name, email, phone_number, hire_date, job_id, salary, commission_pct, manager_id, department_id)
VALUES ('Daniel', 'Faviet', 'DFAVIET', '515.124.4169', TO_DATE('16-AUG-1994', 'DD-MON-YYYY'), 'FI_ACCOUNT', 9000, NULL, 108, 100);

-- Commit all changes
COMMIT;

-- Exit
EXIT; 
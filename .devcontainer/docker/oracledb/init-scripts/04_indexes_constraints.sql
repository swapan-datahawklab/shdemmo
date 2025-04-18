-- Connect as HR user
ALTER SESSION SET CURRENT_SCHEMA = HR;

-- Create sequences
CREATE SEQUENCE locations_seq
    START WITH 3300
    INCREMENT BY 100
    MAXVALUE 9900
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE departments_seq
    START WITH 280
    INCREMENT BY 10
    MAXVALUE 9990
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE employees_seq
    START WITH 207
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Add primary key constraints
ALTER TABLE regions ADD CONSTRAINT reg_id_pk PRIMARY KEY (region_id);
ALTER TABLE countries ADD CONSTRAINT country_id_pk PRIMARY KEY (country_id);
ALTER TABLE locations ADD CONSTRAINT loc_id_pk PRIMARY KEY (location_id);
ALTER TABLE departments ADD CONSTRAINT dept_id_pk PRIMARY KEY (department_id);
ALTER TABLE jobs ADD CONSTRAINT job_id_pk PRIMARY KEY (job_id);
ALTER TABLE employees ADD CONSTRAINT emp_id_pk PRIMARY KEY (employee_id);
ALTER TABLE job_history ADD CONSTRAINT jhist_id_pk PRIMARY KEY (employee_id, start_date);

-- Add foreign key constraints
ALTER TABLE countries ADD CONSTRAINT countr_reg_fk 
    FOREIGN KEY (region_id) REFERENCES regions(region_id);

ALTER TABLE locations ADD CONSTRAINT loc_c_id_fk 
    FOREIGN KEY (country_id) REFERENCES countries(country_id);

ALTER TABLE departments ADD CONSTRAINT dept_loc_fk 
    FOREIGN KEY (location_id) REFERENCES locations(location_id);

ALTER TABLE departments ADD CONSTRAINT dept_mgr_fk 
    FOREIGN KEY (manager_id) REFERENCES employees(employee_id);

ALTER TABLE employees ADD CONSTRAINT emp_dept_fk 
    FOREIGN KEY (department_id) REFERENCES departments(department_id);

ALTER TABLE employees ADD CONSTRAINT emp_job_fk 
    FOREIGN KEY (job_id) REFERENCES jobs(job_id);

ALTER TABLE employees ADD CONSTRAINT emp_manager_fk 
    FOREIGN KEY (manager_id) REFERENCES employees(employee_id);

ALTER TABLE job_history ADD CONSTRAINT jhist_dept_fk 
    FOREIGN KEY (department_id) REFERENCES departments(department_id);

ALTER TABLE job_history ADD CONSTRAINT jhist_emp_fk 
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id);

ALTER TABLE job_history ADD CONSTRAINT jhist_job_fk 
    FOREIGN KEY (job_id) REFERENCES jobs(job_id);

-- Add check constraints
ALTER TABLE employees ADD CONSTRAINT emp_salary_min
    CHECK (salary > 0);

ALTER TABLE job_history ADD CONSTRAINT jhist_date_interval
    CHECK (end_date > start_date);

-- Create indexes
CREATE INDEX emp_department_ix ON employees (department_id);
CREATE INDEX emp_job_ix ON employees (job_id);
CREATE INDEX emp_manager_ix ON employees (manager_id);
CREATE INDEX emp_name_ix ON employees (last_name, first_name);
CREATE INDEX dept_location_ix ON departments (location_id);
CREATE INDEX loc_city_ix ON locations (city);
CREATE INDEX loc_country_ix ON locations (country_id);
CREATE INDEX loc_state_province_ix ON locations (state_province);
CREATE INDEX jhist_department_ix ON job_history (department_id);
CREATE INDEX jhist_employee_ix ON job_history (employee_id);
CREATE INDEX jhist_job_ix ON job_history (job_id);

-- Add unique constraints
ALTER TABLE departments ADD CONSTRAINT dept_name_uk UNIQUE (department_name);
ALTER TABLE jobs ADD CONSTRAINT job_title_uk UNIQUE (job_title);
ALTER TABLE employees ADD CONSTRAINT emp_email_uk UNIQUE (email); 
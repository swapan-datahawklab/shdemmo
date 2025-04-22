CREATE OR REPLACE PROCEDURE get_employee_info(p_emp_id IN NUMBER, p_result OUT VARCHAR2) AS 
BEGIN 
  SELECT first_name || ' ' || last_name INTO p_result 
  FROM employees WHERE employee_id = p_emp_id; 
END;
/

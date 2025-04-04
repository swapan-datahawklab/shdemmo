package com.example.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Types;

/**
 * Example demonstrating how to use the OracleStoredProcRunner.
 */
public class StoredProcExample {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: StoredProcExample <host:port/service> <username> <password>");
            System.exit(1);
        }

        String url = "jdbc:oracle:thin:@" + args[0];
        String username = args[1];
        String password = args[2];

        try (Connection conn = DriverManager.getConnection(url, username, password);
             OracleStoredProcRunner runner = new OracleStoredProcRunner(conn)) {
            
            // Example 1: Calling a stored procedure with IN and OUT parameters
            System.out.println("\nExample 1: Calling stored procedure");
            runner.executeProcedure(
                "UPDATE_EMPLOYEE_SALARY",
                OracleStoredProcRunner.ProcedureParam.in("p_emp_id", Types.NUMERIC, 101),
                OracleStoredProcRunner.ProcedureParam.in("p_percentage", Types.NUMERIC, 10.5),
                OracleStoredProcRunner.ProcedureParam.out("p_new_salary", Types.NUMERIC)
            );

            // Example 2: Calling a function that returns a value
            System.out.println("\nExample 2: Calling function");
            Object result = runner.executeFunction(
                "GET_DEPARTMENT_BUDGET",
                Types.NUMERIC,
                OracleStoredProcRunner.ProcedureParam.in("p_dept_id", Types.NUMERIC, 20)
            );
            System.out.printf("Department budget: %s%n", result);
            
            // Example 3: Procedure with INOUT parameter
            System.out.println("\nExample 3: Procedure with INOUT parameter");
            runner.executeProcedure(
                "CALCULATE_BONUS",
                OracleStoredProcRunner.ProcedureParam.in("p_emp_id", Types.NUMERIC, 101),
                OracleStoredProcRunner.ProcedureParam.inOut("p_bonus_amount", Types.NUMERIC, 1000.0)
            );

        } catch (Exception e) {
            System.err.println("Error executing stored procedures/functions:");
            e.printStackTrace();
            System.exit(1);
        }
    }
} 
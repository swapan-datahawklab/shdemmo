package com.example.oracle.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

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
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            OracleStoredProcRunner runner = new OracleStoredProcRunner(args[0], username, password, false, 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), true);
            
            // Example 1: Calling a stored procedure with IN and OUT parameters
            System.out.println("\nExample 1: Calling stored procedure");
            java.util.List<OracleStoredProcRunner.ProcedureParam> inputParams = new ArrayList<>();
            inputParams.add(new OracleStoredProcRunner.ProcedureParam("p_emp_id", "NUMERIC", 101));
            inputParams.add(new OracleStoredProcRunner.ProcedureParam("p_percentage", "NUMERIC", 10.5));
            java.util.List<OracleStoredProcRunner.ProcedureParam> outputParams = new ArrayList<>();
            outputParams.add(new OracleStoredProcRunner.ProcedureParam("p_new_salary", "NUMERIC", null));
            
            runner = new OracleStoredProcRunner(args[0], username, password, false, 
                inputParams, outputParams, new ArrayList<>(), true);
            runner.execute("UPDATE_EMPLOYEE_SALARY");

            // Example 2: Calling a function that returns a value
            System.out.println("\nExample 2: Calling function");
            java.util.List<OracleStoredProcRunner.ProcedureParam> inputParams2 = new ArrayList<>();
            inputParams2.add(new OracleStoredProcRunner.ProcedureParam("p_dept_id", "NUMERIC", 20));
            java.util.List<OracleStoredProcRunner.ProcedureParam> outputParams2 = new ArrayList<>();
            outputParams2.add(new OracleStoredProcRunner.ProcedureParam("result", "NUMERIC", null));
            
            runner = new OracleStoredProcRunner(args[0], username, password, false, 
                inputParams2, outputParams2, new ArrayList<>(), true);
            Object result = runner.execute("GET_DEPARTMENT_BUDGET");
            System.out.printf("Department budget: %s%n", result);
            // Example 3: Procedure with INOUT parameter
            System.out.println("\nExample 3: Procedure with INOUT parameter");
            java.util.List<OracleStoredProcRunner.ProcedureParam> inputParams3 = new ArrayList<>();
            inputParams3.add(new OracleStoredProcRunner.ProcedureParam("p_emp_id", "NUMERIC", 101));
            java.util.List<OracleStoredProcRunner.ProcedureParam> inOutParams = new ArrayList<>();
            inOutParams.add(new OracleStoredProcRunner.ProcedureParam("p_bonus_amount", "NUMERIC", 1000.0));
            
            runner = new OracleStoredProcRunner(args[0], username, password, false, 
                inputParams3, new ArrayList<>(), inOutParams, true);
            runner.execute("CALCULATE_BONUS");

        } catch (Exception e) {
            System.err.println("Error executing stored procedures/functions:");
            e.printStackTrace();
            System.exit(1);
        }
    }
} 
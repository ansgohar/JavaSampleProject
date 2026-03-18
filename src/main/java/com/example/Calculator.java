package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Calculator class with INTENTIONAL code quality issues for SonarQube demonstration.
 * 
 * This class contains various security vulnerabilities, bugs, and code smells:
 * - Security: Hardcoded credentials, SQL injection, weak cryptography
 * - Bugs: Null pointer risks, resource leaks, division by zero
 * - Code Smells: Unused methods, code duplication, high complexity
 * - Coverage: Insufficient test coverage
 * 
 * DO NOT USE THIS CODE IN PRODUCTION!
 */
public class Calculator {
    
    // SECURITY ISSUE: Hardcoded credentials (Critical)
    // OWASP A2 - Broken Authentication
    private static final String DB_PASSWORD = "hardcoded123";
    private static final String API_KEY = "sk_test_4eC39HqLyjWDarjtT1zdp7dc";
    
    // CODE SMELL: Unused private field
    private int unusedField = 42;
    
    /**
     * Basic addition - This method is tested
     */
    public int add(int a, int b) {
        return a + b;
    }
    
    /**
     * Basic subtraction - This method is tested
     */
    public int subtract(int a, int b) {
        return a - b;
    }
    
    /**
     * BUG: Division without zero check (High)
     * This method will throw ArithmeticException if b is zero
     * ALSO: Not covered by tests (Coverage issue)
     */
    public int divide(int a, int b) {
        return a / b; // No validation!
    }
    
    /**
     * Basic multiplication - Partially tested
     */
    public int multiply(int a, int b) {
        return a * b;
    }
    
    /**
     * CODE SMELL: High Cyclomatic Complexity (Major)
     * Deeply nested conditions make this hard to maintain
     */
    public int complexCalculation(int a, int b, int c, String operation) {
        // CODE SMELL: Magic number 100
        if (a > 100) {
            if (operation.equals("add")) {
                if (b > 0) {
                    if (c > 0) {
                        return a + b + c;
                    } else {
                        return a + b;
                    }
                } else {
                    if (c > 0) {
                        return a + c;
                    } else {
                        return a;
                    }
                }
            } else if (operation.equals("multiply")) {
                if (b > 0) {
                    if (c > 0) {
                        return a * b * c;
                    } else {
                        return a * b;
                    }
                } else {
                    return a;
                }
            }
        }
        return 0;
    }
    
    /**
     * SECURITY ISSUE: SQL Injection vulnerability (Critical)
     * OWASP A1 - Injection
     * CWE-89: SQL Injection
     */
    public void getUserById(Connection conn, String userId) throws SQLException {
        // Direct string concatenation allows SQL injection
        String sql = "SELECT * FROM users WHERE id=" + userId;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        
        // BUG: Resource leak - ResultSet and Statement not closed
        // Should use try-with-resources
    }
    
    /**
     * SECURITY ISSUE: Weak cryptography (Major)
     * CWE-327: Use of a Broken or Risky Cryptographic Algorithm
     */
    public String hashPassword(String password) throws NoSuchAlgorithmException {
        // MD5 is cryptographically broken
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(password.getBytes());
        
        // CODE SMELL: Code duplication with hashData method
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * CODE SMELL: Code duplication with hashPassword method
     * This logic should be extracted to a shared method
     */
    public String hashData(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(data.getBytes());
        
        // Duplicated code from hashPassword
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * BUG: Resource leak (Major)
     * FileInputStream not closed properly
     */
    public byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        // BUG: fis not closed - should use try-with-resources
        return data;
    }
    
    /**
     * CODE SMELL: Unused private method
     * This method is never called
     */
    private void unusedMethod() {
        System.out.println("This method is never used");
    }
    
    /**
     * CODE SMELL: Method with TODO comment
     * TODO: Implement proper error handling
     */
    public int processData(int value) {
        // CODE SMELL: Magic number 50
        if (value > 50) {
            return value * 2;
        }
        return value;
    }
    
    /**
     * BUG: Potential NullPointerException (Major)
     * No null check on input string
     */
    public int countWords(String text) {
        // BUG: Will throw NullPointerException if text is null
        return text.split("\\s+").length;
    }
    
    /**
     * CODE SMELL: Duplicate string literals
     */
    public String getStatus(int code) {
        if (code == 200) {
            return "OK";
        } else if (code == 404) {
            return "Not Found";
        } else if (code == 500) {
            return "Internal Server Error";
        }
        return "Unknown";
    }
    
    /**
     * Get database password - SECURITY ISSUE
     * This exposes the hardcoded password
     */
    public String getDatabasePassword() {
        return DB_PASSWORD;
    }
    
    /**
     * Get API key - SECURITY ISSUE
     * This exposes the hardcoded API key
     */
    public String getApiKey() {
        return API_KEY;
    }
}
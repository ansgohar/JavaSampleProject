package com.example;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Test class for Calculator
 * 
 * NOTE: This test class INTENTIONALLY has low coverage (~65%)
 * to demonstrate quality gate failures
 * 
 * Covered methods:
 * - add()
 * - subtract()
 * - multiply() (partially)
 * 
 * NOT covered methods (to trigger quality gate failures):
 * - divide()
 * - complexCalculation()
 * - getUserById()
 * - hashPassword()
 * - hashData()
 * - readFile()
 * - processData()
 * - countWords()
 * - getStatus()
 */
public class CalculatorTest {
    
    private Calculator calculator;
    
    @Before
    public void setUp() {
        calculator = new Calculator();
    }
    
    @Test
    public void testAdd() {
        assertEquals(5, calculator.add(2, 3));
        assertEquals(0, calculator.add(0, 0));
        assertEquals(-1, calculator.add(-3, 2));
    }
    
    @Test
    public void testSubtract() {
        assertEquals(1, calculator.subtract(3, 2));
        assertEquals(0, calculator.subtract(5, 5));
        assertEquals(-5, calculator.subtract(2, 7));
    }
    
    @Test
    public void testMultiply() {
        assertEquals(6, calculator.multiply(2, 3));
        assertEquals(0, calculator.multiply(5, 0));
        // Note: Negative numbers not tested (partial coverage)
    }
    
    // NOTE: divide() method is NOT tested
    // This will cause coverage to be low and quality gate to fail
    
    // NOTE: complexCalculation() method is NOT tested
    // This contributes to low coverage
    
    // NOTE: Security methods (getUserById, hashPassword, etc.) are NOT tested
    // This contributes to low coverage
    
    // NOTE: processData() method is NOT tested
    // This contributes to low coverage
    
    // NOTE: countWords() method is NOT tested
    // This contributes to low coverage
    
    // NOTE: getStatus() method is NOT tested
    // This contributes to low coverage
}
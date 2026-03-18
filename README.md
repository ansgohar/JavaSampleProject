# Java Sample Project - SonarQube Quality Gates Demo

This is a Maven-based Java project with **intentional code quality issues** to demonstrate SonarQube quality gates.

## 🎯 Purpose

Demonstrate how different quality gate levels catch various types of issues:
- 🔒 Security vulnerabilities
- 🐛 Bugs and reliability issues
- 💩 Code smells and maintainability issues
- 📊 Test coverage gaps

## 🚀 Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- SonarQube server running
- SonarScanner configured

### Running the Analysis

```bash
# Build and run tests
mvn clean verify

# Run SonarQube analysis
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin \
  -Dsonar.qualitygate.wait=true
```

### Using Specific Quality Gate

```bash
# Analyze with Basic quality gate
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin \
  -Dsonar.qualitygate=basic-quality-gate

# Analyze with Standard quality gate (default)
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin \
  -Dsonar.qualitygate=standard-quality-gate
```

## 🐛 Intentional Issues

### Security Vulnerabilities (Critical)

1. **Hardcoded Credentials**
   ```java
   private static final String PASSWORD = "hardcoded123";
   ```
   - **Severity**: Critical
   - **OWASP**: A2 - Broken Authentication
   - **Fix**: Use environment variables or secure vault

2. **SQL Injection**
   ```java
   String sql = "SELECT * FROM users WHERE id=" + userId;
   ```
   - **Severity**: Critical
   - **OWASP**: A1 - Injection
   - **Fix**: Use prepared statements

3. **Weak Cryptography**
   ```java
   MessageDigest.getInstance("MD5");
   ```
   - **Severity**: Major
   - **CWE**: CWE-327
   - **Fix**: Use SHA-256 or stronger

### Bugs (High Priority)

4. **Potential NullPointerException**
   ```java
   public int divide(int a, int b) {
       return a / b; // No zero check
   }
   ```
   - **Fix**: Add validation for b != 0

5. **Resource Leak**
   ```java
   FileInputStream fis = new FileInputStream(file);
   // Not closed in finally or try-with-resources
   ```
   - **Fix**: Use try-with-resources

### Code Smells

6. **Unused Private Method**
   ```java
   private void unusedMethod() { }
   ```

7. **Code Duplication**
   - Multiple similar methods with duplicated logic

8. **High Cyclomatic Complexity**
   ```java
   public int complexCalculation(...) {
       // Deeply nested if statements
   }
   ```

9. **Magic Numbers**
   ```java
   if (value > 100) { ... }
   ```

10. **TODO Comments**
    ```java
    // TODO: Implement proper error handling
    ```

### Coverage Issues

- **Current Coverage**: ~65%
- **Untested Methods**: divide(), processData()
- **Missing Edge Cases**: Error handling, boundary conditions

## 📊 Expected Results

### With Basic Quality Gate
- ❌ **FAIL**
- Issues: ~12 code smells
- Reason: Multiple minor issues detected

### With Standard Quality Gate
- ❌ **FAIL**
- Issues: 3 security hotspots, coverage 65% (< 80%)
- Reason: Security issues and insufficient coverage

### With Strict Quality Gate
- ❌ **FAIL**
- Issues: 2 critical vulnerabilities, coverage < 90%
- Reason: Critical security vulnerabilities

### With Enterprise Quality Gate
- ❌ **FAIL**
- Issues: Multiple vulnerabilities, maintainability C
- Reason: Any vulnerability fails enterprise gate

## 🔧 How to Fix Issues

### Step 1: Fix Critical Security Issues

```java
// Before: Hardcoded password
private static final String PASSWORD = "hardcoded123";

// After: Use environment variable
private static final String PASSWORD = System.getenv("DB_PASSWORD");
```

```java
// Before: SQL injection
String sql = "SELECT * FROM users WHERE id=" + userId;

// After: Use PreparedStatement
String sql = "SELECT * FROM users WHERE id=?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setInt(1, userId);
```

### Step 2: Fix Bugs

```java
// Before: No validation
public int divide(int a, int b) {
    return a / b;
}

// After: Add validation
public int divide(int a, int b) {
    if (b == 0) {
        throw new IllegalArgumentException("Division by zero");
    }
    return a / b;
}
```

### Step 3: Improve Code Quality

- Remove unused methods
- Extract duplicated code into shared methods
- Reduce method complexity
- Add proper error handling

### Step 4: Increase Test Coverage

```java
@Test
public void testDivide() {
    Calculator calc = new Calculator();
    assertEquals(5, calc.divide(10, 2));
}

@Test(expected = IllegalArgumentException.class)
public void testDivideByZero() {
    Calculator calc = new Calculator();
    calc.divide(10, 0);
}
```

## ✅ After Fixes

Once all issues are fixed:

```bash
# Re-run analysis
mvn clean verify sonar:sonar

# Expected: ✅ PASS (for Basic and Standard gates)
# Coverage should be > 80%
# No critical vulnerabilities
# Minimal code smells
```

## 🔗 Integration with Jenkins

Create a `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    tools {
        maven 'Maven 3.8'
        jdk 'JDK 11'
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time:
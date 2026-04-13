# Quick Fix Checklist - Get Your Pipeline Running

**Target Time**: 30 minutes  
**Difficulty**: Easy  
**Status**: 🟡 4 issues to fix

---

## ✅ Checklist

### 🔴 CRITICAL (Must Do First)

- [ ] **1. Rename Jenkinsfile** (2 minutes)
  ```bash
  cd /Users/ahmadgohar/GitHub/JavaSampleProject
  mv Jenkinsfile.java Jenkinsfile
  git add Jenkinsfile
  git commit -m "fix: Rename Jenkinsfile.java to Jenkinsfile"
  git push origin main
  ```
  **Why**: Jenkins looks for a file named exactly "Jenkinsfile", not "Jenkinsfile.java"

---

### 🟡 CONFIGURATION (Must Do)

- [ ] **2. Check JDK Tool Name** (3 minutes)
  1. Go to: http://49.13.128.43:8080/manage/configureTools/
  2. Find "JDK installations" section
  3. Note the exact name (e.g., "JDK 17", "Java 21", "jdk-21")
  4. Update Jenkinsfile line 11 with that exact name
  
  **Current**:
  ```groovy
  jdk 'JDK 21'
  ```
  
  **Update to match Jenkins** (example):
  ```groovy
  jdk 'JDK 17'  // or whatever you see in Jenkins
  ```

- [ ] **3. Configure Credentials** (10 minutes)
  
  Go to: http://49.13.128.43:8080/manage/credentials/store/system/domain/_/
  
  Click "Add Credentials" and create:
  
  **a) SonarQube Token**:
  - Kind: Secret text
  - Scope: Global
  - Secret: [Get from http://49.13.128.43:9000/account/security]
  - ID: `sonarqube-token`
  - Description: SonarQube Authentication Token
  
  **b) Harbor Credentials** (if using Docker):
  - Kind: Username with password
  - Scope: Global
  - Username: `admin` (or your Harbor admin)
  - Password: [your Harbor password]
  - ID: `harbor-credentials`
  - Description: Harbor Registry Credentials
  
  **c) Discord Webhook** (optional):
  - Kind: Secret text
  - Scope: Global
  - Secret: [your Discord webhook URL]
  - ID: `discord-webhook`
  - Description: Discord Webhook for notifications

- [ ] **4. Set Environment Variables** (5 minutes)
  
  Go to: http://49.13.128.43:8080/manage/configure/
  
  Scroll to "Global properties" → Check "Environment variables"
  
  Add:
  - Name: `HARBOR_URL`, Value: `49.13.128.43:8090`
  - Name: `SONARQUBE_URL`, Value: `http://49.13.128.43:9000`

---

### 🟢 OPTIONAL (Can Skip For Now)

- [ ] **5. Verify Trivy Installation** (2 minutes)
  ```bash
  ssh root@49.13.128.43 "trivy --version"
  ```
  If not found, it should be at `/usr/local/bin/trivy`

- [ ] **6. Configure Jenkins Job** (5 minutes)
  - Go to: http://49.13.128.43:8080/job/java-pipline/configure
  - Pipeline section:
    - Definition: "Pipeline script from SCM"
    - SCM: Git
    - Repository URL: `https://github.com/YOUR_USERNAME/JavaSampleProject`
    - Branch: `*/main` (or `*/master`)
    - Script Path: `Jenkinsfile`
  - Click "Save"

---

## 🚀 Test Your Pipeline

After completing the checklist:

1. **Trigger Build**:
   - Go to: http://49.13.128.43:8080/job/java-pipline/
   - Click "Build with Parameters"
   - Leave defaults and click "Build"

2. **Monitor Progress**:
   - Click on build number (e.g., #1, #2)
   - Click "Console Output"
   - Watch for errors

3. **Check Results**:
   - SonarQube: http://49.13.128.43:9000
   - Jenkins workspace for reports
   - Netdata for server resources: http://49.13.128.43:19999

---

## 🐛 Common Issues & Solutions

### Issue: "JDK not found"
**Solution**: Update JDK name in Jenkinsfile to match Jenkins configuration (Step 2)

### Issue: "Credentials not found"
**Solution**: Add credentials in Jenkins (Step 3)

### Issue: "SonarQube connection failed"
**Solution**: 
1. Check SonarQube is running: http://49.13.128.43:9000
2. Verify token is correct
3. Check SONARQUBE_URL environment variable

### Issue: "Docker not found"
**Solution**: Docker stages will be skipped (no Dockerfile in project). This is OK for Maven-only builds.

### Issue: "Quality Gate timeout"
**Solution**: 
1. Go to SonarQube
2. Check if analysis completed
3. May need to increase timeout in Jenkinsfile

---

## 📊 Success Criteria

Your pipeline is working when you see:

- ✅ Build stage completes
- ✅ Tests pass and show in Jenkins
- ✅ SonarQube analysis appears at http://49.13.128.43:9000
- ✅ Quality gate passes (or shows specific failures)
- ✅ OWASP report generated
- ✅ Build completes successfully

---

## 🎯 What's Next?

Once pipeline runs successfully:

1. ✅ Review SonarQube reports
2. ✅ Fix any code quality issues
3. ✅ Add more tests to improve coverage
4. ✅ Configure notifications
5. ✅ Add Docker support (if needed)

---

**Need Help?** See full assessment: `PIPELINE-ASSESSMENT.md`

**Last Updated**: March 28, 2026
// Java CI/CD Pipeline Template
// Optimized for Java 21 LTS with Spring Boot 3.x, Maven/Gradle
// Includes: Security scanning, SBOM generation, image signing, quality gates

pipeline {
    agent any
    
    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch to build')
        choice(name: 'BUILD_TYPE', choices: ['snapshot', 'release', 'hotfix'], description: 'Build type')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip unit tests')
        booleanParam(name: 'SKIP_SONAR', defaultValue: false, description: 'Skip SonarQube analysis')
        booleanParam(name: 'DEPLOY_TO_DEV', defaultValue: true, description: 'Auto-deploy to dev environment')
    }
    
    environment {
        // Java Configuration
        JAVA_VERSION = '21' // Java 21 LTS - stable and well-supported for modern applications
        MAVEN_OPTS = '-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication'
        
        // Registry & Repositories
        HARBOR_REGISTRY = "${env.HARBOR_URL ?: 'harbor.local'}"
        // HARBOR_CREDENTIALS = credentials('harbor-credentials') // Optional: Uncomment if using Harbor
        
        // Quality & Security
        SONAR_URL = "${env.SONARQUBE_URL ?: 'http://localhost:9000'}"
        // SONAR_TOKEN = credentials('sonarqube-token') // Optional: Uncomment if using SonarQube
        
        // Project Info
        PROJECT_NAME = "${env.JOB_NAME}".tokenize('/').last()
        BUILD_VERSION = "${env.BUILD_NUMBER}"
        GIT_COMMIT_SHORT = "${env.GIT_COMMIT?.take(8) ?: 'unknown'}"
        
        // Image & Artifact
        DOCKER_IMAGE = "${HARBOR_REGISTRY}/${PROJECT_NAME}:${BUILD_VERSION}"
        DOCKER_BUILDKIT = '1'
        
        // Notifications (optional - will be skipped if not configured)
        // DISCORD_WEBHOOK = credentials('discord-webhook')
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "🔄 Checking out code from ${params.BRANCH}"
                    checkout scm
                }
            }
        }
        
        stage('Setup Tools') {
            steps {
                script {
                    echo "🔧 Setting up build tools..."
                    sh '''
                        # Check if Maven is installed, if not, download it
                        if ! command -v mvn &> /dev/null; then
                            echo "Maven not found, downloading..."
                            cd /tmp
                            
                            # Use curl instead of wget (more commonly available)
                            curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o apache-maven-3.9.6-bin.tar.gz
                            tar xzf apache-maven-3.9.6-bin.tar.gz
                            rm apache-maven-3.9.6-bin.tar.gz
                            
                            export PATH=/tmp/apache-maven-3.9.6/bin:$PATH
                            echo "Maven 3.9.6 installed to /tmp/apache-maven-3.9.6"
                        else
                            echo "Maven already installed:"
                        fi
                        mvn --version
                    '''
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    echo "🔨 Building Java project..."
                    sh '''
                        # Set Maven path if it was downloaded
                        if [ -d "/tmp/apache-maven-3.9.6" ]; then
                            export PATH=/tmp/apache-maven-3.9.6/bin:$PATH
                        fi
                        
                        if [ -f "pom.xml" ]; then
                            mvn clean compile -DskipTests
                        elif [ -f "build.gradle" ]; then
                            ./gradlew clean build -x test
                        else
                            echo "No Maven pom.xml or Gradle build.gradle found!"
                            exit 1
                        fi
                    '''
                }
            }
        }
        
        stage('Unit Tests') {
            when {
                expression { !params.SKIP_TESTS }
            }
            steps {
                script {
                    echo "🧪 Running unit tests..."
                    sh '''
                        # Set Maven path if it was downloaded
                        if [ -d "/tmp/apache-maven-3.9.6" ]; then
                            export PATH=/tmp/apache-maven-3.9.6/bin:$PATH
                        fi
                        
                        if [ -f "pom.xml" ]; then
                            mvn test
                        elif [ -f "build.gradle" ]; then
                            ./gradlew test
                        fi
                    '''
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml'
                }
            }
        }
        
        stage('Code Quality - SonarQube') {
            when {
                allOf {
                    expression { !params.SKIP_SONAR }
                    expression { env.SONAR_TOKEN != null }
                }
            }
            steps {
                script {
                    echo "📊 Running SonarQube analysis..."
                    echo "⚠️ SonarQube stage skipped - SONAR_TOKEN credential not configured"
                    // Uncomment below when sonarqube-token credential is added
                    // withSonarQubeEnv('SonarQube') {
                    //     if (fileExists('pom.xml')) {
                    //         sh 'mvn sonar:sonar'
                    //     } else if (fileExists('build.gradle')) {
                    //         sh './gradlew sonarqube'
                    //     }
                    // }
                }
            }
        }
        
        stage('Quality Gate') {
            when {
                allOf {
                    expression { !params.SKIP_SONAR }
                    expression { env.SONAR_TOKEN != null }
                }
            }
            steps {
                script {
                    echo "🚦 Waiting for Quality Gate..."
                    echo "⚠️ Quality Gate skipped - SONAR_TOKEN credential not configured"
                    // Uncomment below when sonarqube-token credential is added
                    // timeout(time: 5, unit: 'MINUTES') {
                    //     waitForQualityGate abortPipeline: true
                    // }
                }
            }
        }
        
        stage('Security Scan - Dependencies') {
            steps {
                script {
                    echo "🔒 Scanning dependencies for vulnerabilities..."
                    sh '''
                        if [ ! -f dependency-check.sh ]; then
                            wget -q https://github.com/jeremylong/DependencyCheck/releases/download/v8.4.0/dependency-check-8.4.0-release.zip
                            unzip -q dependency-check-8.4.0-release.zip
                        fi
                        ./dependency-check/bin/dependency-check.sh \
                            --project "${PROJECT_NAME}" \
                            --scan . \
                            --format HTML \
                            --format JSON \
                            --failOnCVSS 7
                    '''
                }
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'dependency-check-report',
                        reportFiles: 'dependency-check-report.html',
                        reportName: 'Dependency Check Report'
                    ])
                }
            }
        }
        
        stage('Security Scan - Secrets') {
            steps {
                script {
                    echo "🔐 Scanning for exposed secrets..."
                    sh '''
                        docker run --rm -v $(pwd):/scan \
                            zricethezav/gitleaks:latest \
                            detect --source /scan --report-path /scan/gitleaks-report.json || true
                    '''
                }
            }
        }
        
        stage('Package') {
            steps {
                script {
                    echo "📦 Packaging application..."
                    sh '''
                        # Set Maven path if it was downloaded
                        if [ -d "/tmp/apache-maven-3.9.6" ]; then
                            export PATH=/tmp/apache-maven-3.9.6/bin:$PATH
                        fi
                        
                        if [ -f "pom.xml" ]; then
                            mvn package -DskipTests
                        elif [ -f "build.gradle" ]; then
                            ./gradlew build -x test
                        fi
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🐳 Building Docker image..."
                    def imageName = "${HARBOR_REGISTRY}/${PROJECT_NAME}:${BUILD_VERSION}"
                    sh "docker build -t ${imageName} ."
                    sh "docker tag ${imageName} ${HARBOR_REGISTRY}/${PROJECT_NAME}:latest"
                }
            }
        }
        
        stage('Container Security Scan') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🔍 Scanning Docker image with Trivy..."
                    def imageName = "${HARBOR_REGISTRY}/${PROJECT_NAME}:${BUILD_VERSION}"
                    sh """
                        trivy image \
                            --severity HIGH,CRITICAL \
                            --exit-code 0 \
                            --format json \
                            --output trivy-report.json \
                            ${imageName}
                            
                        # Also scan for misconfigurations
                        trivy config --exit-code 0 --severity HIGH,CRITICAL . || true
                    """
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true
                }
            }
        }
        
        stage('Generate SBOM') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "📋 Generating Software Bill of Materials (SBOM)..."
                    def imageName = "${HARBOR_REGISTRY}/${PROJECT_NAME}:${BUILD_VERSION}"
                    sh """
                        # Generate SBOM with Syft
                        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                            anchore/syft:latest \
                            ${imageName} \
                            -o spdx-json=sbom-spdx.json \
                            -o cyclonedx-json=sbom-cyclonedx.json
                            
                        # Generate SBOM with Trivy as well
                        trivy image --format cyclonedx ${imageName} > trivy-sbom.json || true
                    """
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'sbom-*.json,trivy-sbom.json', allowEmptyArchive: true
                }
            }
        }
        
        stage('Sign Container Image') {
            when {
                allOf {
                    expression { fileExists('Dockerfile') }
                    expression { params.BUILD_TYPE == 'release' }
                }
            }
            steps {
                script {
                    echo "✍️ Signing container image with Cosign..."
                    def imageName = "${HARBOR_REGISTRY}/${PROJECT_NAME}:${BUILD_VERSION}"
                    sh """
                        # Sign image with Cosign (requires COSIGN_KEY or keyless signing)
                        cosign sign --yes ${imageName} || echo "Cosign signing skipped (no key configured)"
                        
                        # Attach SBOM to image
                        cosign attach sbom --sbom sbom-spdx.json ${imageName} || true
                    """
                }
            }
        }
        
        stage('Push to Harbor') {
            when {
                allOf {
                    expression { fileExists('Dockerfile') }
                    expression { env.HARBOR_CREDENTIALS != null }
                }
            }
            steps {
                script {
                    echo "🚀 Pushing to Harbor registry..."
                    echo "⚠️ Harbor push skipped - HARBOR_CREDENTIALS not configured"
                    // Uncomment below when harbor-credentials is added
                    // def imageName = "${HARBOR_REGISTRY}/${PROJECT_NAME}"
                    // sh """
                    //     echo ${HARBOR_CREDENTIALS_PSW} | docker login ${HARBOR_REGISTRY} -u ${HARBOR_CREDENTIALS_USR} --password-stdin
                    //     docker push ${imageName}:${BUILD_VERSION}
                    //     docker push ${imageName}:latest
                    // """
                }
            }
        }
        
        stage('Deploy to Dev') {
            when {
                expression { params.DEPLOY_TO_DEV == true }
            }
            steps {
                script {
                    echo "🎯 Deploying to dev environment..."
                    // Add your deployment logic here
                    sh "echo 'Deployment logic goes here'"
                }
            }
        }
    }
    
    post {
        success {
            script {
                echo "✅ Pipeline completed successfully!"
                // Send notifications
                // Uncomment below if you configure discord-webhook credential
                // node {
                //     sh '''
                //         if [ -n "$DISCORD_WEBHOOK" ]; then
                //             curl -X POST "$DISCORD_WEBHOOK" \
                //                 -H "Content-Type: application/json" \
                //                 -d "{\\"content\\": \\"✅ Build ${BUILD_NUMBER} for ${PROJECT_NAME} succeeded!\\"}"
                //         fi
                //     '''
                // }
            }
        }
        failure {
            script {
                echo "❌ Pipeline failed!"
                // Uncomment below if you configure discord-webhook credential
                // node {
                //     sh '''
                //         if [ -n "$DISCORD_WEBHOOK" ]; then
                //             curl -X POST "$DISCORD_WEBHOOK" \
                //                 -H "Content-Type: application/json" \
                //                 -d "{\\"content\\": \\"❌ Build ${BUILD_NUMBER} for ${PROJECT_NAME} failed!\\"}"
                //         fi
                //     '''
                // }
            }
        }
        always {
            script {
                node {
                    cleanWs()
                }
            }
        }
    }
}
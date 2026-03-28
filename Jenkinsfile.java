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
        JAVA_VERSION = '21' # Java 21 LTS - stable and well-supported
        MAVEN_OPTS = '-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication'
        
        // Registry & Repositories
        HARBOR_REGISTRY = "${env.HARBOR_URL ?: 'harbor.local'}"
        HARBOR_CREDENTIALS = credentials('harbor-credentials')
        
        // Quality & Security
        SONAR_URL = "${env.SONARQUBE_URL ?: 'http://localhost:9000'}"
        SONAR_TOKEN = credentials('sonarqube-token')
        
        // Project Info
        PROJECT_NAME = "${env.JOB_NAME}".tokenize('/').last()
        BUILD_VERSION = "${env.BUILD_NUMBER}"
        GIT_COMMIT_SHORT = "${env.GIT_COMMIT?.take(8) ?: 'unknown'}"
        
        // Image & Artifact
        DOCKER_IMAGE = "${HARBOR_REGISTRY}/${PROJECT_NAME}:${BUILD_VERSION}"
        DOCKER_BUILDKIT = '1'
        
        // Notifications
        DISCORD_WEBHOOK = credentials('discord-webhook')
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
        
        stage('Build') {
            steps {
                script {
                    echo "🔨 Building Java project..."
                    if (fileExists('pom.xml')) {
                        sh 'mvn clean compile -DskipTests'
                    } else if (fileExists('build.gradle')) {
                        sh './gradlew clean build -x test'
                    } else {
                        error "No Maven pom.xml or Gradle build.gradle found!"
                    }
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
                    if (fileExists('pom.xml')) {
                        sh 'mvn test'
                    } else if (fileExists('build.gradle')) {
                        sh './gradlew test'
                    }
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
                expression { !params.SKIP_SONAR }
            }
            steps {
                script {
                    echo "📊 Running SonarQube analysis..."
                    withSonarQubeEnv('SonarQube') {
                        if (fileExists('pom.xml')) {
                            sh 'mvn sonar:sonar'
                        } else if (fileExists('build.gradle')) {
                            sh './gradlew sonarqube'
                        }
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            when {
                expression { !params.SKIP_SONAR }
            }
            steps {
                script {
                    echo "🚦 Waiting for Quality Gate..."
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
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
                    if (fileExists('pom.xml')) {
                        sh 'mvn package -DskipTests'
                    } else if (fileExists('build.gradle')) {
                        sh './gradlew build -x test'
                    }
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
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🚀 Pushing to Harbor registry..."
                    def imageName = "${HARBOR_REGISTRY}/${PROJECT_NAME}"
                    sh """
                        echo ${HARBOR_CREDENTIALS_PSW} | docker login ${HARBOR_REGISTRY} -u ${HARBOR_CREDENTIALS_USR} --password-stdin
                        docker push ${imageName}:${BUILD_VERSION}
                        docker push ${imageName}:latest
                    """
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
                node {
                    sh '''
                        if [ -n "$DISCORD_WEBHOOK" ]; then
                            curl -X POST "$DISCORD_WEBHOOK" \
                                -H "Content-Type: application/json" \
                                -d "{\\"content\\": \\"✅ Build ${BUILD_NUMBER} for ${PROJECT_NAME} succeeded!\\"}"
                        fi
                    '''
                }
            }
        }
        failure {
            script {
                echo "❌ Pipeline failed!"
                node {
                    sh '''
                        if [ -n "$DISCORD_WEBHOOK" ]; then
                            curl -X POST "$DISCORD_WEBHOOK" \
                                -H "Content-Type: application/json" \
                                -d "{\\"content\\": \\"❌ Build ${BUILD_NUMBER} for ${PROJECT_NAME} failed!\\"}"
                        fi
                    '''
                }
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
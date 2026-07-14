pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    environment {
        IMAGE_NAME = "product-service:v1"
        CONTAINER_NAME = "product-service"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                bat 'echo JAVA_HOME=%JAVA_HOME%'
                bat 'java -version'
                bat 'mvn -version'
                bat 'docker --version'
                bat 'trivy --version'
            }
        }

        stage('Build Application') {
            steps {
                bat '''
                echo =====================================
                echo Building Spring Boot Application
                echo =====================================

                mvn clean package -DskipTests
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-auth-token', variable: 'SONAR_TOKEN')]) {

                    bat '''
                    echo =====================================
                    echo Running SonarQube Analysis
                    echo =====================================

                    mvn sonar:sonar ^
                    -Dsonar.host.url=http://localhost:9000 ^
                    -Dsonar.login=%SONAR_TOKEN% ^
                    -Dsonar.projectKey=product-service ^
                    -Dsonar.projectName="Product Service"
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {

                bat '''
                echo =====================================
                echo Building Docker Image
                echo =====================================

                docker build -t %IMAGE_NAME% .
                '''
            }
        }

        stage('Trivy Security Scan') {
            steps {

                bat '''
                if not exist reports mkdir reports

                echo =====================================
                echo Running Trivy Scan
                echo =====================================

                trivy image ^
                --severity HIGH,CRITICAL ^
                --format table ^
                -o reports\\trivy-report.txt ^
                %IMAGE_NAME%

                echo.
                echo =====================================
                echo Trivy Report
                echo =====================================

                type reports\\trivy-report.txt

                echo.
                echo Trivy Scan Completed Successfully
                '''
            }
        }

        stage('Remove Old Container') {
            steps {

                bat '''
                @echo off

                docker stop %CONTAINER_NAME% 2>nul
                docker rm %CONTAINER_NAME% 2>nul

                exit /b 0
                '''
            }
        }

        stage('Deploy Container') {
            steps {

                bat '''
                @echo off

                docker run -d ^
                --name %CONTAINER_NAME% ^
                -p 8082:8081 ^
                -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5434/product_db ^
                -e SPRING_DATASOURCE_USERNAME=postgres ^
                -e SPRING_DATASOURCE_PASSWORD=2163 ^
                %IMAGE_NAME%
                '''
            }
        }

        stage('Verify Deployment') {
            steps {

                script {

                    echo "Waiting 30 seconds for application startup..."

                    sleep(time:30, unit:'SECONDS')

                    boolean healthy = false

                    for(int i=1;i<=60;i++){

                        echo "Health Check Attempt ${i}/60"

                        int status = bat(
                            script:'curl --silent --fail http://localhost:8082/actuator/health',
                            returnStatus:true
                        )

                        if(status==0){
                            healthy=true
                            break
                        }

                        sleep(time:5, unit:'SECONDS')
                    }

                    bat 'docker ps'
                    bat 'docker logs product-service'

                    if(!healthy){
                        error("Application failed health check.")
                    }

                    echo "====================================="
                    echo "Application Deployed Successfully"
                    echo "====================================="
                }
            }
        }

    }

    post {

        success {

            echo "============================================="
            echo "BUILD SUCCESSFUL"
            echo "============================================="
            echo "Application URL : http://localhost:8082"
            echo "Swagger UI      : http://localhost:8082/swagger-ui/index.html"
            echo "Health API      : http://localhost:8082/actuator/health"
            echo "Prometheus      : http://localhost:8082/actuator/prometheus"
            echo "SonarQube       : http://localhost:9000/dashboard?id=product-service"
            echo "============================================="
        }

        failure {

            echo "============================================="
            echo "BUILD FAILED"
            echo "============================================="

            bat 'docker ps -a'
            bat 'docker logs product-service'
        }

        always {

            archiveArtifacts artifacts: 'reports/*', fingerprint: true

            cleanWs()
        }
    }
}
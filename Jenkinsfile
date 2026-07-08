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
            }
        }

        stage('Build JAR') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t %IMAGE_NAME% .'
            }
        }

        stage('Stop Old Container') {
            steps {
                bat '''
                docker stop %CONTAINER_NAME% 2>nul
                docker rm %CONTAINER_NAME% 2>nul
                exit /b 0
                '''
            }
        }

        stage('Run Docker Container') {
            steps {
                bat '''
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
                bat '''
                @echo off

                echo ==============================
                echo Running Containers
                echo ==============================
                docker ps

                echo.
                echo Waiting 30 seconds for Spring Boot...
                timeout /t 30 >nul

                echo.
                echo ==============================
                echo Container Logs
                echo ==============================
                docker logs product-service

                echo.
                echo ==============================
                echo Health Check
                echo ==============================
                curl http://localhost:8082/actuator/health

                echo.
                echo ==============================
                echo Swagger Check
                echo ==============================
                curl http://localhost:8082/swagger-ui/index.html
                '''
            }
        }
    }

    post {
        success {
            echo '======================================='
            echo 'BUILD SUCCESSFUL!'
            echo 'Application is running.'
            echo 'Swagger: http://localhost:8082/swagger-ui/index.html'
            echo 'Health : http://localhost:8082/actuator/health'
            echo '======================================='
        }

        failure {
            echo '======================================='
            echo 'BUILD FAILED!'
            echo '======================================='
        }

        always {
            cleanWs()
        }
    }
}
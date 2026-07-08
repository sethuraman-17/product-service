pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    environment {
        IMAGE_NAME = 'product-service:v1'
        CONTAINER_NAME = 'product-service'
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
                @echo off
                docker stop %CONTAINER_NAME% 2>nul
                docker rm %CONTAINER_NAME% 2>nul
                exit /b 0
                '''
            }
        }

        stage('Run Docker Container') {
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
                bat '''
                @echo off

                echo ==================================
                echo Running Containers
                echo ==================================
                docker ps

                echo.
                echo Waiting for Spring Boot to start...
                ping 127.0.0.1 -n 20 >nul

                echo.
                echo ==================================
                echo Container Logs
                echo ==================================
                docker logs %CONTAINER_NAME%

                echo.
                echo ==================================
                echo Checking Health Endpoint
                echo ==================================

                set COUNT=0

                :retry

                curl http://localhost:8082/actuator/health

                if %ERRORLEVEL% EQU 0 (
                    echo.
                    echo ==================================
                    echo APPLICATION IS UP
                    echo ==================================
                    goto success
                )

                set /a COUNT+=1

                if %COUNT% GEQ 12 (
                    echo Application failed to start.
                    exit /b 1
                )

                echo Waiting 5 seconds...
                ping 127.0.0.1 -n 6 >nul

                goto retry

                :success

                echo.
                echo ==================================
                echo Swagger
                echo ==================================
                curl http://localhost:8082/swagger-ui/index.html

                exit /b 0
                '''
            }
        }
    }

    post {

        success {
            echo '========================================'
            echo 'BUILD SUCCESSFUL!'
            echo '========================================'
            echo 'Application URL : http://localhost:8082'
            echo 'Swagger UI      : http://localhost:8082/swagger-ui/index.html'
            echo 'Health Check    : http://localhost:8082/actuator/health'
            echo '========================================'
        }

        failure {
            echo '========================================'
            echo 'BUILD FAILED!'
            echo 'Check the console output above.'
            echo '========================================'
        }

        always {
            cleanWs()
        }
    }
}
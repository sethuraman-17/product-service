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
                script {

                    int retries = 24
                    boolean healthy = false

                    for (int i = 1; i <= retries; i++) {

                        echo "Health Check Attempt ${i}/24"

                        int status = bat(
                            script: 'curl --silent --fail http://localhost:8082/actuator/health',
                            returnStatus: true
                        )

                        if (status == 0) {
                            healthy = true
                            break
                        }

                        sleep(time: 5, unit: 'SECONDS')
                    }

                    bat 'docker logs product-service'
                    bat 'docker ps'

                    if (!healthy) {
                        error("Application failed to become healthy.")
                    }

                    echo "Application is UP!"
                }
            }
        }

    }

    post {

        success {
            echo "========================================="
            echo "BUILD SUCCESSFUL!"
            echo "========================================="
            echo "Application : http://localhost:8082"
            echo "Swagger UI  : http://localhost:8082/swagger-ui/index.html"
            echo "Health      : http://localhost:8082/actuator/health"
            echo "========================================="
        }

        failure {
            echo "========================================="
            echo "BUILD FAILED!"
            echo "========================================="

            bat 'docker ps -a'
            bat 'docker logs product-service'
        }

        always {
            cleanWs()
        }
    }
}
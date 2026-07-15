pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    environment {
        IMAGE_NAME = "product-service:${BUILD_NUMBER}"
        DOCKER_IMAGE = "sethu1705/product-service:${BUILD_NUMBER}"
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

        stage('Verify Docker Login') {
            steps {
                bat '''
                echo =====================================
                echo Verifying Docker Installation
                echo =====================================
                docker --version
                docker images
                docker info
                '''
            }
        }

        stage('Build Application') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-auth-token', variable: 'SONAR_TOKEN')]) {
                    bat '''
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
                bat 'docker build -t %IMAGE_NAME% .'
            }
        }

        stage('Push Docker Image to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat '''
                    docker login -u %DOCKER_USER% -p %DOCKER_PASS%
                    docker tag %IMAGE_NAME% %DOCKER_IMAGE%
                    docker push %DOCKER_IMAGE%
                    docker logout
                    '''
                }
            }
        }

        stage('Trivy Security Scan') {
            steps {
                bat '''
                if not exist reports mkdir reports

                echo =====================================
                echo Running Trivy Security Scan
                echo =====================================

                trivy image ^
                --severity HIGH,CRITICAL ^
                --format table ^
                -o reports/trivy-report.txt ^
                %IMAGE_NAME%

                echo.
                echo =====================================
                echo Trivy Report
                echo =====================================

                type reports\\trivy-report.txt
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                bat '''
                set KUBECONFIG=C:\\Users\\Admin\\.kube\\config

                echo =====================================
                echo Deploying to Kubernetes
                echo =====================================

                kubectl config current-context

                kubectl set image deployment/product-service ^
                product-service=%DOCKER_IMAGE%

                kubectl rollout status deployment/product-service
                '''
            }
        }

        stage('Verify Kubernetes Deployment') {
            steps {
                bat '''
                set KUBECONFIG=C:\\Users\\Admin\\.kube\\config

                echo =====================================
                echo Kubernetes Deployment Status
                echo =====================================

                kubectl get deployments

                echo.

                kubectl get pods

                echo.

                kubectl get svc
                '''
            }
        }
    }

    post {
        success {
            echo "BUILD SUCCESSFUL"
        }

        failure {

            bat 'docker ps -a'

            bat '''
            set KUBECONFIG=C:\\Users\\Admin\\.kube\\config
            kubectl get pods
            '''
        }

        always {
            archiveArtifacts artifacts: 'reports/*', fingerprint: true
            cleanWs()
        }
    }
}
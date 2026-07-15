pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    environment {
        IMAGE_NAME   = "product-service:${BUILD_NUMBER}"
        DOCKER_IMAGE = "sethu1705/product-service:${BUILD_NUMBER}"
        KUBECONFIG   = "C:\\Users\\Admin\\.kube\\config"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                bat '''
                echo =====================================
                echo VERIFYING TOOLS
                echo =====================================

                echo JAVA_HOME=%JAVA_HOME%

                java -version
                mvn -version
                docker --version
                trivy --version
                kubectl version --client
                '''
            }
        }

        stage('Verify Docker') {
            steps {
                bat '''
                echo =====================================
                echo VERIFYING DOCKER
                echo =====================================

                docker info
                docker images
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
                withCredentials([
                    string(credentialsId: 'sonarqube-auth-token',
                    variable: 'SONAR_TOKEN')
                ]) {

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
                bat '''
                docker build -t %IMAGE_NAME% .
                '''
            }
        }

        stage('Push Docker Image') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {

                    bat '''
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin

                    docker tag %IMAGE_NAME% %DOCKER_IMAGE%

                    docker push %DOCKER_IMAGE%

                    docker logout
                    '''
                }
            }
        }

        stage('Trivy Scan') {
            steps {

                bat '''
                if not exist reports mkdir reports

                echo =====================================
                echo TRIVY SECURITY SCAN
                echo =====================================

                trivy image ^
                --severity HIGH,CRITICAL ^
                --format table ^
                -o reports\\trivy-report.txt ^
                %IMAGE_NAME%

                type reports\\trivy-report.txt
                '''
            }
        }

        stage('Deploy to Kubernetes') {

            steps {

                script {

                    try {

                        bat '''
                        set KUBECONFIG=%KUBECONFIG%

                        echo =====================================
                        echo DEPLOYING IMAGE
                        echo =====================================

                        kubectl set image deployment/product-service ^
                        product-service=%DOCKER_IMAGE%

                        echo.

                        echo Waiting for rollout...

                        kubectl rollout status deployment/product-service --timeout=600s

                        echo.

                        echo =====================================
                        echo VERIFY DEPLOYMENT
                        echo =====================================

                        kubectl get deployment product-service

                        kubectl get pods -o wide

                        echo.

                        kubectl get deployment product-service -o=jsonpath="{.spec.template.spec.containers[0].image}"

                        echo.
                        '''

                        echo "Deployment Successful"

                    }

                    catch(Exception ex) {

                        echo "Deployment Failed"

                        bat '''
                        set KUBECONFIG=%KUBECONFIG%

                        echo =====================================
                        echo DEPLOYMENT DETAILS
                        echo =====================================

                        kubectl describe deployment product-service

                        echo.

                        kubectl get pods

                        echo.

                        kubectl describe pods

                        echo.

                        kubectl logs deployment/product-service --tail=100
                        '''

                        echo "Rolling Back..."

                        bat '''
                        set KUBECONFIG=%KUBECONFIG%

                        kubectl rollout undo deployment/product-service

                        kubectl rollout status deployment/product-service --timeout=600s
                        '''

                        error("Deployment Failed. Rollback completed.")
                    }

                }
            }
        }

        stage('Verify Kubernetes Deployment') {

            steps {

                bat '''
                set KUBECONFIG=%KUBECONFIG%

                echo =====================================
                echo DEPLOYMENT STATUS
                echo =====================================

                kubectl get deployments

                echo.

                kubectl get pods

                echo.

                kubectl get svc

                echo.

                kubectl get deployment product-service -o wide
                '''
            }
        }

    }

    post {

        success {

            echo "====================================="
            echo "PIPELINE COMPLETED SUCCESSFULLY"
            echo "====================================="
        }

        failure {

            echo "====================================="
            echo "PIPELINE FAILED"
            echo "====================================="

            bat '''
            docker ps -a

            set KUBECONFIG=%KUBECONFIG%

            kubectl get pods

            kubectl describe deployment product-service
            '''
        }

        always {

            archiveArtifacts artifacts: 'reports/*', fingerprint: true

            cleanWs()
        }
    }
}
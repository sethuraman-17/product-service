pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
    }

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    environment {
        IMAGE_NAME   = "product-service:${BUILD_NUMBER}"
        DOCKER_IMAGE = "sethu1705/product-service:${BUILD_NUMBER}"
        KUBECONFIG   = "/var/lib/jenkins/.kube/config"
        SONAR_HOST   = "http://localhost:9000"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                sh '''
                set -e
                echo "===== VERIFY TOOLS ====="
                java -version
                mvn -version
                git --version
                docker --version
                trivy --version
                kubectl version --client
                '''
            }
        }

        stage('Verify Docker') {
            steps {
                sh '''
                set -e
                docker info
                docker images
                '''
            }
        }

        stage('Build Application') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-auth-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                    mvn sonar:sonar \
                      -Dsonar.host.url=$SONAR_HOST \
                      -Dsonar.login=$SONAR_TOKEN \
                      -Dsonar.projectKey=product-service \
                      -Dsonar.projectName="Product Service"
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${IMAGE_NAME} .'
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker tag ${IMAGE_NAME} ${DOCKER_IMAGE}
                    docker push ${DOCKER_IMAGE}
                    docker logout
                    '''
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                sh '''
                mkdir -p reports
                trivy image \
                  --severity HIGH,CRITICAL \
  --exit-code 0 \
                  --format table \
                  -o reports/trivy-report.txt \
                  ${IMAGE_NAME}
                cat reports/trivy-report.txt
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    try {
                        sh '''
                        export KUBECONFIG=${KUBECONFIG}

                        kubectl set image deployment/product-service \
                        product-service=${DOCKER_IMAGE}

                        kubectl rollout status deployment/product-service --timeout=600s

                        kubectl rollout history deployment/product-service

                        echo

                        kubectl get rs

                        echo

                        kubectl get pods -o wide

                        kubectl get deployment product-service

                        kubectl get deployment product-service -o wide

                        echo

                        kubectl get deployment product-service \
                        -o=jsonpath='{.spec.template.spec.containers[0].image}'

                        echo
                        '''
                        echo "Deployment Successful"
                    } catch(Exception ex) {
                        echo "Deployment Failed"

                        sh '''
                        export KUBECONFIG=${KUBECONFIG}

                        kubectl describe deployment product-service || true
                        kubectl get pods || true
                        kubectl describe pods || true
                        kubectl logs deployment/product-service --tail=100 || true

                        kubectl rollout undo deployment/product-service
                        kubectl rollout status deployment/product-service --timeout=600s
                        '''

                        error("Deployment failed. Rollback completed.")
                    }
                }
            }
        }

        stage('Verify Kubernetes Deployment') {
            steps {
                sh '''
                export KUBECONFIG=${KUBECONFIG}

                kubectl get deployments
                kubectl get pods
                kubectl get svc
                kubectl get deployment product-service -o wide

                echo

                kubectl get deployment product-service \
                -o=jsonpath='{.spec.template.spec.containers[0].image}'

                echo
                '''
            }
        }
    }

    post {
        success {
            echo '====================================='
            echo 'PIPELINE COMPLETED SUCCESSFULLY'
            echo '====================================='
        }

        failure {
            echo '====================================='
            echo 'PIPELINE FAILED'
            echo '====================================='

            sh '''
            docker ps -a || true
            export KUBECONFIG=${KUBECONFIG}
            kubectl get pods || true
            kubectl describe deployment product-service || true
            '''
        }

        always {
            archiveArtifacts artifacts: 'reports/*', fingerprint: true, allowEmptyArchive: true
            cleanWs()
        }
    }
}

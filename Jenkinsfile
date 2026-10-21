// Declarative CI/CD pipeline for the Calculator REST API.
// Checkout -> Build -> Test -> Docker Build -> Push -> Deploy, plus email notification.
//
// Jenkins prerequisites (see GUIDE.md, Part B):
//   * Credentials: "dockerhub-creds" (Username with password = Docker Hub user + access token)
//   * Global env var NOTIFY_EMAIL (Manage Jenkins -> System -> Global properties)
//   * Extended E-mail Notification configured with an SMTP server
//   * Job option "GitHub hook trigger for GITScm polling" + a GitHub webhook

pipeline {
    agent any

    options {
        disableConcurrentBuilds()          // two pushes never deploy at the same time
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
    }

    triggers {
        githubPush()                       // GitHub webhook (primary)
        pollSCM('H/5 * * * *')             // fallback if the webhook is unreachable
    }

    environment {
        // Homebrew Jenkins starts with a minimal PATH and cannot see mvn/docker otherwise.
        PATH        = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        IMAGE       = 'nirbhaysharma504/calculator-api'
        TAG         = "${env.BUILD_NUMBER}"
        APP_URL     = 'http://localhost:8081'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="format:Commit %h by %an: %s"'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                // A failing test makes mvn exit non-zero, which fails the build here.
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                      --label org.opencontainers.image.revision="$GIT_COMMIT" \
                      -t "$IMAGE:$TAG" \
                      -t "$IMAGE:latest" .
                '''
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DOCKERHUB_USER',
                                                  passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    // Single quotes: the secret is expanded by the shell, never by Groovy,
                    // so it is not written to the build log.
                    sh '''
                        echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker push "$IMAGE:$TAG"
                        docker push "$IMAGE:latest"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // "up -d" only recreates containers whose image/config changed, so
                // re-running the pipeline never tears down a healthy deployment.
                sh 'IMAGE_TAG="$TAG" docker compose up -d --remove-orphans'

                sh '''
                    echo "Waiting for $APP_URL/actuator/health ..."
                    for i in $(seq 1 30); do
                      if curl -fs "$APP_URL/actuator/health" | grep -q '"status":"UP"'; then
                        echo "Application is UP (attempt $i)"
                        break
                      fi
                      if [ "$i" -eq 30 ]; then
                        echo "Health check failed after 60s"
                        docker compose ps
                        docker compose logs --tail=50 app
                        exit 1
                      fi
                      sleep 2
                    done
                '''

                sh '''
                    echo "Smoke tests through Nginx:"
                    curl -fs "$APP_URL/api/v1/add?a=2&b=3" | tee /dev/stderr | grep -q '"result":5.0'
                    echo
                    code=$(curl -s -o /dev/null -w '%{http_code}' "$APP_URL/api/v1/divide?a=1&b=0")
                    echo "divide by zero -> HTTP $code"
                    [ "$code" = "400" ]
                    docker compose ps
                '''
            }
        }
    }

    post {
        success {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """<p>Pipeline <b>${env.JOB_NAME}</b> build <b>#${env.BUILD_NUMBER}</b> succeeded.</p>
                         <p>Deployed image: <code>${env.IMAGE}:${env.TAG}</code></p>
                         <p>Console: <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>"""
            )
        }
        failure {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                attachLog: true,
                body: """<p>Pipeline <b>${env.JOB_NAME}</b> build <b>#${env.BUILD_NUMBER}</b> failed.</p>
                         <p>Console: <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a> (log attached)</p>"""
            )
        }
        always {
            // Remove dangling layers and keep only the 3 newest build-number tags locally.
            sh '''
                docker image prune -f || true
                docker images "$IMAGE" --format '{{.Tag}}' | grep -E '^[0-9]+$' | sort -rn | tail -n +4 \
                  | xargs -I{} docker rmi "$IMAGE:{}" || true
            '''
        }
    }
}

pipeline {
  agent any

  environment {
    // ====== EDIT ME ======
    IMAGE_NAME         = 'ci-demo'
    SONARQUBE_NAME     = 'sonarqube'                 // Jenkins > Configure System name
    NEXUS_HOST         = '54.209.208.145'            // Nexus IP/DNS
    NEXUS_DOCKER_PORT  = '8082'                      // Docker (hosted) port
    NEXUS_REPO         = 'docker-hosted'             // Repo name

    // Use this ONLY for login/withRegistry (includes http://)
    NEXUS_REG_HTTP     = "http://${NEXUS_HOST}:${NEXUS_DOCKER_PORT}"

    // Use this for image names/tags (MUST NOT include http://)
    NEXUS_IMAGE        = "${NEXUS_HOST}:${NEXUS_DOCKER_PORT}/${NEXUS_REPO}/${IMAGE_NAME}:${BUILD_NUMBER}"

    CRED_NEXUS_DOCKER  = 'nexus-docker-creds'        // Jenkins Username/Password
    VERSION            = "${BUILD_NUMBER}"
  }

  tools {
    maven 'Maven_3_9'
    jdk   'JDK17'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Maven Build & Unit Tests') {
      steps {
        sh 'mvn -B -U clean verify'
      }
      post {
        always {
          junit '**/target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
        }
      }
    }

    stage('SonarQube Scan') {
      steps {
        withSonarQubeEnv("${env.SONARQUBE_NAME}") {
          sh 'mvn -B -DskipTests sonar:sonar'
        }
        // If you want to enforce the gate, uncomment:
        // timeout(time: 10, unit: 'MINUTES') {
        //   waitForQualityGate abortPipeline: true
        // }
      }
    }

    stage('Docker Build') {
      steps {
        sh 'docker build -t ${IMAGE_NAME}:${VERSION} .'
      }
    }

    stage('Push to Nexus (Docker hosted)') {
      steps {
        script {
          // Tag must NOT include "http://"
          sh "docker tag ${IMAGE_NAME}:${VERSION} ${NEXUS_IMAGE}"

          // Login/push using http URL
          docker.withRegistry("${NEXUS_REG_HTTP}", "${CRED_NEXUS_DOCKER}") {
            sh "docker push ${NEXUS_IMAGE}"
          }
          echo "Pushed: ${NEXUS_IMAGE}"
        }
      }
    }
  } // <-- closes stages

  post {
    success {
      echo "Build OK → ${NEXUS_IMAGE}"
    }
    failure {
      echo 'Pipeline failed'
    }
  }
}

pipeline {
  agent any

  environment {
    IMAGE_NAME         = 'ci-demo'
    SONARQUBE_NAME     = 'sonarqube'                 
    NEXUS_HOST         = '54.209.208.145'            
    NEXUS_DOCKER_PORT  = '8082'                      
    NEXUS_REPO         = 'docker-hosted'             
    NEXUS_REG_HTTP     = "http://${NEXUS_HOST}:${NEXUS_DOCKER_PORT}"
    NEXUS_IMAGE        = "${NEXUS_HOST}:${NEXUS_DOCKER_PORT}/${NEXUS_REPO}/${IMAGE_NAME}:${BUILD_NUMBER}"
    CRED_NEXUS_DOCKER  = 'nexus-docker-creds'        
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

    stage('Build & Tests') {
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
          sh "docker tag ${IMAGE_NAME}:${VERSION} ${NEXUS_IMAGE}"

          docker.withRegistry("${NEXUS_REG_HTTP}", "${CRED_NEXUS_DOCKER}") {
            sh "docker push ${NEXUS_IMAGE}"
          }
          echo "Pushed: ${NEXUS_IMAGE}"
        }
      }
    }
  } 

  post {
    success {
      echo "Build OK → ${NEXUS_IMAGE}"
    }
    failure {
      echo 'Pipeline failed'
    }
  }
}

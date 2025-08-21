pipeline {
  agent {
    dockerfile {
      filename '.jenkins/Dockerfile'
      args '-v /var/run/docker.sock:/var/run/docker.sock'
    }
  }

  tools { maven 'Maven_3_9'; jdk 'JDK17' }

  environment {
    CRED_NEXUS_DOCKER = 'nexus-docker-creds'
    NEXUS_REG_URL = "http://http://54.209.208.145:8082"
    NEXUS_REPO = "docker-hosted"
    APP_NAME = "ci-demo"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Tests') {
      steps { sh 'mvn clean verify' }
      post {
        always { junit '**/target/surefire-reports/*.xml' }
      }
    }

    stage('SonarQube Scan') {
      steps {
        withSonarQubeEnv('sonarqube') {
          sh 'mvn sonar:sonar'
        }
        waitForQualityGate abortPipeline: true
      }
    }

    stage('Functional Tests') {
      steps {
        sh 'docker run --rm -v "$PWD/tests:/etc/newman" postman/newman_alpine run /etc/newman/functional.postman_collection.json'
      }
    }

    stage('Performance Tests') {
      steps {
        sh 'docker run --rm -v "$PWD/tests:/tests" justb4/jmeter:5.6.3 -n -t /tests/performance.jmx -l /tests/results.jtl'
      }
    }

    stage('Docker Build & Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.CRED_NEXUS_DOCKER,
                                          usernameVariable: 'NEXUS_USER',
                                          passwordVariable: 'NEXUS_PASS')]) {
          sh '''
            docker build -t ${APP_NAME}:${BUILD_NUMBER} .
            echo "${NEXUS_PASS}" | docker login ${NEXUS_REG_URL} -u "${NEXUS_USER}" --password-stdin
            docker tag ${APP_NAME}:${BUILD_NUMBER} ${NEXUS_REG_URL}/${NEXUS_REPO}/${APP_NAME}:${BUILD_NUMBER}
            docker push ${NEXUS_REG_URL}/${NEXUS_REPO}/${APP_NAME}:${BUILD_NUMBER}
          '''
        }
      }
    }
  }
}

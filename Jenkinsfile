pipeline {
  agent any

  environment {
    // ====== EDIT ME ======
    IMAGE_NAME        = 'ci-demo'
    SONARQUBE_NAME    = 'sonarqube'                 // Jenkins > Configure System name
    NEXUS_HOST        = '54.209.208.145'              // << your Nexus IP/DNS
    NEXUS_DOCKER_PORT = '8082'                      // Docker (hosted) port
    NEXUS_REPO        = 'docker-hosted'             // Repo name
    NEXUS_REG_URL     = "http://${NEXUS_HOST}:${NEXUS_DOCKER_PORT}"
    NEXUS_IMAGE       = "${NEXUS_REG_URL}/${NEXUS_REPO}/${IMAGE_NAME}:${BUILD_NUMBER}"
    CRED_NEXUS_DOCKER = 'nexus-docker-creds'        // Jenkins Username/Password
    // ======================

    // Helpful for logs
    VERSION           = "${BUILD_NUMBER}"
  }

  tools { maven 'Maven_3_9'; jdk 'JDK17' }
  // options { timestamps(); ansiColor('xterm'); timeout(time: 45, unit: 'MINUTES') }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Maven Build & Unit Tests') {
      steps { sh 'mvn -B -U clean verify' }
      post {
        always {
          junit '**/target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
        }
      }
    }

    stage('SonarQube Scan + Quality Gate') {
      steps {
        withSonarQubeEnv("${env.SONARQUBE_NAME}") {
          // If you set server auth in Jenkins Sonar config, you don't need -Dsonar.login
          sh 'mvn -B -DskipTests sonar:sonar'
        }
        //timeout(time: 10, unit: 'MINUTES') {
        //  waitForQualityGate abortPipeline: true
       // }
      }
    }

    stage('Functional Tests (Newman)') {
      steps {
        // Use dockerized newman so you don't need local install
        sh '''
          if ! docker image inspect postman/newman_alpine:latest >/dev/null 2>&1; then
            docker pull postman/newman_alpine:latest
          fi
          docker run --rm -v "$PWD/tests:/etc/newman" postman/newman_alpine:latest \
            run /etc/newman/functional.postman_collection.json || true
        '''
      }
    }

    stage('Performance Tests (JMeter)') {
      steps {
        // Use dockerized JMeter; results go back into tests/results.jtl
        sh '''
          if ! docker image inspect justb4/jmeter:5.6.3 >/dev/null 2>&1; then
            docker pull justb4/jmeter:5.6.3
          fi
          mkdir -p tests
          docker run --rm -v "$PWD/tests:/tests" justb4/jmeter:5.6.3 \
            -n -t /tests/performance.jmx -l /tests/results.jtl || true
        '''
      }
      post {
        always { archiveArtifacts artifacts: 'tests/results.jtl', allowEmptyArchive: true }
      }
    }

    stage('Docker Build') {
      steps {
        sh 'docker build -t ${IMAGE_NAME}:${VERSION} .'
      }
    }

    stage('Push → Nexus (Docker hosted)') {
      steps {
        script {
          sh "docker tag ${IMAGE_NAME}:${VERSION} ${NEXUS_IMAGE}"
          docker.withRegistry("${NEXUS_REG_URL}", "${CRED_NEXUS_DOCKER}") {
            sh "docker push ${NEXUS_IMAGE}"
          }
          echo "✅ Pushed: ${NEXUS_IMAGE}"
        }
      }
    }
  }

  post {
    success {
      echo "Build OK → ${NEXUS_IMAGE}"
    }
    failure {
      echo '❌ Pipeline failed'
    }
  }
}

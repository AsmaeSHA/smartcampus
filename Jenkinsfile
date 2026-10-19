pipeline {
    agent any

    tools {
        maven 'Maven' 
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build (Maven)') {
            steps {
                dir('backend') {
                    sh 'mvn -B -Dmaven.test.skip=TRUE clean install'
                }
            }
        }
        stage('Run App (JAR)') {
            steps {
                dir('backend') {
                    sh 'echo "start running the jar file"'
                    sh 'nohup java -jar target/*.jar > app.log 2>&1 &'
                }
            }
        }
    }
}

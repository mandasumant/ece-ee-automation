@Library('PSL@master') _

def common_sonar

pipeline {

    agent {
        label 'aws-centos'
    }

    triggers {
        cron '0 2 * * *'
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        timestamps()
    }

    stages {
        stage ('Prepare environment') {
            when {
                not {
                    triggeredBy 'TimerTrigger'
                }
            }
            steps {
                script {
                    common_sonar = new ors.utils.CommonSonar(steps, env, docker)

                    // pull source
                    checkout scm
                }
            }
        }

        stage('Build Maven Project') {
            when {
                not {
                    triggeredBy 'TimerTrigger'
                }
            }
            agent {
                docker {
                    image 'artifactory.dev.adskengineer.net/autodeskcloud/ctr-ci-slave-jdk-11:latest'
                    args '-v /mnt/data/.m2:/home/jenkins/.m2'
                    reuseNode true
                }
            }
            steps {
                script {
                    sh "mvn clean install -DskipTests"
                }
            }
        }

        stage ('SonarQube code quality scan') {
            when {
                not {
                    triggeredBy 'TimerTrigger'
                }
            }
            agent {
                docker {
                    image 'artifactory.dev.adskengineer.net/autodeskcloud/ctr-ci-slave-jdk-11:latest'
                    args '-v /mnt/data/.m2:/home/jenkins/.m2'
                    reuseNode true
                }
            }
            steps {
                script {
                    common_sonar.do_maven_scan(trunk: "${env.BRANCH_NAME}")
                }
            }
        }

        stage ('Nightly Regression') {
            when {
                allOf {
                    branch 'master'
                    triggeredBy 'TimerTrigger'
                }
            }
            steps {
                echo 'Space for Nightly CJT TestingHub Call'
            }
        }
    }
}
import com.autodesk.wpe.dsl.build.ServicesBuildHelper
import com.autodesk.wpe.dsl.build.BuildInfo
import com.autodesk.wpe.dsl.shared.AmbassadorService

@Library(['PSL@master', 'jenkins-shared-lib', 'jenkins-modules', 'wpe-shared-library@psl_2.0']) _

def common_sonar
def buildInfo = new BuildInfo(currentBuild: currentBuild, moduleName: "testinghub-autobot", stack: "test")
def serviceBuildHelper = new ServicesBuildHelper(this, 'svc_d_artifactory', buildInfo)

pipeline {

    agent {
        label 'aws-centos'
    }

    triggers {
        cron 'H 2 * * *'
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
          /*  when {
                allOf {
                    branch 'master'
                    triggeredBy 'TimerTrigger'
                }
           }*/
            steps {
                echo 'Initiating Nightly Customer Lifecycle Tests'

                script {
                    println("Building Testing Hub API Input Map - estore")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"Order Service - Orders","notificationemail":[""],"testcases":[' +
                                                              '{"displayname":"BiC Trial Download","testcasename":"validateTrialDownloadUI","description":"BiC Trial Download","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"trialDownload-UI","testMethod":"validateTrialDownloadUI","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"thubnamerbaazjecvgbut@letscheck.pw","password":"Password1"},"notsupportedenv":[],"wiki":""}],"workstreamname":"dclecjt"},'
                    println("Starting Testing Hub API Call - estore")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - estore')
                    } else {
                        println('Testing Hub API call failed - estore')
                    }
                }
                script {
                    println("Building Testing Hub API Input Map - accountportal")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"Order Service - Account Portal","notificationemail":[""],"testcases":[' +
                                                              '{"displayname":"Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native-US","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native-US","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Change Payment","testcasename":"validateBICChangePaymentProfile","description":"Change Payment from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-changePayment-US","testMethod":"validateBICChangePaymentProfile","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Align Billing","testcasename":"validateAlignBilling","description":"Align 2 Subscriptions to same Renewal from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-align-billing","testMethod":"validateAlignBilling","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""}],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - accountportal")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - accountportal')
                    } else {
                        println('Testing Hub API call failed - accountportal')
                    }
                }
                script {
                    println("Building Testing Hub API Input Map - EDU ")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"EDU Deploy Tests","notificationemail":[""],"testcases":[' +
                        '{"displayname":"Educator flow","testcasename":"validateFusionActivationByEducator","description":"Activate fusion Educator","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"activate-fusion-educator","testMethod":"validateFusionActivationByEducator","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                        '{"displayname":"Student Flow","testcasename":"validateNewStudentSubscription","description":"Student Subscription flow","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-student-subscription","testMethod":"validateNewStudentSubscription","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                        '{"displayname":"IT Admin Flow","testcasename":"validateAdminUser","description":"Flow for a school IT admin","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-edu-admin","testMethod":"validateAdminUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"},' +
                        '{"displayname":"Design Competition Mentor Flow","testcasename":"validateMentorUser","description":"Design competition mentor flow","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-mentor-user","testMethod":"validateMentorUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"}' +
                        '],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - EDU Tests")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - EDU Tests')
                    } else {
                        println('Testing Hub API call failed - EDU Tests')
                    }
                }
            }
        }
    }
}

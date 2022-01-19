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

        stage ('Regression') {
            when {
                branch 'master'
            }
            steps {
                echo 'Initiating Customer Lifecycle Tests'

                script {
                    println("Building Testing Hub API Input Map - estore")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"CLT - GUAC Orders","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"BiC Native Order US","testcasename":"validateBicNativeOrder","description":"BiC Native Order - US ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order UK","testcasename":"validateBicNativeOrder","description":"BiC Native Order - UK ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order DE","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Add seats from GUAC","testcasename":"validateBicAddSeats","description":"Add seats from GUAC","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order Flex","testcasename":"validateBicFlexOrder","description":"BiC order Flex","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder","testMethod":"validateBicFlexOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"META order","testcasename":"validateBicMetaOrder","description":"META order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-metaorder","testMethod":"validateBicMetaOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname": "MOE order", "testcasename": "validateBicNativeOrderMoe", "description": "MOE order", "os": "windows", "testClass": "com.autodesk.ece.bic.testsuites.BICOrderCreation", "testGroup": "bic-nativeorder-moe", "testMethod": "validateBicNativeOrderMoe", "parameters": { "application": "ece" }, "testdata": { "usertype": "new", "password": "", "payment": "VISA", "store": "STORE-NAMER", "sku": "default:1", "email": "" }, "notsupportedenv": [], "wiki": "" },' +
                                                              '{"displayname":"BiC refund order ACH","testcasename":"validateBicRefundOrder","description":"BiC refund order - ACH","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"ACH","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC refund order PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC refund order - PAYPAL","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC refund order VISA","testcasename":"validateBicRefundOrder","description":"BiC refund order - VISA","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order with existing user","testcasename":"validateBicReturningUser","description":"BiC order with existing user","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC renew order","testcasename":"validateRenewBicOrder","description":"BiC renew recurring order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Indirect Japan","testcasename":"validateBicIndirectOrder","description":"BiC Indirect Order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-indirectorder-JP","testMethod":"validateBicIndirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Trial Download","testcasename":"validateTrialDownloadUI","description":"BiC Trial Download","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"trialDownload-UI","testMethod":"validateTrialDownloadUI","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"thubnamerbaazjecvgbut@letscheck.pw","password":"Password1"},"notsupportedenv":[],"wiki":""}],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - estore")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - estore')
                    } else {
                        currentBuild.result = 'FAILURE'
                        println('Testing Hub API call failed - estore')
                    }
                }
                script {
                    println("Building Testing Hub API Input Map - accountportal")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"CLT - Account Portal Order","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Change Payment","testcasename":"validateBICChangePaymentProfile","description":"Change Payment from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-changePayment","testMethod":"validateBICChangePaymentProfile","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Switch Term","testcasename":"validateBicNativeOrderSwitchTerm","description":"Switch Term for BiC Order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder-switch-term","testMethod":"validateBicNativeOrderSwitchTerm","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Restart Subscription","testcasename":"validateRestartSubscription","description":"Restart a Canceled Subscription","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-restart-subscription","testMethod":"validateRestartSubscription","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Align Billing","testcasename":"validateAlignBilling","description":"Align 2 Subscriptions to same Renewal from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-align-billing","testMethod":"validateAlignBilling","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname": "Indirect Order in Portal", "testcasename": "validateBICIndirectSAPOrder", "description": "SAP Order in Portal", "os": "windows", "testClass": "com.autodesk.ece.bic.testsuites.IndirectOrderCreation", "testGroup": "sap-bicindirect", "testMethod": "validateBICIndirectSAPOrder", "parameters": { "application": "ece" }, "testdata": { "sku": "057M1-WWN886-L563:1", "salesorg": "3000", "SAPConfigLocation": "C:\\\\TestingHub\\\\SAPConfig" }, "notsupportedenv": [], "wiki": ""}],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - accountportal")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - accountportal')
                    } else {
                        currentBuild.result = 'FAILURE'
                        println('Testing Hub API call failed - accountportal')
                    }
                }
                script {
                    println("Building Testing Hub API Input Map - EDU ")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/edu/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"EDU Deploy Tests","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                        '{"displayname":"Educator flow","testcasename":"validateProductActivationByEducator","description":"Activate Educator Product","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"activate-product-educator","testMethod":"validateProductActivationByEducator","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                        '{"displayname":"Student Flow","testcasename":"validateNewStudentSubscription","description":"Student Subscription flow","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-student-subscription","testMethod":"validateNewStudentSubscription","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                        '{"displayname":"Design Competition Mentor Flow","testcasename":"validateMentorUser","description":"Design competition mentor flow","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-mentor-user","testMethod":"validateMentorUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"}' +
                        '],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - EDU Tests")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - EDU Tests')
                    } else {
                        currentBuild.result = 'FAILURE'
                        println('Testing Hub API call failed - EDU Tests')
                    }
                }
            }
        }

        stage ('ZIP Regression') {
            when {
                branch 'master'
            }
            steps {
                echo 'Initiating ZIP UAT Regression'

                script {
                    println("Building Testing Hub API Input Map - estore")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"ZIP Regression - GUAC Orders","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"BiC Native Order AU","testcasename":"validateBicNativeOrder","description":"BiC Native Order - AU ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Add seats from GUAC","testcasename":"validateBicAddSeats","description":"Add seats from GUAC","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order Flex","testcasename":"validateBicFlexOrder","description":"BiC order Flex","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder","testMethod":"validateBicFlexOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"META order","testcasename":"validateBicMetaOrder","description":"META order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-metaorder","testMethod":"validateBicMetaOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC refund order ZIP","testcasename":"validateBicRefundOrder","description":"BiC refund order - ZIP","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order with existing user","testcasename":"validateBicReturningUser","description":"BiC order with existing user","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""}' +
                                                              '],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - estore")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - estore')
                    } else {
                        currentBuild.result = 'FAILURE'
                        println('Testing Hub API call failed - estore')
                    }
                }
                script {
                    println("Building Testing Hub API Input Map - Account Portal")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"ZIP Regression - Account Portal Orders","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Change Payment","testcasename":"validateBICChangePaymentProfile","description":"Change Payment from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-changePayment","testMethod":"validateBICChangePaymentProfile","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Restart Subscription","testcasename":"validateRestartSubscription","description":"Restart a Canceled Subscription","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-restart-subscription","testMethod":"validateRestartSubscription","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Align Billing","testcasename":"validateAlignBilling","description":"Align 2 Subscriptions to same Renewal from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-align-billing","testMethod":"validateAlignBilling","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU","productName":"MUDBOX"},"notsupportedenv":[],"wiki":""}],"workstreamname":"dclecjt"}'
                    println("Starting Testing Hub API Call - accountportal")
                    if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                        println('Testing Hub API called successfully - accountportal')
                    } else {
                        currentBuild.result = 'FAILURE'
                        println('Testing Hub API call failed - accountportal')
                    }
                }
            }
        }
    }
}

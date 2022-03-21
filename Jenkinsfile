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

    parameters {
        booleanParam(name: 'CJT' , defaultValue: false, description: 'Run CJT Regression ?')
        booleanParam(name: 'LIFT', defaultValue: false, description: 'Run LIFT Regression ?')
        booleanParam(name: 'APPOLO', defaultValue: false, description: 'Run Appolo Regression ?')

    }

    stages {
        stage ('Prepare environment') {
            when {
                not {
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        expression {
                            params.CJT == true ||
                            params.LIFT == true ||
                            params.APPOLO == true
                        }
                    }
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
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        expression {
                            params.CJT == true ||
                            params.LIFT == true ||
                            params.APPOLO == true
                        }
                    }
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
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        expression {
                            params.CJT == true ||
                            params.LIFT == true ||
                            params.APPOLO == true
                        }
                    }
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
                anyOf {
                    triggeredBy 'TimerTrigger'
                    expression {
                        params.CJT == true
                    }
                }
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
                                                              '{"displayname":"BiC Native Order DE  SEPA","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - SEPA","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order DE  GIROPAY","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - GIROPAY","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order AUS  ZIP","testcasename":"validateBicNativeOrder","description":"BiC Native Order - AUS - ZIP","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"ZIP","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Add seats from GUAC","testcasename":"validateBicAddSeats","description":"Add seats from GUAC","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order Flex","testcasename":"validateBicFlexOrder","description":"BiC order Flex","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder","testMethod":"validateBicFlexOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"META order","testcasename":"validateBicMetaOrder","description":"META order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-metaorder","testMethod":"validateBicMetaOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"MOE order","testcasename":"validateBicNativeOrderMoe","description":"MOE order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC refund order PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC refund order - PAYPAL","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC refund order VISA","testcasename":"validateBicRefundOrder","description":"BiC refund order - VISA","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order with existing user","testcasename":"validateBicReturningUser","description":"BiC order with existing user","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC renew order","testcasename":"validateRenewBicOrder","description":"BiC renew recurring order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Trial Download","testcasename":"validateTrialDownloadUI","description":"BiC Trial Download","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"trialDownload-UI","testMethod":"validateTrialDownloadUI","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"guac-ct-cc-us-20210820@ssttest.net","password":"test1234"},"notsupportedenv":[],"wiki":""}' +
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
                                                              '{"displayname": "Indirect Order in Portal", "testcasename": "validateBICIndirectSAPOrder", "description": "SAP Order in Portal", "os": "windows", "testClass": "com.autodesk.ece.bic.testsuites.IndirectOrderCreation", "testGroup": "sap-bicindirect", "testMethod": "validateBICIndirectSAPOrder", "parameters": { "application": "ece" }, "testdata": { "sku": "057M1-WWN886-L563:1", "salesorg": "3000", "SAPConfigLocation": "C:\\\\TestingHub\\\\SAPConfig" }, "notsupportedenv": [], "wiki": ""}' +
                                                              '],"workstreamname":"dclecjt"}'
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

        stage ('LiftForward Financing Regression') {
            when {
                branch 'master'
                expression {
                    params.LIFT == true
                }
            }
            steps {
                echo 'Initiating LiftForward Financing UAT Regression'

                script {
                    println("Building Testing Hub API Input Map - estore")
                    def testingHubInputMap = [:]
                    testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                    testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"LiftForward Financing Regression - GUAC Orders","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"BiC Native Order Financing","testcasename":"validateBicNativeOrder","description":"BiC Native Order - US ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Financing Declined Order","testcasename":"validateBicNativeFinancingDeclinedOrder","description":"BiC Financing Order Declined","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-declined","testMethod":"validateBicNativeFinancingDeclinedOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Financing Canceled Order","testcasename":"validateBicNativeFinancingCanceledOrder","description":"BiC Financing Order Canceled","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-canceled","testMethod":"validateBicNativeFinancingCanceledOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"META order Financing","testcasename":"validateBicMetaOrder","description":"META order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-metaorder","testMethod":"validateBicMetaOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC refund order Financing","testcasename":"validateBicRefundOrder","description":"BiC refund order - LIFT","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC order with existing user Financing","testcasename":"validateBicReturningUser","description":"BiC order with existing user","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""}' +
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
                    testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"LiftForward Financing - Account Portal Orders","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Change Payment","testcasename":"validateBICChangePaymentProfile","description":"Change Payment from Portal","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-changePayment","testMethod":"validateBICChangePaymentProfile","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"Switch Term","testcasename":"validateBicNativeOrderSwitchTerm","description":"Switch Term for BiC Order","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder-switch-term","testMethod":"validateBicNativeOrderSwitchTerm","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki": ""}' +
                                                              '],"workstreamname":"dclecjt"}'
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

         stage ('Appolo UAT') {
           when {
              branch 'master'
              expression {
                  params.APPOLO == true
              }
          }
          steps {
              echo 'Initiating Appolo UAT '

              script {
                  println("Building Testing Hub API Input Map - estore")
                  def testingHubInputMap = [:]
                  testingHubInputMap.authClientID = 'fSPZcP0OBXjFCtUW7nnAJFYJlXcWvUGe'
                  testingHubInputMap.authCredentialsID = 'testing-hub-creds-id'
                  testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
                  testingHubInputMap.testingHubApiPayload = '{"env":"STG","executionname":"Appolo UAT - GUAC Orders","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                                                              '{"displayname":"BiC Native Order US(en_US)","testcasename":"validateBicNativeOrder","description":"BiC Native Order US(en_US) ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order UK(en_GB)","testcasename":"validateBicNativeOrder","description":"BiC Native Order UK(en_GB) ","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order DE  SEPA(de_DE)","testcasename":"validateBicNativeOrder","description":"BiC Native Order DE  SEPA(de_DE)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order DE  GIROPAY(de_DE)","testcasename":"validateBicNativeOrder","description":"BiC Native Order DE  GIROPAY(de_DE","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order AUS(en_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order AUS  ZIP(en_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"en_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order CA(en_CA)","testcasename":"validateBicNativeOrder","description":"BiC Native Order CA(en_CA)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","locale":"en_CA"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order CA(fr_CA)","testcasename":"validateBicNativeOrder","description":"BiC Native Order CA(fr_CA)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","locale":"fr_CA"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order NL(nl_NL)","testcasename":"validateBicNativeOrder","description":"BiC Native Order NL(nl_NL)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"nl_NL"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order PL(pl_PL)","testcasename":"validateBicNativeOrder","description":"BiC Native Order PL(pl_PL)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-PL","sku":"default:1","email":"","locale":"pl_PL"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order IT(it_IT)","testcasename":"validateBicNativeOrder","description":"BiC Native Order IT(it_IT)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-IT","sku":"default:1","email":"","locale":"it_IT"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order SV(sv_SE)","testcasename":"validateBicNativeOrder","description":"BiC Native Order SV(sv_SE)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-SE","sku":"default:1","email":"","locale":"sv_SE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order NO(no_NO)","testcasename":"validateBicNativeOrder","description":"BiC Native Order NO(no_NO)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-SE","sku":"default:1","email":"","locale":"no_NO"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order FR(fr_FR)","testcasename":"validateBicNativeOrder","description":"BiC Native Order FR(fr_FR)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-FR","sku":"default:1","email":"","locale":"fr_FR"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order DE(de_DE)","testcasename":"validateBicNativeOrder","description":"BiC Native Order DE(de_DE)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order IT(it_IT)","testcasename":"validateBicNativeOrder","description":"BiC Native Order IT(it_IT)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-IT","sku":"default:1","email":"","locale":"it_IT"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order SE(es_ES)","testcasename":"validateBicNativeOrder","description":"BiC Native Order SE(es_ES)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-ES","sku":"default:1","email":"","locale":"es_ES"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order FL(fl_FL)","testcasename":"validateBicNativeOrder","description":"BiC Native Order FL(fl_FL)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"fl_FL"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order HU(hu_HU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order HU(hu_HU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"hu_HU"},"notsupportedenv":[],"wiki": ""},' +
                                                              '{"displayname":"BiC Native Order PT(pt_PT)","testcasename":"validateBicNativeOrder","description":"BiC Native Order PT(pt_PT)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"pt_PT"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order DK(da_DK)","testcasename":"validateBicNativeOrder","description":"BiC Native Order DK(da_DK)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-DK","sku":"default:1","email":"","locale":"da_DK"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order CZ(cs_CZ)","testcasename":"validateBicNativeOrder","description":"BiC Native Order CZ(cs_CZ)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CZ","sku":"default:1","email":"","locale":"cs_CZ"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order CH(de_CH)","testcasename":"validateBicNativeOrder","description":"BiC Native Order CH(de_CH)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CH","sku":"default:1","email":"","locale":"de_CH"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order CH(fr_CH)","testcasename":"validateBicNativeOrder","description":"BiC Native Order CH(fr_CH)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CH","sku":"default:1","email":"","locale":"fr_CH"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order CH(it_CH)","testcasename":"validateBicNativeOrder","description":"BiC Native Order CH(it_CH)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CH","sku":"default:1","email":"","locale":"it_CH"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Papua New Guinea(pn_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Papua New Guinea(pn_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"pn_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Cocos Island(ci_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Cocos Island(ci_AU","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"ci_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Christmas Island(cs_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Christmas Island(cs_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"cs_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Fiji(fi_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Fiji(fi_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"cs_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Marshall Island(mi_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Marshall Island(mi_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"fi_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Nauru(na_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Nauru(na_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"na_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Niue(nu_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Niue(nu_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"nu_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Norfolk Island(ni_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Norfolk Island(ni_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"ni_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Palau(pa_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Palau(pa_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"pa_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Pitcarin Island(pi_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order  Pitcarin Island(pi_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"pi_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Samoa(sa_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Samoa(sa_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"sa_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Solomon Islands(si_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Solomon Islands(si_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"si_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Tonga(to_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Tonga(to_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"to_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Tavulu(tu_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Tavulu(tu_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"tu_AU"},"notsupportedenv":[],"wiki":""},' +
                                                              '{"displayname":"BiC Native Order Vanuatu(va_AU)","testcasename":"validateBicNativeOrder","description":"BiC Native Order Vanuatu(va_AU)","os":"windows","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","locale":"va_AU"},"notsupportedenv":[],"wiki":""}' +
                                                            '],"workstreamname":"dclecjt"}'
                  println("Starting Testing Hub API Call - estore")
                  if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)){
                      println('Testing Hub API called successfully - estore')
                  } else {
                      currentBuild.result = 'FAILURE'
                      println('Testing Hub API call failed - estore')
                  }
              }

          }
      }
    }
}

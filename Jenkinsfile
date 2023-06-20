#!groovy

import com.autodesk.wpe.dsl.build.BuildInfo
import com.autodesk.wpe.dsl.build.ServicesBuildHelper
import groovy.json.JsonBuilder

@Library(['PSL@master', 'jenkins-shared-lib', 'jenkins-modules', 'wpe-shared-library@psl_2.0']) _

SAP_INVOICE_VALIDATION = false

def common_sonar
def buildInfo = new BuildInfo(currentBuild: currentBuild, moduleName: "testinghub-autobot", stack: "test")
def serviceBuildHelper = new ServicesBuildHelper(this, 'svc_d_artifactory', buildInfo)

def generateTest(name, testcase, address, options = []) {
    def testData = [
            usertype     : "new",
            password     : "",
            emailType    : "biz",
            sapValidation: String.valueOf(SAP_INVOICE_VALIDATION)
    ]
    testData.putAll(address)
    testData.putAll(options)

    return new JsonBuilder([
            displayname : testcase.displaynamePrefix + " " + name,
            testcasename: testcase.testcasename,
            description : testcase.descriptionPrefix + " " + name,
            testClass   : testcase.testClass,
            testGroup   : testcase.testGroup,
            testMethod  : testcase.testMethod,
            parameters  : [
                    application: "ece"
            ],
            testdata    : testData
    ]).toPrettyString()
}

pipeline {

    agent {
        label 'aws-centos'
    }

    triggers {
        cron 'H 2 * * *'
    }

    options {
        timeout(time: 2, unit: 'HOURS')
        timestamps()
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['STG', 'INT'], description: 'Choose Environment')
        booleanParam(name: 'CJT', defaultValue: false, description: 'Run CJT Regression')
        string(name: 'EXECUTION_ID', defaultValue: '', description: 'Enter previous Execution ID')
        booleanParam(name: 'APOLLO_R2_0_5', defaultValue: false, description: 'Run Apollo R2.0.5 Japan')
        booleanParam(name: 'APOLLO_R2_1_1', defaultValue: false, description: 'Run Apollo R2.1.1')
        booleanParam(name: 'APOLLO_R2_1_2', defaultValue: false, description: 'Run Apollo R2.1.2')
        booleanParam(name: 'APOLLO_CREDIT_MEMO', defaultValue: false, description: 'LOC Credit Memo flows')
        booleanParam(name: 'FINANCING', defaultValue: false, description: 'Run Financing Regression')
        booleanParam(name: 'ANALYTICS', defaultValue: false, description: 'Run ANALYTICS Regression')
        booleanParam(name: 'EDU', defaultValue: false, description: 'Run all EDU tests')
        choice(name: 'INVOICE_VALIDATION', choices: ['False', 'True'], description: 'Run Invoice Validation ?')
        string(name: 'JIRAPAT', defaultValue: 'NjI2Mjk1Mzg1MDU0OjmbRT/D/UpHMhc92q4uPBIPhwYo')
    }

    stages {
        stage('Store Globals') {
            steps {
                script {
                    SAP_INVOICE_VALIDATION = params.INVOICE_VALIDATION == "true"
                }
            }
        }
        stage('Prepare environment') {
            when {
                not {
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        expression {
                            params.CJT == true ||
                                    params.APOLLO_R2_0_5 == true ||
                                    params.APOLLO_R2_1_1 == true ||
                                    params.APOLLO_R2_1_2 == true ||
                                    params.APOLLO_CREDIT_MEMO == true ||
                                    params.EDU == true ||
                                    params.FINANCING == true ||
                                    params.ANALYTICS == true
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
                                    params.APOLLO_R2_0_5 == true ||
                                    params.APOLLO_R2_1_1 == true ||
                                    params.APOLLO_R2_1_2 == true ||
                                    params.APOLLO_CREDIT_MEMO == true ||
                                    params.EDU == true ||
                                    params.FINANCING == true ||
                                    params.ANALYTICS == true
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
        stage('SonarQube code quality scan') {
            when {
                not {
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        expression {
                            params.CJT == true ||
                                    params.APOLLO_R2_0_5 == true ||
                                    params.APOLLO_R2_1_1 == true ||
                                    params.APOLLO_R2_1_2 == true ||
                                    params.APOLLO_CREDIT_MEMO == true ||
                                    params.EDU == true ||
                                    params.FINANCING == true ||
                                    params.ANALYTICS == true
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
        stage('CJT Nightly Regression') {
            when {
                branch 'master'
                triggeredBy 'TimerTrigger'
            }
            steps {
                triggerCJT(serviceBuildHelper, 'INT')
                script {
                    echo 'Sleeping 1 min. between next CJT'
                    sh 'sleep 60'
                }
                triggerCJT(serviceBuildHelper, 'STG')
            }
        }
        stage('CJT Regression') {
            when {
                branch 'master'
                expression {
                    params.CJT == true
                }
            }
            steps {
                triggerCJT(serviceBuildHelper, params.ENVIRONMENT)
            }
        }
        stage('Nightly sleep') {
            when {
                branch 'master'
                triggeredBy 'TimerTrigger'
            }
            steps {
                echo 'Sleeping 10 min.'
                script {
                    sh 'sleep 600'
                }
            }
        }

        stage('Apollo R2.0.5') {
            when {
                branch 'master'
                anyOf {
                    expression {
                        params.APOLLO_R2_0_5 == true
                    }
                }
            }
            steps {
                triggerApolloR2_0_5(serviceBuildHelper, 'STG')
                script {
                    sh 'sleep 600'
                }
            }
        }

        stage('Apollo R2.1.1') {
            when {
                branch 'master'
                expression {
                    params.APOLLO_R2_1_1 == true
                }
            }
            steps {
                triggerApolloR2_1_1(serviceBuildHelper, 'INT')
            }
        }

        stage('Apollo R2.1.2') {
            when {
                branch 'master'
                expression {
                    params.APOLLO_R2_1_2 == true
                }
            }
            steps {
                triggerApolloR2_1_2(serviceBuildHelper, 'INT')
            }
        }

        stage('Apollo Quote 2 Order - Credit Memo') {
            when {
                branch 'master'
                expression {
                    params.APOLLO_CREDIT_MEMO == true
                }
            }
            steps {
                triggerApolloR2_3CreateCreditMemo(serviceBuildHelper)
            }
        }


        stage('EDU Tests') {
            when {
                expression {
                    params.EDU == true
                }
            }
            steps {
                script {
                    triggerTestingHub(serviceBuildHelper)
                }
            }
        }
        stage('Financing Tests') {
            when {
                branch 'master'
                expression {
                    params.FINANCING == true
                }
            }
            steps {
                triggerFinancing(serviceBuildHelper, params.ENVIRONMENT)
            }
        }
        stage('Analysis Tests') {
            when {
                branch 'master'
                expression {
                    params.ANALYTICS == true
                }
            }
            steps {
                triggerAnalytics(serviceBuildHelper, params.ENVIRONMENT)
            }
        }
    }
}

def generateEDUTests(product, plc) {
    return '{"displayname":"Educator Flow - ' + product + '","testcasename":"validateProductActivationByEducator","description":"Activate Educator Product","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"activate-product-educator","testMethod":"validateProductActivationByEducator","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","externalKey":"' + plc + '"},"notsupportedenv":[],"wiki":""},' +
            '{"displayname":"Student Flow - ' + product + '","testcasename":"validateNewStudentSubscription","description":"Student Subscription flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-student-subscription","testMethod":"validateNewStudentSubscription","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","sku":"default:1","email":"","externalKey":"' + plc + '"},"notsupportedenv":[],"wiki":""},' +
            '{"displayname":"Design Competition Mentor Flow - ' + product + '","testcasename":"validateMentorUser","description":"Design competition mentor flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-mentor-user","testMethod":"validateMentorUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","externalKey":"' + plc + '"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"},' +
            '{"displayname":"IT Admin Flow - ' + product + '","testcasename":"validateAdminUser","description":"IT admin flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-edu-admin","testMethod":"validateMentorUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","externalKey":"' + plc + '"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"}'
}

def triggerTestingHub(serviceBuildHelper) {
    def testcases = readJSON text: ('[' +
            generateEDUTests("AutoCAD", "ACD") + ',' +
            generateEDUTests("Revit", "RVT") + ',' +
            generateEDUTests("Fusion 360", "F360") + ',' +
            generateEDUTests("Inventor", "INVNTOR") + ',' +
            generateEDUTests("3ds Max", "3DSMAX") + ',' +
            generateEDUTests("Maya", "MAYA") + ',' +
            generateEDUTests("Civil 3D", "CIV3D") + ',' +
            generateEDUTests("AutoCAD LT", "ACDLT") + ',' +
            generateEDUTests("Navisworks Manage", "NAVMAN") + ',' +
            generateEDUTests("Robot Structural Analysis Professional", "RSAPRO") + ',' +
            '{"displayname":"Educator Flow - Existing User","testcasename":"76cb6265","description":"Validate existing educator user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"educator"},"notsupportedenv":[],"wiki":""},' +
            '{"displayname":"Student Flow - Existing User","testcasename":"76cb6265","description":"Validate existing student user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"student"},"notsupportedenv":[],"wiki":""},' +
            '{"displayname":"Design Competition Mentor Flow - Existing User","testcasename":"76cb6265","description":"Validate existing mentor user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"mentor"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"},' +
            '{"displayname":"IT Admin Flow - Existing User","testcasename":"76cb6265","description":"Validate existing IT Admin user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"itAdmin"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"}' +
            ']')

    def testingHubInputMap = [:]
    def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
    testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
    testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/edu/testcase'
    testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionname":"EDU Deploy Tests","notificationemail":["ece.dcle.platform.automation@autodesk.com", "dcle.dep.metroid@autodesk.com"],"testcases":' +
            new JsonBuilder(testcases[0..2]).toPrettyString() +
            ',"workstreamname":"dclecjt"}'
    println("Starting Testing Hub API Call - estore - All")
    execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
    if (execution_id != null) {
        println('Testing Hub API called successfully - estore - All')
    } else {
        currentBuild.result = 'FAILURE'
        println('Testing Hub API call failed - estore - All')
    }

    sh 'sleep 30'

    for (int i = 1; i < testcases.size() / 3; i++) {
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":' +
                new JsonBuilder(testcases[(i * 3)..Math.min(i * 3 + 2, testcases.size() - 1)]).toPrettyString() +
                ',"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore - All")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
        if (execution_id != null) {
            println('Testing Hub API called successfully - estore - All')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore - All')
        }

        sh 'sleep 30'
    }
}

def triggerApolloR2_3CreateCreditMemo(def serviceBuildHelper) {
    echo 'Initiating Apollo Create Credit Memo for Pay Invoice - All'
    script {
        println("Building Testing Hub API Input Map - All")

        def addresses = readJSON file: "./testdata/addresses.json"

        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionname":"Apollo: Create Credit Memo on ' + params.ENVIRONMENT + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"LOC - Credit Memo - Florida(en_US)","testcasename":"a4710b89","description":"LOC - Credit Memo - US(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-create-credit-memo","testMethod":"validateLocCreateCreditMemo","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","prvexecutionid":"' + params.EXECUTION_ID + '","address":"Autodesk@1297 Miracle Strip Pkwy SE@Fort Walton Beach@32548@9916800100@United States@FL","timezone":"America/Los_Angeles"}},' +
                '{"displayname":"LOC - Credit Memo - CA Alberta(en_CA)","testcasename":"a4710b89","description":"LOC - Credit Memo - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-create-credit-memo","testMethod":"validateLocCreateCreditMemo","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","locale":"en_CA","prvexecutionid":"' + params.EXECUTION_ID + '","address":"AutodeskCA@2379 Kelly Cir SW@Edmonton@T6W 4G3@397202088@Canada@AB","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC - Credit Memo - Italy(it_IT)","testcasename":"a4710b89","description":"LOC - Credit Memo - IT(it_IT)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-create-credit-memo","testMethod":"validateLocCreateCreditMemo","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-IT","sku":"default:1","email":"","emailType":"biz","locale":"it_IT","prvexecutionid":"' + params.EXECUTION_ID + '","address":"Autodesk@Viale delle Province 131@San Michele Di Ganzaria@95040@0367 5117952@Italy@Catania","timezone":"Europe/Rome"}},' +
                '{"displayname":"LOC - Credit Memo - UK(en_GB)","testcasename":"a4710b89","description":"LOC - Credit Memo - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-create-credit-memo","testMethod":"validateLocCreateCreditMemo","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","locale":"en_GB","prvexecutionid":"' + params.EXECUTION_ID + '","address":"Autodesk@Small Heath Business Park Talbot@Birmingham@B10 0HJ@9916800100@United Kingdom","timezone":"Europe/London"}},' +
                '{"displayname":"LOC - Credit Memo - AUS Northern Territory(en_AU)","testcasename":"a4710b89","description":"LOC - Credit Memo - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-create-credit-memo","testMethod":"validateLocCreateCreditMemo","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","locale":"en_AU","prvexecutionid":"' + params.EXECUTION_ID + '","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - flex - All")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - flex - All')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - flex - All')
        }
    }
}

def triggerApolloR2_1_1(def serviceBuildHelper, String env) {
    echo 'Initiating Apollo R2.1.1'
    script {
        println("Building Testing Hub API Input Map - eStore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + env + ' ","executionname":"Apollo: R2.1.1 on ' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                //TODO: placeholder for validation only. To be removed.
                '{"displayname":"GUAC - BiC Native Order US 1 MONTH AUTOCAD","testcasename":"validateBicNativeOrder","description":"BiC Native Order US 1 MONTH AUTOCAD","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","locale":"en_US","sku":"default:1","productName":"autocad","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order US 1 YEAR 3DS MAX","testcasename":"validateBicNativeOrder","description":"BiC Native Order US 1 YEAR 3DS MAX","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","locale":"en_US","sku":"default:1","productName":"3ds-Max","term":"1-YEAR","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order AUS 1 YEAR AUTOCAD","testcasename":"validateBicNativeOrder","description":"BiC Native Order US 1 YEAR AUTOCAD","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","locale":"en_GB","sku":"default:1","productName":"autocad","term":"1-YEAR","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order UK 1 YEAR MAYA","testcasename":"validateBicNativeOrder","description":"BiC Native Order UK 1 YEAR MAYA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","sku":"default:1","productName":"maya","term":"1-YEAR","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order CA 3 YEAR AUTOCAD LT","testcasename":"validateBicNativeOrder","description":"BiC Native Order CA 3 YEAR AUTOCAD LT ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","locale":"en_CA","sku":"default:1","productName":"autocad-lt","term":"3-YEAR","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order IT 3 YEAR AUTOCAD LT","testcasename":"validateBicNativeOrder","description":" BiC Native Order IT 3 YEAR AUTOCAD LT ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-IT","locale":"it_IT","sku":"default:1","productName":"autocad-lt","term":"3-YEAR","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - eStore")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - eStore')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - eStore')
        }
    }
}

def triggerApolloR2_1_2(def serviceBuildHelper, String env) {
    echo 'Initiating Apollo R2.1.2'
    script {
        println("Building Testing Hub API Input Map - O2P")

        def addresses = readJSON file: "./testdata/addresses.json"

        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/o2p/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + env + ' ","executionname":"Apollo: R2.1.2 on ' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"Flex Direct Order - AU - CC","testcasename":"4ef43ece","description":"Flex Direct Order - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Monthly Offer AU - CC","testcasename":"0459fbf4","description":"Q2O SUS - Single Line Item - Monthly Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-monthly","testMethod":"validateQuoteOrderMonthly","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Annual Offer AU - CC","testcasename":"10bb01af","description":"Q2O SUS - Single Line Item - Annual Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual","testMethod":"validateQuoteOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - MYAB Offer AU - CC","testcasename":"6308c0bb","description":"Q2O SUS - Single Line Item - MYAB Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-myab","testMethod":"validateQuoteOrderMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Premium Product Offer AU - CC","testcasename":"e3ecfeb4","description":"Q2O SUS - Single Line Item - Premium Product Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-premium","testMethod":"validateQuoteOrderPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Monthly with Annual Offer AU - CC","testcasename":"a2afad9d","description":"Q2O SUS - Multi Line Item - Monthly with Annual Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-monthly-annual","testMethod":"validateQuoteOrderMonthlyAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Monthly with Annual and Flex Offer AU - CC","testcasename":"c26311ff","description":"Q2O SUS - Multi Line Item - Monthly with Annual and Flex Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-monthly-annual-flex","testMethod":"validateQuoteOrderMonthlyAnnualFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS  Multi Line Item - Monthly with Annual, Flex, MYAB and Premium Offer AU - CC","testcasename":"57223646","description":"Q2O SUS - Multi Line Item - Monthly with Annual, Flex, MYAB and Premium Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-monthly-annual-flex-myab-premium","testMethod":"validateQuoteOrderMonthlyAnnualMYABFLEXPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU - CC","testcasename":"6eb74ce4","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-direct-order","testMethod":"createDirectOrder","parameters":{"application":"ece"},"testdata":{"payment":"CREDITCARD","locale":"en_AU"}},' +
                '{"displayname":"SUS Direct AU - Muiltiline - LOC","testcasename":"0b9bd2c4","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-multiline-order","testMethod":"createMultilineDirectOrder","parameters":{"application":"ece"},"testdata":{"payment":"LOC","locale":"en_AU"}},' +
                '{"displayname":"SUS Direct AU - Returning User - Paypal","testcasename":"776f2a25","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"returning-user","testMethod":"createReturningUserDirectOrder","parameters":{"application":"ece"},"testdata":{"payment":"PAYPAL","locale":"en_AU"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - O2P")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - O2P')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - O2P')
        }
    }
}

def triggerApolloR2_0_5(def serviceBuildHelper, String env) {
    echo 'Initiating Apollo R2.0.5 for Japan'
    script {
        println("Building Testing Hub API Input Map - estore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + env + ' ","executionname":"Apollo: R2.0.5 Japan orders on ' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","piyush.laddha@autodesk.com","Satish.Jupalli@autodesk.com","keshav.prasad.kuruva@autodesk.com","Ameko.Chen@autodesk.com","pavan.venkatesh.malyala@autodesk.com","ramanathan.kasiviswanathan@autodesk.com","arivuchelvan.pandian@autodesk.com","roshan.nampeli@autodesk.com","Joe.Mcqueeney@autodesk.com","tanner.hirakida@autodesk.com","vishal.kaul@autodesk.com","jeong.sohn@autodesk.com","cherry.ngo@autodesk.com"],"jiraTestCycleId":"30825","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
                '{"displayname":"GUAC - BiC Native Order JP CC","testcasename":"validateBicNativeOrder","description":"GUAC - BiC Native Order JP CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16011"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","locale":"ja_JP","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111"}},' +
                '{"displayname":"GUAC - BiC Native Order JP PAYPAL","testcasename":"validateBicNativeOrder","description":"GUAC - BiC Native Order JP PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16012"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-JP","locale":"ja_JP","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111"}},' +
                '{"displayname":"GUAC - MOE order JP CC","testcasename":"validateBicNativeOrderMoe","description":"MOE order JP CC","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16013"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","locale":"ja_JP","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"GUAC - MOE order JP Konbini","testcasename":"validateBicNativeOrderMoe","description":"MOE order JP Konbini","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16013"},"testdata":{"usertype":"new","password":"","payment":"KONBINI","store":"STORE-JP","locale":"ja_JP","store_type":"FAMILY_MART","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","timezone":"Japan/Tokyo"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)

        if (execution_id != null) {
            println('Testing Hub API called successfully - estore')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore')
        }
    }
    script {
        println("Building Testing Hub API Input Map - All")

        def addresses = readJSON file: "./testdata/addresses.json"

        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + env + ' ","executionname":"Apollo: R2.0.5 Japan orders on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"jiraTestCycleId":"30825","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
                '{"displayname":"BiC order Flex - Japan Credit Card","testcasename":"d27c5060","description":"BiC order Flex - Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16014"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC order Flex Estimator - Japan Paypal","testcasename":"fbf7fe55","description":"BiC order Flex - Japan Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"flex-token-estimator","testMethod":"validateFlexTokenEstimatorTool","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16015"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC Flex Direct Order Refund Japan Credit Card","testcasename":"a1c54974","description":"Flex Direct Order Refund Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16016"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Quote 2 Order - Japan Credit Card ","testcasename":"9d3de1c2","description":"Quote 2 Order - Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16017"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","pullFromDataStore":"True","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111@Japan@TOKYO","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"Quote 2 Order Multi line item Order Japan Credit Card","testcasename":"e803e4a4","description":"Quote 2 Order Multi line item Order Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"multiline-quoteorder","testMethod":"validateMultiLineItemQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16018"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","pullFromDataStore":"True","store":"STORE-JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111@Japan@TOKYO","sku":"default:1","email":"","locale":"ja_JP","quantity1":"2000","quantity2":"4000","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"Quote 2 Order SUS and Quote Orders Japan Credit Card","testcasename":"c5558739","description":"Quote 2 Order SUS and Quote Orders Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-sus-quote-orders","testMethod":"validateBicSUSAndQuoteOrders","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16019"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","pullFromDataStore":"True","store":"STORE-JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111@Japan@TOKYO","sku":"default:1","email":"","locale":"ja_JP","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"LOC Q2O Same Purchaser & Payer - Japan","testcasename":"9d3de1c2","description":"LOC Q2O Same Purchaser & Payer - Japan","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16020"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","pullFromDataStore":"True","store":"STORE-JP","locale":"ja_JP","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111@Japan@TOKYO","emailType":"biz","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"LOC Q2O Pay Invoice Same Purchaser & Payer - JP","testcasename":"9329504a","description":"LOC Q2O Pay Invoice Same Purchaser & Payer - JP","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16021"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-JP","locale":"ja_JP","sku":"default:1","email":"","emailType":"biz","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"MOE O2P Order JP - Agent - New ","testcasename":"e2ea9875","description":"MOE O2P Order JP - Agent - New user","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmAgent-moe","testMethod":"validateMoeOdmOpportunityFlowAgent","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16022"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","locale":"ja_JP"}},' +
                '{"displayname":"MOE O2P Order JP - Agent - New - Konbini","testcasename":"e2ea9875","description":"MOE O2P Order JP - Agent - New user - Konbini","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmAgent-moe","testMethod":"validateMoeOdmOpportunityFlowAgent","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16022"},"testdata":{"usertype":"new","password":"","payment":"KONBINI","store":"STORE-JP","store_type":"FAMILY_MART","sku":"default:1","email":"","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","locale":"ja_JP"}},' +
                '{"displayname":"MOE O2P Order JP - Customer - Existing","testcasename":"97993340","description":"MOE O2P Order JP - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmCustomer-moe","testMethod":"validateMoeOdmOpportunityFlowCustomer","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16023"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","locale":"ja_JP"}},' +
                '{"displayname":"MOE O2P Order JP - Customer - Existing - Konbini","testcasename":"97993340","description":"MOE O2P Order JP - Customer - Existing - Konbini","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmCustomer-moe","testMethod":"validateMoeOdmOpportunityFlowCustomer","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16023"},"testdata":{"usertype":"existing","password":"","payment":"KONBINI","store":"STORE-JP","store_type":"FAMILY_MART","sku":"default:1","email":"","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","locale":"ja_JP"}},' +
                '{"displayname":"MOE DTC O2P Order JP - Customer - Existing","testcasename":"2363224d","description":"MOE DTC O2P Order JP - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-returningUserOdmDtc-moe","testMethod":"validateMoeOdmDtcFlowReturningCustomer","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16023"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","locale":"ja_JP"}},' +
                '{"displayname":"BiC order Flex - Japan Konbini","testcasename":"d27c5060","description":"BiC order Flex - Japan Konbini","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16025"},"testdata":{"usertype":"new","password":"","payment":"KONBINI","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","store_type":"FAMILY_MART","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","sapValidation":"' + params.INVOICE_VALIDATION + '"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - flex - All")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - flex - All')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - flex - All')
        }
    }
    script {
        println("Building Testing Hub API Input Map - accountportal")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Apollo: R2.0.5 Japan orders on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"jiraTestCycleId":"30825","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
                '{"displayname":"MOAB - Reseller  Pay invoices Japan with Cash - Japan","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - Japan","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16938"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-JP","purchaserEmail":"Reseller_JP_DCLE_zLGQXO@letscheck.pw","csn":"5500971276","sku":"default:1","email":"","locale":"ja_JP"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices Japan with ATM - Japan","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with ATM - Japan","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"8794","jiraId":"APLR2PMO-16938"},"testdata":{"usertype":"new","password":"","payment":"ATM","store":"STORE-JP","purchaserEmail":"Reseller_JP_DCLE_zLGQXO@letscheck.pw","csn":"5500971276","sku":"default:1","email":"","locale":"ja_JP"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - accountportal")

        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - accountportal')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - accountportal')
        }
    }
}

def triggerCJT(def serviceBuildHelper, String env) {
    echo 'Initiating Customer Lifecycle Tests - Regression'
    script {
        println("Building Testing Hub API Input Map - estore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"GUAC - BiC Native Multi line item Order","testcasename":"validateMultiLineItemBicNativeOrder","description":"BiC Native Multi line item Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-multiline-bicorder","testMethod":"validateMultiLineItemBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@Alabama", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order US","testcasename":"validateBicNativeOrder","description":"BiC Native Order - US ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order UK BACS","testcasename":"validateBicNativeOrder","description":"BiC Native Order - UK BACS","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order DE  SEPA","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - SEPA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order DE  GIROPAY","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - GIROPAY","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - Add seats from GUAC","testcasename":"validateBicAddSeats","description":"Add seats from GUAC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - MOE order","testcasename":"validateBicNativeOrderMoe","description":"MOE order","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC refund order PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC refund order - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC refund order VISA","testcasename":"validateBicRefundOrder","description":"BiC refund order - VISA","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC order with existing user","testcasename":"validateBicReturningUser","description":"BiC order with existing user","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC renew order","testcasename":"validateRenewBicOrder","description":"BiC renew recurring order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","address":"Autodesk@1245 Alpine Ave@Boulder@80304@9916800100@United States@CO", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC refund order UK","testcasename":"validateBicRefundOrder","description":"BiC refund order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","locale":"en_GB","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"WAPE Health Monitor - 001 - Tealium","testcasename":"5810b037","description":"WAPE Health Monitor - 001 - Tealium","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"tealium-network-logs","testMethod":"validateTealiumNetworkLogs","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE Health Monitor - 002 - Google Analytics","testcasename":"3fe26a1b","description":"WAPE Health Monitor - 002 - Google Analytics","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"google-network-logs","testMethod":"validateGoogleNetworkLogsAndTags","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE Health Monitor - 003 - Adobe Analytics","testcasename":"3a9c7241","description":"WAPE Health Monitor - 003 - Adobe Analytics","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"adobe-network-logs","testMethod":"validateAdobeNetworkLogsAndTags","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"BiC Mini Cart Multiple Product Order","testcasename":"d25acd9a","description":"BiC Mini Cart Multiple Product Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-multiproduct-minicart","testMethod":"validateAddingMultiProductMiniCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@Alabama"}},' +
                '{"displayname":"BiC Mini Cart Delete Product","testcasename":"af06c61b","description":"BiC Mini Cart Delete Product","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-delete-product-minicart","testMethod":"validateDeleteProductFromMiniCart","parameters":{"application":"ece"},"testdata":{"store":"STORE-NAMER","sku":"default:1"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
        if (execution_id != null) {
            println('Testing Hub API called successfully - estore')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore')
        }
    }
    script {
        println("Building Testing Hub API Input Map - accountportal")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"Account Portal - Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Account Portal - Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Account Portal - Change Payment","testcasename":"validateBICChangePaymentProfile","description":"Change Payment from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-changePayment","testMethod":"validateBICChangePaymentProfile","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"Account Portal - Switch Term","testcasename":"validateBicNativeOrderSwitchTerm","description":"Switch Term for BiC Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder-switch-term","testMethod":"validateBicNativeOrderSwitchTerm","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"Account Portal - Restart Subscription","testcasename":"validateRestartSubscription","description":"Restart a Canceled Subscription","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-restart-subscription","testMethod":"validateRestartSubscription","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{}},' +
                '{"displayname":"Account Portal - Align Billing","testcasename":"validateAlignBilling","description":"Align 2 Subscriptions to same Renewal from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-align-billing","testMethod":"validateAlignBilling","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - accountportal")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - accountportal')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - accountportal')
        }
    }
    script {
        if (env == "STG") {
            println("Building Testing Hub API Input Map - accountportal")
            def testingHubInputMap = [:]
            def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
            testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
            testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash & CM - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash & CM- US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","applyCM":"Y","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with BACS - UK","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with BACS- UK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","purchaserEmail":"Reseller_UK_DCLE_2ZgMkv@letscheck.pw","csn":"5500971062","sku":"default:1","email":"","locale":"en_GB"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with SEPA - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-IT","purchaserEmail":"Reseller_IT_DCLE_9zbCbX@letscheck.pw","csn":"5500971063","sku":"default:1","email":"","locale":"it_IT"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with GIROPAY - Germany","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with GIROPAY- Germany","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","purchaserEmail":"Reseller_DE_PWS-Performance_kufcb@letscheck.pw","csn":"5500989129","sku":"default:1","email":"","locale":"de_DE"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - CA","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- CA","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-CA","purchaserEmail":"Reseller_CA_DCLE_s1wWRX@letscheck.pw","csn":"5500971257","sku":"default:1","email":"","locale":"en_CA"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - AU","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- AU","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-AUS","purchaserEmail":"Reseller_AU_DCLE_ppHDJ5@letscheck.pw","csn":"5500971071","sku":"default:1","email":"","locale":"en_AU"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-IT","purchaserEmail":"Reseller_IT_DCLE_9zbCbX@letscheck.pw","csn":"5500971063","sku":"default:1","email":"","locale":"it_IT"}}' +
                    '],"workstreamname":"dclecjt"}'
            println("Starting Testing Hub API Call - accountportal")
            if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
                println('Testing Hub API called successfully - accountportal')
            } else {
                currentBuild.result = 'FAILURE'
                println('Testing Hub API call failed - accountportal')
            }
        } else if (env == "INT") {
            println("Building Testing Hub API Input Map - accountportal")
            def testingHubInputMap = [:]
            def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
            testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
            testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"thpu7OwL5kk97h@letscheck.pw","csn":"5501308785","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash & CM - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash & CM- US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"thpu7OwL5kk97h@letscheck.pw","csn":"5501308785","applyCM":"Y","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with BACS - UK","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with BACS- UK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","purchaserEmail":"thpumurxsSnXxe@letscheck.pw","csn":"5501308790","sku":"default:1","email":"","locale":"en_GB"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with SEPA - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-IT","purchaserEmail":"thpuWib85OHCcS@letscheck.pw","csn":"5501276951","sku":"default:1","email":"","locale":"it_IT"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with GIROPAY - Germany","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with GIROPAY- Germany","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","purchaserEmail":"thpuRQSOrg6Sqg@letscheck.pw","csn":"5501308975","sku":"default:1","email":"","locale":"de_DE"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-NAMER","purchaserEmail":"thpu7OwL5kk97h@letscheck.pw","csn":"0070176510","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - CA","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- CA","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-CA","purchaserEmail":"thpuXa8oSCwst0@letscheck.pw","csn":"5501308786","sku":"default:1","email":"","locale":"en_CA"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - AU","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- AU","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-AUS","purchaserEmail":"thpuRKbjdVbZwt@letscheck.pw","csn":"5501308787","sku":"default:1","email":"","locale":"en_AU"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-IT","purchaserEmail":"thpuxzsJzkISws@letscheck.pw","csn":"5501308820","sku":"default:1","email":"","locale":"it_IT"}}' +
                    '],"workstreamname":"dclecjt"}'
            println("Starting Testing Hub API Call - accountportal")
            if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
                println('Testing Hub API called successfully - accountportal')
            } else {
                currentBuild.result = 'FAILURE'
                println('Testing Hub API call failed - accountportal')
            }
        }
    }
    script {
        println("Building Testing Hub API Input Map - flex")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '","executionid":"' + execution_id + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC order Flex US VISA","testcasename":"d27c5060","description":"BiC order new Flex US VISA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC order Flex DE GIROPAY","testcasename":"d27c5060","description":"BiC Flex Direct Order - DE - GIROPAY","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Quote 2 Order Multi line item Order US PAYPAL","testcasename":"e803e4a4","description":"Quote 2 Order Multi line item Order US PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"multiline-quoteorder","testMethod":"validateMultiLineItemQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","pullFromDataStore":"True","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","quantity1":"2000","quantity2":"4000","isMultiLineItem":"True","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/Los_Angeles"}},' +
                '{"displayname":"Quote 2 Order UK BACS","testcasename":"9d3de1c2","description":"Quote 2 Order UK BACS","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","pullFromDataStore":"True","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Europe/London"}},' +
                '{"displayname":"Quote 2 Order AUS CREDIT CARD","testcasename":"9d3de1c2","description":"Quote 2 Order AUS CREDIT CARD","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","pullFromDataStore":"True","store":"STORE-AUS","sku":"default:1","email":"","isTaxed":"Y","locale":"en_AU","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"AutodeskAU@114 Darlinghurst Rd@Darlinghurst@2010@397202088@Australia@NSW","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Quote 2 Order SUS and CA CREDIT CARD","testcasename":"c5558739","description":"Quote 2 Order SUS and Quote Orders CA CREDIT CARD","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-sus-quote-orders","testMethod":"validateBicSUSAndQuoteOrders","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","isTaxed":"Y","locale":"en_CA","sapValidation":"' + params.INVOICE_VALIDATION + '","emailType":"biz","address":"AutodeskCA@2379 Kelly Cir SW@Edmonton@T6W 4G3@397202088@Canada@AB","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O Same Purchaser & Payer - US VISA","testcasename":"9d3de1c2","description":"LOC Q2O Same Purchaser & Payer - US VISA","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","pullFromDataStore":"True","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"TTR Q2O CA CREDIT CARD","testcasename":"9d3de1c2","description":"TTR Q2O CA CREDIT CARD","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","newPaymentType":"CREDITCARD","pullFromDataStore":"True","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","submitTaxInfo":"true","locale":"en_CA","sapValidation":"False","address":"Autodesk@721 Government St@Victoria@V8W 1W5@9916800100@Canada@BC","timezone":"Canada/Pacific"}},' +
                '{"displayname":"MOE O2P Order USA - Agent - New","testcasename":"e2ea9875","description":"MOE O2P Order USA - Agent - New user","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmAgent-moe","testMethod":"validateMoeOdmOpportunityFlowAgent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","locale":"en_US"}},' +
                '{"displayname":"MOE O2P Order CA - Customer - Existing","testcasename":"97993340","description":"MOE O2P Order CA - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmCustomer-moe","testMethod":"validateMoeOdmOpportunityFlowCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","locale":"en_CA","address":"CompanyNameCA@4204 Av Northcliffe@Montreal@H4A 3L3@9916800100@Canada@QC"}},' +
                '{"displayname":"MOE DTC O2P Order UK - Customer - Existing","testcasename":"2363224d","description":"MOE DTC O2P Order UK - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-returningUserOdmDtc-moe","testMethod":"validateMoeOdmDtcFlowReturningCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","locale":"en_GB"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - flex")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - flex')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - flex')
        }
    }
    script {
        if (env == "STG") {
            println("Building Testing Hub API Input Map - EDU")
            def testingHubInputMap = [:]
            def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
            testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
            testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/edu/testcase'
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression for EDU","notificationemail":["ece.dcle.platform.automation@autodesk.com", "dcle.dep.metroid@autodesk.com"],"testcases":[' +
                    '{"displayname":"EDU - Educator flow","testcasename":"validateProductActivationByEducator","description":"Activate Educator Product","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"activate-product-educator","testMethod":"validateProductActivationByEducator","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                    '{"displayname":"EDU - Student Flow","testcasename":"validateNewStudentSubscription","description":"Student Subscription flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-student-subscription","testMethod":"validateNewStudentSubscription","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                    '{"displayname":"EDU - Design Competition Mentor Flow","testcasename":"validateMentorUser","description":"Design competition mentor flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-mentor-user","testMethod":"validateMentorUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"},' +
                    '{"displayname":"EDU - IT Admin Flow","testcasename":"validateAdminUser","description":"IT admin flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-edu-admin","testMethod":"validateMentorUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","externalKey":"RVT"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"},' +
                    '{"displayname":"Educator Flow - Existing User","testcasename":"76cb6265","description":"Validate existing educator user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"educator"},"notsupportedenv":[],"wiki":""},' +
                    '{"displayname":"Student Flow - Existing User","testcasename":"76cb6265","description":"Validate existing student user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"student"},"notsupportedenv":[],"wiki":""},' +
                    '{"displayname":"Design Competition Mentor Flow - Existing User","testcasename":"76cb6265","description":"Validate existing mentor user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"mentor"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"},' +
                    '{"displayname":"IT Admin Flow - Existing User","testcasename":"76cb6265","description":"Validate existing IT Admin user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece","store":"STORE-NAMER"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","existingUserType":"itAdmin"},"notsupportedenv":[],"wiki":"https://wiki.autodesk.com/pages/viewpage.action?spaceKey=EFDE&title=Automation+Command+Line"}' +
                    '],"workstreamname":"dclecjt"}'
            println("Starting Testing Hub API Call - EDU Tests")
            if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
                println('Testing Hub API called successfully - EDU Tests')
            } else {
                currentBuild.result = 'FAILURE'
                println('Testing Hub API call failed - EDU Tests')
            }
        }
    }
    script {
        println("Building Testing Hub API Input Map - LOC Apollo R3 Regression")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"LOC Q2O CJT - Alabama(en_US)","testcasename":"9d3de1c2","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"LOC Q2O CJT - CA Ontario(en_CA)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12566"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","sapValidation":"False","address":"Autodesk@246 Lynden Road@Vineland@L0R 2E0@9055624155@Canada@ON","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O CJT - UK(en_GB)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Q2O CJT - Germany(de_DE)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Q2O CJT - AUS Northern Territory(en_AU)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - Alabama(en_US)","testcasename":"9329504a","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - CA Ontario(en_CA)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12566"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","sapValidation":"False","address":"Autodesk@246 Lynden Road@Vineland@L0R 2E0@9055624155@Canada@ON","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - UK(en_GB)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - Germany(de_DE)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - AUS Northern Territory(en_AU)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Customer Wire Payment - LOC Order - ja_JP","testcasename":"9329504a","description":"Verify wire transfer bank payment information for customer order - ja_JP","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice ","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","store":"STORE-JP","newPaymentType":"WIRE_TRANSFER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"ja_JP","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"Customer Wire Payment - LOC Order - en_GB","testcasename":"9329504a","description":"Verify wire transfer bank payment information for customer order - en_GB","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice ","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","store":"STORE-UK","newPaymentType":"WIRE_TRANSFER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"Customer Wire Payment - LOC Order - en_CA","testcasename":"9329504a","description":"Verify wire transfer bank payment information for customer order - en_CA","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice ","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","store":"STORE-CA","newPaymentType":"WIRE_TRANSFER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","timezone":"Canada/Pacific"}},' +
                '{"displayname":"Customer Wire Payment - LOC Order - en_US","testcasename":"9329504a","description":"Verify wire transfer bank payment information for customer order - en_US","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice ","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","store":"STORE-NAMER","newPaymentType":"WIRE_TRANSFER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","timezone":"America/New_York"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - LOC Tests")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - LOC Tests')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - LOC Tests')
        }
    }
    script {
        if (env == "STG") {
            println("Building Testing Hub API Input Map - estore")
            def testingHubInputMap = [:]
            def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
            testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
            testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                    '{"displayname":"DotCom - BiC Trial Download","testcasename":"validateTrialDownloadUI","description":"BiC Trial Download","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"trialDownload-UI","testMethod":"validateTrialDownloadUI","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"VISA","store":"STORE-NAMER","sku":"default:1"}}' +
                    '],"workstreamname":"dclecjt"}'
            println("Starting Testing Hub API Call - estore")
            if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
                println('Testing Hub API called successfully - estore')
            } else {
                currentBuild.result = 'FAILURE'
                println('Testing Hub API call failed - estore')
            }
        }
    }
}

def triggerFinancing(def serviceBuildHelper, String env) {
    echo 'Initiating Financing Tests'
    script {
        println("Building Testing Hub API Input Map - estore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Financing Regression on ' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","pavan.venkatesh.malyala@autodesk.com","jeong.sohn@autodesk.com","anjani.singh@autodesk.com", "cherry.ngo@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC Financing Order","testcasename":"validateBicNativeOrder","description":"BiC Financing Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC Financing Flex Order","testcasename":"34de7a6d","description":"BiC Financing Flex Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Canceled Order","testcasename":"validateBicNativeFinancingCanceledOrder","description":"BiC Financing Order Canceled","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-canceled","testMethod":"validateBicNativeFinancingCanceledOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Declined Order","testcasename":"validateBicNativeFinancingDeclinedOrder","description":"BiC Financing Order Declined","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-declined","testMethod":"validateBicNativeFinancingDeclinedOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Renew Order","testcasename":"783c495f","description":"BiC Financing Renew Order","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-renew-order","testMethod":"validateBicNativeFinancingRenewalOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Refund Flex Order","testcasename":"ffae8105","description":"BiC Financing Refund Flex Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
        if (execution_id != null) {
            println('Testing Hub API called successfully - estore')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore')
        }
    }
    if (env == "STG") {
        //NOTE: INT env not configure to support Financing Renewal.
        script {
            println("Building Testing Hub API Input Map - estore")
            def testingHubInputMap = [:]
            def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
            testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
            testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Financing Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","pavan.venkatesh.malyala@autodesk.com","jeong.sohn@autodesk.com","anjani.singh@autodesk.com", "cherry.ngo@autodesk.com"],"testcases":[' +
                    '{"displayname":"BiC Financing Renew Order","testcasename":"783c495f","description":"BiC Financing Renew Order","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-renew-order","testMethod":"validateBicNativeFinancingRenewalOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}}' +
                    '],"workstreamname":"dclecjt"}'
            println("Starting Testing Hub API Call - estore")
            if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
                println('Testing Hub API called successfully - estore')
            } else {
                currentBuild.result = 'FAILURE'
                println('Testing Hub API call failed - estore')
            }
        }
    }
    script {
        println("Building Testing Hub API Input Map - flex")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Financing Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","pavan.venkatesh.malyala@autodesk.com","jeong.sohn@autodesk.com","anjani.singh@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC Financing Q2O Order","testcasename":"9d3de1c2","description":"BiC Financing Q2O Order","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":"","timezone":"America/New_York"}},' +
                '{"displayname":"MOE Financing ODM DTC O2P Order - Customer","testcasename":"28d21011","description":"MOE Financing ODM DTC O2P Order - Customer","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmDtcCustomer-moe","testMethod":"validateMoeOdmDtcFlowCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":"","quantity":"400","timezone":"America/New_York"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - flex")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - flex')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - flex')
        }
    }
}

def triggerAnalytics(def serviceBuildHelper, String env) {
    echo 'Initiating Analytics Tests'
    script {
        println("Building Testing Hub API Input Map - estore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Analytics Regression on ' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","abhijit.rajurkar@autodesk.com","adam.hill@autodesk.com"],"testcases":[' +
                '{"displayname":"WAPE Health Monitor - 001 - Tealium","testcasename":"5810b037","description":"WAPE Health Monitor - 001 - Tealium","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"tealium-network-logs","testMethod":"validateTealiumNetworkLogs","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE Health Monitor - 002 - Google Analytics","testcasename":"3fe26a1b","description":"WAPE Health Monitor - 002 - Google Analytics","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"google-network-logs","testMethod":"validateGoogleNetworkLogsAndTags","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE Health Monitor - 003 - Adobe Analytics","testcasename":"3a9c7241","description":"WAPE Health Monitor - 003 - Adobe Analytics","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"adobe-network-logs","testMethod":"validateAdobeNetworkLogsAndTags","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 001 - Cookies Valid - Test page - Before Consent","testcasename":"85f95d88","description":"WAPE GDPR Validation - GDPR Site - 001 - Cookies Valid - Test page - Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-cookies-before-consent","testMethod":"validateGDPRCookiesBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 002 - Footer Banner Shown - Test page - Before Consent","testcasename":"6633b8bd","description":"WAPE GDPR Validation - GDPR Site - 002 - Footer Banner Shown - Test page - Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-footer-banner-before-consent","testMethod":"validateGDPRFooterBannerBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 003 - Mandatory Tags Only - Test page - Before Consent","testcasename":"19aa6e3d","description":"WAPE GDPR Validation - GDPR Site - 003 - Mandatory Tags Only - Test page - Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-mandatory-tags-not-fired-before-consent","testMethod":"validateGdprMandatoryTagsNotFiredBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 004 - Heartbeat Tag - Test page - Before Consent","testcasename":"c8016bf0","description":"WAPE GDPR Validation - GDPR Site - 004 - Heartbeat Tag - Test page - Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-google-tags-before-consent","testMethod":"validateGDPRGoogleNetworkTagsBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 005 - Cookies Valid - Test page - After Consent","testcasename":"9e5efee8","description":"WAPE GDPR Validation - GDPR Site - 005 - Cookies Valid - Test page - After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-cookies-after-consent","testMethod":"validateGDPRCookiesAfterConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 006 - Footer Banner Hidden - Test page - After Consent","testcasename":"387c6e2c","description":"WAPE GDPR Validation - GDPR Site - 006 - Footer Banner Hidden - Test page - After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-footer-banner-after-consent","testMethod":"validateGDPRFooterBannerAfterConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 007 - All Tags - Test page - After Consent","testcasename":"88c7224d","description":"WAPE GDPR Validation - GDPR Site - 007 - All Tags - Test page - After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-mandatory-tags-fired-after-consent","testMethod":"validateGdprMandatoryTagsFiredAfterConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 008 - Cookies Valid - Next Page - After Consent","testcasename":"ecf50dad","description":"WAPE GDPR Validation - GDPR Site - 008 - Cookies Valid - Next Page - After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-cookies-after-next-page-load","testMethod":"validateGDPRCookiesOnNextPageLoad","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 009 - Footer Banner Hidden - Next Page - After Consent","testcasename":"e0bc731f","description":"WAPE GDPR Validation - GDPR Site - 009 - Footer Banner Hidden - Next Page - After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-footer-banner-after-next-page-load","testMethod":"validateGDPRFooterBannerOnNextPageLoad","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"WAPE GDPR Validation - GDPR Site - 010 - All Tags - Next Page - After Consent","testcasename":"21ff36cf","description":"WAPE GDPR Validation - GDPR Site - 010 - All Tags - Next Page - After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-mandatory-tags-fired-after-next-page-load","testMethod":"validateGdprMandatoryTagsFiredOnNextPageLoad","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
                '{"displayname":"GUAC - BiC Native Multi line item Order","testcasename":"validateMultiLineItemBicNativeOrder","description":"BiC Native Multi line item Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-multiline-bicorder","testMethod":"validateMultiLineItemBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","applyAnalytics":"True","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@Alabama", "sapValidation":"' + params.INVOICE_VALIDATION + '"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
        if (execution_id != null) {
            println('Testing Hub API called successfully - estore - Analytics')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore - Analytics')
        }
    }
    script {
        if (env == "STG") {
            println("Building Testing Hub API Input Map - EDU")
            def testingHubInputMap = [:]
            def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
            testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
            testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/edu/testcase'
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Analytics Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","abhijit.rajurkar@autodesk.com","adam.hill@autodesk.com"],"testcases":[' +
                    '{"displayname":"Educator Flow - Existing User","testcasename":"76cb6265","description":"Validate existing educator user still has EDU status","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-existing-user","testMethod":"validateExistingUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","applyAnalytics":"True","existingUserType":"educator"},"notsupportedenv":[],"wiki":""},' +
                    '{"displayname":"EDU - Student Flow","testcasename":"validateNewStudentSubscription","description":"Student Subscription flow","testClass":"com.autodesk.ece.bic.testsuites.EDUUserFlows","testGroup":"validate-student-subscription","testMethod":"validateNewStudentSubscription","parameters":{"application":"ece"},"testdata":{"usertype":"new","payment":"ACH","password":"","store":"STORE-NAMER","applyAnalytics":"True","sku":"default:1","email":""}}' +
                    '],"workstreamname":"dclecjt"}'
            println("Starting Testing Hub API Call - EDU Tests")
            if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
                println('Testing Hub API called successfully - edu - Analytics')
            } else {
                currentBuild.result = 'FAILURE'
                println('Testing Hub API call failed - edu - Analytics')
            }
        }
    }
    script {
        println("Building Testing Hub API Input Map - flex")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Analytics Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","abhijit.rajurkar@autodesk.com","adam.hill@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC order Flex","testcasename":"d27c5060","description":"BiC order new Flex","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","applyAnalytics":"True","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - AUS Northern Territory(en_AU)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","applyAnalytics":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - flex")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - flex - Analytics')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - flex - Analytics')
        }
    }
    script {
        println("Building Testing Hub API Input Map - accountportal")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/accountportal/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Analytics Regression on ' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","abhijit.rajurkar@autodesk.com","adam.hill@autodesk.com"],"testcases":[' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash & CM - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash & CM - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","applyCM":"Y","sku":"default:1","applyAnalytics":"True","email":"","locale":"en_US"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - accountportal")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - accountportal - Analytics')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - accountportal - Analytics')
        }
    }
}
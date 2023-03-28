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
        booleanParam(name: 'MOAB', defaultValue: false, description: 'Run MOAB')
        booleanParam(name: 'CJT', defaultValue: false, description: 'Run CJT Regression')
        string(name: 'EXECUTION_ID', defaultValue: '', description: 'Enter previous Execution ID')
        booleanParam(name: 'APOLLO_R2_0_4', defaultValue: false, description: 'Run Apollo R2.0.4')
        booleanParam(name: 'APOLLO_R2_0_5', defaultValue: false, description: 'Run Apollo R2.0.5 Japan')
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
                                    params.MOAB == true ||
                                    params.APOLLO_R2_0_4 == true ||
                                    params.APOLLO_R2_0_5 == true ||
                                    params.APOLLO_CREDIT_MEMO == true ||
                                    params.EDU == true ||
                                    params.FINANCING == true
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
                                    params.MOAB == true ||
                                    params.APOLLO_R2_0_4 == true ||
                                    params.APOLLO_R2_0_5 == true ||
                                    params.APOLLO_CREDIT_MEMO == true ||
                                    params.EDU == true ||
                                    params.FINANCING == true
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
                                    params.MOAB == true ||
                                    params.APOLLO_R2_0_4 == true ||
                                    params.APOLLO_R2_0_5 == true ||
                                    params.APOLLO_CREDIT_MEMO == true ||
                                    params.EDU == true ||
                                    params.FINANCING == true
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
        stage('MOAB Tests') {
            when {
                branch 'master'
                anyOf {
                    triggeredBy 'TimerTrigger'
                    expression {
                        params.MOAB == true
                    }
                }
            }
            steps {
                triggerMOABTests(serviceBuildHelper, 'STG')
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

        stage('Apollo R2.0.4') {
            when {
                branch 'master'
                anyOf {
                    triggeredBy 'TimerTrigger'
                    expression {
                        params.APOLLO_R2_0_4 == true
                    }
                }
            }
            steps {
                triggerApolloR2_0_4(serviceBuildHelper)
                script {
                    sh 'sleep 600'
                }
            }
        }

       stage('Apollo R2.0.5') {
            when {
                branch 'master'
                anyOf {
                    triggeredBy 'TimerTrigger'
                    expression {
                        params.APOLLO_R2_0_5 == true
                    }
                }
            }
            steps {
                triggerApolloR2_0_5(serviceBuildHelper)
                script {
                    sh 'sleep 600'
                }
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

def triggerApolloR2_0_5(def serviceBuildHelper) {
    echo 'Initiating Apollo R2.0.5 for Japan'
    script {
       println("Building Testing Hub API Input Map - estore")
       def testingHubInputMap = [:]
       def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
       testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
       testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
       testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionname":"Apollo: Apollo R2.0.5 Japan orders on ' + params.ENVIRONMENT + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"jiraTestCycleId":"29733","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
               '{"displayname":"GUAC - BiC Native Order JP CC","testcasename":"validateBicNativeOrder","description":"GUAC - BiC Native Order JP CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13812"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府","locale":"ja_JP"}},' +
               '{"displayname":"GUAC - BiC Native Order JP PAYPAL","testcasename":"validateBicNativeOrder","description":"GUAC - BiC Native Order JP PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13837"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-JP","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府","locale":"ja_JP"}}' +
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
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC order Flex - Japan Credit Card","testcasename":"d27c5060","description":"BiC order Flex - Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府","sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC order Flex Estimator - Japan Paypal","testcasename":"fbf7fe55","description":"BiC order Flex - Japan Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"flex-token-estimator","testMethod":"validateFlexTokenEstimatorTool","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC Flex Direct Order Refund Japan Credit Card","testcasename":"a1c54974","description":"Flex Direct Order Refund Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Quote 2 Order - Japan Credit Card ","testcasename":"9d3de1c2","description":"Quote 2 Order - Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","isTaxed":"Y","locale":"ja_JP","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"Quote 2 Order Multi line item Order Japan Credit Card","testcasename":"e803e4a4","description":"Quote 2 Order Multi line item Order Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"multiline-quoteorder","testMethod":"validateMultiLineItemQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","isTaxed":"Y","locale":"ja_JP","quantity1":"2000","quantity2":"4000","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"Quote 2 Order SUS and Quote Orders Japan Credit Card","testcasename":"c5558739","description":"Quote 2 Order SUS and Quote Orders Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-sus-quote-orders","testMethod":"validateBicSUSAndQuoteOrders","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","isTaxed":"Y","locale":"ja_JP","sapValidation":"' + params.INVOICE_VALIDATION + '","emailType":"biz","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"LOC Q2O Same Purchaser & Payer - Japan","testcasename":"9d3de1c2","description":"LOC Q2O Same Purchaser & Payer - Japan","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-JP","locale":"ja_JP","sku":"default:1","email":"","locale":"ja_JP","emailType":"biz","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"LOC Q2O Pay Invoice Same Purchaser & Payer - JP","testcasename":"9329504a","description":"LOC Q2O Pay Invoice Same Purchaser & Payer - JP","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"MOE O2P Order JP - Agent - New ","testcasename":"e2ea9875","description":"MOE O2P Order JP - Agent - New user","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmAgent-moe","testMethod":"validateMoeOdmOpportunityFlowAgent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府","locale":"ja_JP"}},' +
                '{"displayname":"MOE O2P Order JP - Customer - Existing","testcasename":"97993340","description":"MOE O2P Order JP - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmCustomer-moe","testMethod":"validateMoeOdmOpportunityFlowCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府","locale":"ja_JP"}},' +
                '{"displayname":"MOE DTC O2P Order JP - Customer - Existing","testcasename":"2363224d","description":"MOE DTC O2P Order JP - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-returningUserOdmDtc-moe","testMethod":"validateMoeOdmDtcFlowReturningCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@163-8001@03-5321-1111@Japan@大阪府","locale":"ja_JP"}}' +
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

def triggerApolloR2_0_4(def serviceBuildHelper) {
    echo 'Initiating Apollo R2.0.4'
    script {
        println("Building Testing Hub API Input Map - estore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionname":"Apollo: R2.0.4 PSP SSS orders on ' + params.ENVIRONMENT + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","Sally.Gillespie@autodesk.com","harman.preet@autodesk.com","sai.saripalli@autodesk.com","piyush.laddha@autodesk.com","Satish.Jupalli@autodesk.com","manoj.t.l@autodesk.com","keshav.prasad.kuruva@autodesk.com","Ameko.Chen@autodesk.com","pavan.venkatesh.malyala@autodesk.com","ramanathan.kasiviswanathan@autodesk.com","arivuchelvan.pandian@autodesk.com","nimit.shah@autodesk.com","roshan.nampeli@autodesk.com","Joe.Mcqueeney@autodesk.com","gaurav.bains@autodesk.com","rohit.rana@autodesk.com","chris.gouldy@autodesk.com","tanner.hirakida@autodesk.com","mahija.sarma@autodesk.com","jeong.sohn@autodesk.com","Cherry.ngo@autodesk.com","erik.batz@autodesk.com"],"jiraTestCycleId":"29733","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
                '{"displayname":"GUAC - BiC Native Order UK","testcasename":"validateBicNativeOrder","description":"BiC Native Order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13812"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Order UK PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13820"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Order NL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - NL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13811"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"nl_NL"}},' +
                '{"displayname":"GUAC - BiC Native Order NL PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - NL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13819"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","locale":"nl_NL"}},' +
                '{"displayname":"GUAC - BiC Native Order DK","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13815"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-DK","sku":"default:1","email":"","locale":"da_DK"}},' +
                '{"displayname":"GUAC - BiC Native Order DK PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DK PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13823"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-DK","sku":"default:1","email":"","locale":"da_DK"}},' +
                '{"displayname":"GUAC - BiC Native Order SE","testcasename":"validateBicNativeOrder","description":"BiC Native Order - SE","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13818"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-SE","sku":"default:1","email":"","locale":"sv_SE"}},' +
                '{"displayname":"GUAC - BiC Native Order SE PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - SE PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13826"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-SE","sku":"default:1","email":"","locale":"sv_SE"}},' +
                '{"displayname":"GUAC - BiC Native Order NO","testcasename":"validateBicNativeOrder","description":"BiC Native Order - NO","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13817"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-NO","sku":"default:1","email":"","locale":"no_NO"}},' +
                '{"displayname":"GUAC - BiC Native Order NO PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - NO PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13825"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-NO","sku":"default:1","email":"","locale":"no_NO"}},' +
                '{"displayname":"GUAC - BiC Native Order PL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - PL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13816"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-PL","sku":"default:1","email":"","locale":"pl_PL"}},' +
                '{"displayname":"GUAC - BiC Native Order PL PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - PL PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13824"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-PL","sku":"default:1","email":"","locale":"pl_PL"}},' +
                '{"displayname":"GUAC - BiC Native Order CH","testcasename":"validateBicNativeOrder","description":"BiC Native Order - CH","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13813"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CH","sku":"default:1","email":"","locale":"fr_CH"}},' +
                '{"displayname":"GUAC - BiC Native Order CH PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - CH PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13821"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-CH","sku":"default:1","email":"","locale":"fr_CH"}},' +
                '{"displayname":"GUAC - BiC Native Order CZ","testcasename":"validateBicNativeOrder","description":"BiC Native Order - CZ ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13814"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CZ","sku":"default:1","email":"","locale":"cs_CZ"}},' +
                '{"displayname":"GUAC - BiC Native Order CZ PAYPAL","testcasename":"validateBicNativeOrder","description":"BiC Native Order - CZ PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13822"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-CZ","sku":"default:1","email":"","locale":"cs_CZ"}},' +
                '{"displayname":"BiC Native Order UK - VAT ID","testcasename":"validateBicNativeOrder","description":"BiC Native Order UK Using VAT ID","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13831"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB","taxId":"107328000","testtype":"initialorderbic"}},' +
                '{"displayname":"BiC Native Order FR - VAT ID","testcasename":"validateBicNativeOrder","description":"BiC Native Order FR Using VAT ID","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13832"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-FR","sku":"default:1","email":"","locale":"fr_FR","taxId":"74799222823","testtype":"initialorderbic","sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Refund Order UK","testcasename":"validateBicRefundOrder","description":"BiC Refund Order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13828"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Refund Order UK PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC Refund Order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13830"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Refund Order NL","testcasename":"validateBicRefundOrder","description":"BiC Refund Order - NL","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13827"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"nl_NL"}},' +
                '{"displayname":"GUAC - BiC Refund Order NL PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC Refund Order - NL","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13829"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","locale":"nl_NL"}},' +
                '{"displayname":"GUAC - BiC Refund Old Order UK","testcasename":"4d1fedd7","description":"Refund Old Order PSP - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"refundOrder-PSP","testMethod":"validateRefundOrderPSP","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13840"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Refund Old Order UK PAYPAL","testcasename":"4d1fedd7","description":"Refund Old Order PSP - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"refundOrder-PSP","testMethod":"validateRefundOrderPSP","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13839"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Refund Old Order DE","testcasename":"4d1fedd7","description":"Refund Old Order PSP - DE","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"refundOrder-PSP","testMethod":"validateRefundOrderPSP","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13842"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"}},' +
                '{"displayname":"GUAC - BiC Refund Old Order DE PAYPAL","testcasename":"4d1fedd7","description":"Refund Old Order PSP - DE","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"refundOrder-PSP","testMethod":"validateRefundOrderPSP","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13841"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"}},' +
                '{"displayname":"GUAC - BiC Native Renewal UK","testcasename":"validateRenewBicOrder","description":"Native Renewal - UK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13834"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Renewal UK PAYPAL","testcasename":"validateRenewBicOrder","description":"BiC Renewal Seats - UK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13836"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Renewal PT","testcasename":"validateRenewBicOrder","description":"BiC Native Renewal - PT","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13835"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"pt_PT"}},' +
                '{"displayname":"GUAC - BiC Native Renewal PT PAYPAL","testcasename":"validateRenewBicOrder","description":"BiC Native Renewal - PT","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13833"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","locale":"pt_PT"}},' +
                '{"displayname":"GUAC - BiC Native Add Seats UK","testcasename":"validateBicAddSeats","description":"BiC Native Add Seats - UK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13838"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Add Seats UK PAYPAL","testcasename":"validateBicAddSeats","description":"BiC Native Add Seats - UK","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-14084"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Add Seats PT","testcasename":"validateBicAddSeats","description":"BiC Native Add Seats - PT","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-14085"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","locale":"pt_PT"}},' +
                '{"displayname":"GUAC - BiC Native Add Seats PT PAYPAL","testcasename":"validateBicAddSeats","description":"BiC Native Add Seats - PT","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13837"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","locale":"pt_PT"}}' +
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
        println("Building Testing Hub API Input Map - flex")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionid":"' + execution_id + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com"],"jiraTestCycleId":"29733","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
                '{"displayname":"Flex Direct Order UK CC","testcasename":"d27c5060","description":"Flex Direct Order UK CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13845"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB"}},' +
                '{"displayname":"Flex Direct Order UK Paypal","testcasename":"d27c5060","description":"Flex Direct Order UK Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13853"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB"}},' +
                '{"displayname":"Flex Direct Order Sweden CC","testcasename":"d27c5060","description":"Flex Direct Order SE CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13851"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-SE","sku":"default:1","email":"","isTaxed":"Y","locale":"sv_SE"}},' +
                '{"displayname":"Flex Direct Order Sweden Paypal","testcasename":"d27c5060","description":"Flex Direct Order SE Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13859"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-SE","sku":"default:1","email":"","isTaxed":"Y","locale":"sv_SE"}},' +
                '{"displayname":"Flex Direct Order Norway CC","testcasename":"d27c5060","description":"Flex Direct Order NO CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13850"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-NO","sku":"default:1","email":"","isTaxed":"Y","locale":"no_NO"}},' +
                '{"displayname":"Flex Direct Order Norway Paypal","testcasename":"d27c5060","description":"Flex Direct Order NO Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13858"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-NO","sku":"default:1","email":"","isTaxed":"Y","locale":"no_NO"}},' +
                '{"displayname":"Flex Direct Order Poland CC","testcasename":"d27c5060","description":"Flex Direct Order PL CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13849"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-PL","sku":"default:1","email":"","isTaxed":"Y","locale":"pl_PL"}},' +
                '{"displayname":"Flex Direct Order Poland Paypal","testcasename":"d27c5060","description":"Flex Direct Order PL Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13857"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-PL","sku":"default:1","email":"","isTaxed":"Y","locale":"pl_PL"}},' +
                '{"displayname":"Flex Direct Order Denmark CC","testcasename":"d27c5060","description":"Flex Direct Order DK CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13848"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-DK","sku":"default:1","email":"","isTaxed":"Y","locale":"da_DK"}},' +
                '{"displayname":"Flex Direct Order Denmark Paypal","testcasename":"d27c5060","description":"Flex Direct Order DK Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13856"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-DK","sku":"default:1","email":"","isTaxed":"Y","locale":"da_DK"}},' +
                '{"displayname":"Flex Direct Order Czech Republic CC","testcasename":"d27c5060","description":"Flex Direct Order CZ CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13847"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CZ","sku":"default:1","email":"","isTaxed":"Y","locale":"cs_CZ","address":"Autodesk@Hornomecholupska 873@Praha@102 00@03230700940@Czech republic"}},' +
                '{"displayname":"Flex Direct Order Czech Republic PAYPAL","testcasename":"d27c5060","description":"Flex Direct Order CZ PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13855"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-CZ","sku":"default:1","email":"","isTaxed":"Y","locale":"cs_CZ","address":"Autodesk@Hornomecholupska 873@Praha@102 00@03230700940@Czech republic"}},' +
                '{"displayname":"Flex Direct Order Netherlands CC","testcasename":"d27c5060","description":"Flex Direct Order NL CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13844"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
                '{"displayname":"Flex Direct Order Netherlands PAYPAL","testcasename":"d27c5060","description":"Flex Direct Order NL PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13852"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
                '{"displayname":"Flex Direct Order Switzerland CC","testcasename":"d27c5060","description":"Flex Direct Order CH CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13846"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CH","sku":"default:1","email":"","isTaxed":"Y","locale":"de_CH"}},' +
                '{"displayname":"Flex Direct Order Switzerland PAYPAL","testcasename":"d27c5060","description":"Flex Direct Order CH PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13854"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-CH","sku":"default:1","email":"","isTaxed":"Y","locale":"de_CH"}},' +
                '{"displayname":"Flex Direct Order Refund EMEA CC","testcasename":"a1c54974","description":"Refund Flex Direct Order EMEA CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13860"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
                '{"displayname":"Flex Direct Order Refund EMEA PAYPAL","testcasename":"a1c54974","description":"Refund Flex Direct Order EMEA PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13862"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
                '{"displayname":"Flex Direct Order Refund GBP CC","testcasename":"a1c54974","description":"Refund Flex Direct Order GBP CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13861"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB"}},' +
                '{"displayname":"Flex Direct Order Refund GBP PAYPAL","testcasename":"a1c54974","description":"Refund Flex Direct Order GBP PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13863"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB"}},' +
//                 '{"displayname":"Flex Direct Refund Old Order CHF","testcasename":"a1c54974","description":"Refund Flex Direct Old Order EMEA CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13867"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CH","sku":"default:1","email":"","isTaxed":"Y","locale":"de_CH"}},' +
//                 '{"displayname":"Flex Direct Refund Old Order NL","testcasename":"a1c54974","description":"Refund Flex Direct Old Order EMEA CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13866"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
//                 '{"displayname":"Flex Direct Refund Old Order NL","testcasename":"a1c54974","description":"Refund Flex Direct Old Order EMEA CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13866"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
//                 '{"displayname":"Flex Direct Refund Old Order NL PAYPAL","testcasename":"a1c54974","description":"Refund Flex Direct Old Order EMEA PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13865"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL"}},' +
//                 '{"displayname":"Flex Direct Refund Old Order UK","testcasename":"a1c54974","description":"Refund Flex Direct Old Order GBP CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13864"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB"}},' +
                '{"displayname":"Quote 2 Order UK(en_GB)","testcasename":"9d3de1c2","description":"Quote 2 Order UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13874"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"Quote 2 Order UK(en_GB) PAYPAL","testcasename":"9d3de1c2","description":"Quote 2 Order UK(en_GB) PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13876"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"Quote 2 Order NL(nl_NL)","testcasename":"9d3de1c2","description":"Quote 2 Order NL(nl_NL)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13873"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL","timezone":"Europe/Amsterdam"}},' +
                '{"displayname":"Quote 2 Order NL(nl_NL) PAYPAL","testcasename":"9d3de1c2","description":"Quote 2 Order NL(nl_NL) PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13875"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-EU","sku":"default:1","email":"","isTaxed":"Y","locale":"nl_NL","timezone":"Europe/Amsterdam"}},' +
                '{"displayname":"Refund Q2O UK(en_GB)","testcasename":"a2d62443","description":"Refund Q2O UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"quote-RefundOrder","testMethod":"validateQuoteRefundOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13872"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB) PAYPAL","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB) PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13871"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"PAYPAL","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Quote 2 Order Same Purchaser & Payer - Germany(de_DE)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Quote 2 Order Same Purchaser & Payer - Germany(de_DE) PAYPAL","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE) PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13870"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"PAYPAL","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}}' +
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

def triggerMOABTests(def serviceBuildHelper, String env) {
    echo 'Initiating Apollo MOAB Invoice Tests'
    script {
        println("Building Testing Hub API Input Map - moab")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/moab/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + params.ENVIRONMENT + ' ","executionname":"Apollo: R2.0.4 Reseller and LOC orders on ' + params.ENVIRONMENT + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"jiraTestCycleId":"29733","jiraPAT":"' + params.JIRAPAT + '","testcases":[' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash & CM - US","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices with Cash & CM- US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14311"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","applyCM":"Y","sku":"default:1","email":"","locale":"en_US"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash - CA","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices Cash - CA","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14312"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-CA","purchaserEmail":"Reseller_CA_DCLE_s1wWRX@letscheck.pw","csn":"5500971257","sku":"default:1","email":"","locale":"en_CA"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash - AU","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices Cash - AU","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14313"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-AUS","purchaserEmail":"Reseller_AU_DCLE_ppHDJ5@letscheck.pw","csn":"5500971071","sku":"default:1","email":"","locale":"en_AU"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash & CM - IT","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices CC- IT","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14314"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-IT","purchaserEmail":"Reseller_IT_DCLE_9zbCbX@letscheck.pw","csn":"5500971063","applyCM":"Y","sku":"default:1","email":"","locale":"it_IT"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash - JP","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices Cash - JP","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14320"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-JP","purchaserEmail":"Reseller_JP_DCLE_zLGQXO@letscheck.pw","csn":"5500971276","sku":"default:1","email":"","locale":"ja_JP"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash - UK","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices with Cash - UK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14319"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-UK","currency":"GBP","purchaserEmail":"Reseller_UK_DCLE_hqZ0GK@letscheck.pw","csn":"5500971259","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"MOAB - Reseller  Pay invoices with BACS - UK","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices with BACS - UK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14319"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","currency":"GBP","purchaserEmail":"Reseller_UK_DCLE_hqZ0GK@letscheck.pw","csn":"5500971259","sku":"default:1","email":"","locale":"en_GB"}},' +
                //'{"displayname":"MOAB - Reseller  Pay invoices with Cash - CH","testcasename":"faf86494","description":"MOAB - Reseller Pay invoices - CH","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14315"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-CH","purchaserEmail":"Reseller_CH_DCLE_JFYAJW@letscheck.pw","csn":"5500971064","sku":"default:1","email":"","locale":"fr_CH"}},' +
                //'{"displayname":"MOAB - Reseller  Pay invoices with Cash - CZ","testcasename":"faf86494","description":"MOAB - Reseller with Multi invoices Cash - CZ","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14322"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-CZ","purchaserEmail":"Reseller_CZ_DCLE_zx7e7t@letscheck.pw","csn":"5500971087","sku":"default:1","email":"","locale":"cs_CZ"}},' +
                //'{"displayname":"MOAB - Reseller  Pay invoices with Cash - PL","testcasename":"faf86494","description":"MOAB - Reseller with Multi invoices Cash - PL","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14323"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-PL","purchaserEmail":"Reseller_PL_DCLE_cuelF2@letscheck.pw","csn":"5500971272","sku":"default:1","email":"","locale":"pl_PL"}},' +
                //'{"displayname":"MOAB - Reseller  Pay invoices with Cash - SE","testcasename":"faf86494","description":"MOAB - Reseller with Multi invoices Cash - SE","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14324"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-SE","purchaserEmail":"Reseller_SE_DCLE_2qClrS@letscheck.pw","csn":"5500971066","sku":"default:1","email":"","locale":"sv_SE"}},' +
                //'{"displayname":"MOAB - Reseller  Pay invoices with Cash - NO","testcasename":"faf86494","description":"MOAB - Reseller with Multi invoices Cash - NO","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14325"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NO","purchaserEmail":"Reseller_NO_DCLE_rhIYbG@letscheck.pw","csn":"5500971267","sku":"default:1","email":"","locale":"no_NO"}},' +
                //'{"displayname":"MOAB - Reseller  Pay invoices with Cash - DK","testcasename":"faf86494","description":"MOAB - Reseller with Multi invoices Cash - DK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14326"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-DK","purchaserEmail":"Reseller_DK_DCLE_qIYham@letscheck.pw","csn":"5500971269","sku":"default:1","email":"","locale":"da_DK"}},' +
                '{"displayname":"MOAB - Reseller Pay Multi Currency invoices with Cash & CM - US(USD)","testcasename":"faf86494","description":"MOAB - Reseller Pay Multi Currency invoices with Cash & CM - US(USD)","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14328"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","currency":"USD","purchaserEmail":"Reseller_US_DCLE_BzZvya@letscheck.pw","csn":"5500971059","applyCM":"Y","sku":"default:1","email":"","locale":"en_US"}},' +
                '{"displayname":"MOAB - Reseller Pay Multi Currency invoices with CASH - US(CAD)","testcasename":"faf86494","description":"MOAB - Reseller Pay Multi Currency invoices with CASH - US(CAD)","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7262","jiraId":"APLR2PMO-14329"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","currency":"CAD","purchaserEmail":"Reseller_US_DCLE_BzZvya@letscheck.pw","csn":"5500971059","sku":"default:1","email":"","locale":"en_US"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - moab")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
        if (execution_id != null) {
            println('Testing Hub API called successfully - moab')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - moab')
        }
    }
    script {
        println("Building Testing Hub API Input Map - flex")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"LOC PayInvoice same Payer - US CC","testcasename":"9329504a","description":"LOC PayInvoice same Payer - US CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","timezone":"America/New_York"}},' +
                '{"displayname":"LOC PayInvoice same Payer - US Paypal","testcasename":"9329504a","description":"LOC PayInvoice same Payer - US Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","timezone":"America/New_York"}},' +
                '{"displayname":"LOC PayInvoice same Payer - CA","testcasename":"9329504a","description":"LOC PayInvoice same Payer - CA","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12566"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC PayInvoice same Payer - UK","testcasename":"9329504a","description":"LOC PayInvoice same Payer - UK","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC PayInvoice same Payer - EURO","testcasename":"9329504a","description":"LOC PayInvoice same Payer - EURO","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC PayInvoice same Payer - AUS","testcasename":"9329504a","description":"LOC PayInvoice same Payer - AUS","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","timezone":"Australia/Sydney"}},' +
                '{"displayname":"LOC PayInvoice diff Payer - US CC","testcasename":"9329504a","description":"LOC PayInvoice diff Payer - US CC","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","timezone":"America/New_York"}},' +
                '{"displayname":"LOC PayInvoice diff Payer - US Paypal","testcasename":"9329504a","description":"LOC PayInvoice diff Payer - US Paypal","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","timezone":"America/New_York"}},' +
                '{"displayname":"LOC PayInvoice diff Payer - CA","testcasename":"9329504a","description":"LOC PayInvoice diff Payer - CA","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12566"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC PayInvoice diff Payer - UK","testcasename":"9329504a","description":"LOC PayInvoice diff Payer - UK","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC PayInvoice diff Payer - EURO","testcasename":"9329504a","description":"LOC PayInvoice diff Payer - EURO","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC PayInvoice diff Payer - AUS","testcasename":"9329504a","description":"LOC PayInvoice diff Payer - AUS","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","timezone":"Australia/Sydney"}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - flex")
        execution_id = serviceBuildHelper.ambassadorService.callTestingHub(testingHubInputMap)
        if (execution_id != null) {
            println('Testing Hub API called successfully - flex')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - flex')
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
                '{"displayname":"GUAC - BiC Native Order UK","testcasename":"validateBicNativeOrder","description":"BiC Native Order - UK ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order DE  SEPA","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - SEPA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC Native Order DE  GIROPAY","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - GIROPAY","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - Add seats from GUAC","testcasename":"validateBicAddSeats","description":"Add seats from GUAC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - MOE order","testcasename":"validateBicNativeOrderMoe","description":"MOE order","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC refund order PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC refund order - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC refund order VISA","testcasename":"validateBicRefundOrder","description":"BiC refund order - VISA","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC order with existing user","testcasename":"validateBicReturningUser","description":"BiC order with existing user","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC renew order","testcasename":"validateRenewBicOrder","description":"BiC renew recurring order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","address":"Autodesk@1245 Alpine Ave@Boulder@80304@9916800100@United States@CO", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"GUAC - BiC refund order UK","testcasename":"validateBicRefundOrder","description":"BiC refund order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","locale":"en_GB","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}}' +
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
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"Account Portal - Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Account Portal - Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Account Portal - Change Payment","testcasename":"validateBICChangePaymentProfile","description":"Change Payment from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-changePayment","testMethod":"validateBICChangePaymentProfile","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"Account Portal - Switch Term","testcasename":"validateBicNativeOrderSwitchTerm","description":"Switch Term for BiC Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder-switch-term","testMethod":"validateBicNativeOrderSwitchTerm","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"Account Portal - Restart Subscription","testcasename":"validateRestartSubscription","description":"Restart a Canceled Subscription","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-restart-subscription","testMethod":"validateRestartSubscription","parameters":{"application":"ece","payment":"VISA","store":"STORE-NAMER"},"testdata":{}},' +
                '{"displayname":"Account Portal - Align Billing","testcasename":"validateAlignBilling","description":"Align 2 Subscriptions to same Renewal from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-align-billing","testMethod":"validateAlignBilling","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}}' +
//                '{"displayname": "Indirect Order in Portal", "testcasename": "validateBICIndirectSAPOrder", "description": "SAP Order in Portal", "testClass": "com.autodesk.ece.bic.testsuites.IndirectOrderCreation", "testGroup": "sap-bicindirect", "testMethod": "validateBICIndirectSAPOrder", "parameters": { "application": "ece" }, "testdata": { "sku": "057M1-WWN886-L563:1", "salesorg": "3000", "SAPConfigLocation": "C:\\\\TestingHub\\\\SAPConfig" }}' +
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
        println("Building Testing Hub API Input Map - flex")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/flex/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionid":"' + execution_id + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC order Flex","testcasename":"d27c5060","description":"BiC order new Flex","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC order Flex DE GIROPAY","testcasename":"d27c5060","description":"BiC Flex Direct Order - DE - GIROPAY","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"Quote 2 Order Multi line item Order","testcasename":"e803e4a4","description":"Quote 2 Order Multi line item Order","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"multiline-quoteorder","testMethod":"validateMultiLineItemQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","pullFromDataStore":"True","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","quantity1":"2000","quantity2":"4000","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/Los_Angeles"}},' +
                '{"displayname":"Quote 2 Order AUS New South Wales(en_AU)","testcasename":"9d3de1c2","description":"Quote 2 Order AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","pullFromDataStore":"True","store":"STORE-AUS","sku":"default:1","email":"","isTaxed":"Y","locale":"en_AU","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"AutodeskAU@114 Darlinghurst Rd@Darlinghurst@2010@397202088@Australia@NSW","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Quote 2 Order SUS and Quote Orders CA (en_CA)","testcasename":"c5558739","description":"Quote 2 Order SUS and Quote Orders CA (en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-sus-quote-orders","testMethod":"validateBicSUSAndQuoteOrders","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","isTaxed":"Y","locale":"en_CA","sapValidation":"' + params.INVOICE_VALIDATION + '","emailType":"biz","address":"AutodeskCA@2379 Kelly Cir SW@Edmonton@T6W 4G3@397202088@Canada@AB","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O Same Purchaser & Payer - Alabama(en_US)","testcasename":"9d3de1c2","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","pullFromDataStore":"True","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","sapValidation":"' + params.INVOICE_VALIDATION + '","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"TTR Q2O CA British Columbia(en_CA)","testcasename":"9d3de1c2","description":"Quote 2 Order CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7095","jiraId":"APLR2PMO-12727"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","newPaymentType":"CREDITCARD","pullFromDataStore":"True","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","submitTaxInfo":"true","locale":"en_CA","sapValidation":"False","address":"Autodesk@721 Government St@Victoria@V8W 1W5@9916800100@Canada@BC","timezone":"Canada/Pacific"}},' +
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
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com", "dcle.dep.metroid@autodesk.com"],"testcases":[' +
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
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"LOC Q2O CJT - Alabama(en_US)","testcasename":"9d3de1c2","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"LOC Q2O CJT - CA Ontario(en_CA)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12566"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","sapValidation":"False","address":"Autodesk@246 Lynden Road@Vineland@L0R 2E0@9055624155@Canada@ON","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O CJT - UK(en_GB)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Q2O CJT - Germany(de_DE)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Q2O CJT - AUS Northern Territory(en_AU)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - Alabama(en_US)","testcasename":"9329504a","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12671"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - CA Ontario(en_CA)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7094","jiraId":"APLR2PMO-12566"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","sapValidation":"False","address":"Autodesk@246 Lynden Road@Vineland@L0R 2E0@9055624155@Canada@ON","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - UK(en_GB)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13869"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - Germany(de_DE)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - AUS Northern Territory(en_AU)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece","jiraTestFolderId":"7206","jiraId":"APLR2PMO-13868"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}}' +
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
            testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionid":"' + execution_id + '","notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
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
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Financing Regression on ' + env + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com","pavan.venkatesh.malyala@autodesk.com","jeong.sohn@autodesk.com","anjani.singh@autodesk.com"],"testcases":[' +
                '{"displayname":"BiC Financing Order","testcasename":"validateBicNativeOrder","description":"BiC Financing Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":"", "sapValidation":"' + params.INVOICE_VALIDATION + '"}},' +
                '{"displayname":"BiC Financing Flex Order","testcasename":"34de7a6d","description":"BiC Financing Flex Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Canceled Order","testcasename":"validateBicNativeFinancingCanceledOrder","description":"BiC Financing Order Canceled","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-canceled","testMethod":"validateBicNativeFinancingCanceledOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Declined Order","testcasename":"validateBicNativeFinancingDeclinedOrder","description":"BiC Financing Order Declined","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-declined","testMethod":"validateBicNativeFinancingDeclinedOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Renew Order","testcasename":"783c495f","description":"BiC Financing Renew Order","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-renew-order","testMethod":"validateBicNativeFinancingRenewalOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}}' +
                '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - estore - Financing')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore - Financing')
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
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"Analytics Regression on ' + env + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com","abhijit.rajurkar@autodesk.com","adam.hill@autodesk.com"],"testcases":[' +
               '{"displayname":"BIC Ecommerce Tealium Analytics logs","testcasename":"5810b037","description":"BIC Ecommerce Tealium Analytics logs","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"tealium-network-logs","testMethod":"validateTealiumNetworkLogs","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"BIC Ecommerce Google Analytics logs","testcasename":"3fe26a1b","description":"BIC Ecommerce Google Analytics logs","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"google-network-logs","testMethod":"validateGoogleNetworkLogsAndTags","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"BIC Ecommerce Adobe Analytics logs","testcasename":"3a9c7241","description":"BIC Ecommerce Adobe Analytics logs","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"adobe-network-logs","testMethod":"validateAdobeNetworkLogsAndTags","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Cookies Before Consent","testcasename":"85f95d88","description":"GDPR Cookies Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-cookies-before-consent","testMethod":"validateGDPRCookiesBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Footer Banner Before Consent","testcasename":"6633b8bd","description":"GDPR Cookies Footer Banner Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-footer-banner-before-consent","testMethod":"validateGDPRFooterBannerBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Mandatory Tags Not Fired Before Consent","testcasename":"19aa6e3d","description":"GDPR Mandatory Tags Not Fired Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-mandatory-tags-not-fired-before-consent","testMethod":"validateGdprMandatoryTagsNotFiredBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Google Tags Before Consent","testcasename":"c8016bf0","description":"GDPR Google Tags Before Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-google-tags-before-consent","testMethod":"validateGDPRGoogleNetworkTagsBeforeConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Cookies After Consent","testcasename":"9e5efee8","description":"GDPR Cookies After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-cookies-after-consent","testMethod":"validateGDPRCookiesAfterConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Footer Banner After Consent","testcasename":"387c6e2c","description":"GDPR Cookies Footer Banner After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-footer-banner-after-consent","testMethod":"validateGDPRFooterBannerAfterConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Mandatory Tags Fired After Consent","testcasename":"88c7224d","description":"GDPR Mandatory Tags Fired After Consent","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-mandatory-tags-fired-after-consent","testMethod":"validateGdprMandatoryTagsFiredAfterConsent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Cookies On Next Page Load","testcasename":"ecf50dad","description":"GDPR Cookies On Next Page Load","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-cookies-after-next-page-load","testMethod":"validateGDPRCookiesOnNextPageLoad","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Footer Banner On Next Page Load","testcasename":"e0bc731f","description":"GDPR Cookies Footer Banner On Next Page Load","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-footer-banner-after-next-page-load","testMethod":"validateGDPRFooterBannerOnNextPageLoad","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}},' +
               '{"displayname":"GDPR Mandatory Tags Fired On Next Page Load","testcasename":"21ff36cf","description":"GDPR Mandatory Tags Fired On Next Page Load","testClass":"com.autodesk.ece.bic.testsuites.TealiumNetworkLogs","testGroup":"GDPR-mandatory-tags-fired-after-next-page-load","testMethod":"validateGdprMandatoryTagsFiredOnNextPageLoad","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":""}}'+
               '],"workstreamname":"dclecjt"}'
        println("Starting Testing Hub API Call - estore")
        if (serviceBuildHelper.ambassadorService.callTestingHubApi(testingHubInputMap)) {
            println('Testing Hub API called successfully - estore - Analytics')
        } else {
            currentBuild.result = 'FAILURE'
            println('Testing Hub API call failed - estore - Analytics')
        }
    }
}
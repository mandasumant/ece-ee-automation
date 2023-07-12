#!groovy

import com.autodesk.wpe.dsl.build.BuildInfo
import com.autodesk.wpe.dsl.build.ServicesBuildHelper
import groovy.json.JsonBuilder

@Library(['PSL@master', 'jenkins-shared-lib', 'jenkins-modules', 'wpe-shared-library@psl_2.0']) _

def common_sonar
def buildInfo = new BuildInfo(currentBuild: currentBuild, moduleName: "testinghub-autobot", stack: "test")
def serviceBuildHelper = new ServicesBuildHelper(this, 'svc_d_artifactory', buildInfo)

def generateTest(name, testcase, address, options = []) {
    def testData = [
            usertype : "new",
            password : "",
            emailType: "biz"
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
        booleanParam(name: 'APOLLO_R2_1_1', defaultValue: false, description: 'Run Apollo R2.1.1')
        booleanParam(name: 'APOLLO_R2_1_2', defaultValue: false, description: 'Run Apollo R2.1.2')
        booleanParam(name: 'ANALYTICS', defaultValue: false, description: 'Run ANALYTICS Regression')
        booleanParam(name: 'EDU', defaultValue: false, description: 'Run all EDU tests')
    }

    stages {
        stage('Prepare environment') {
            when {
                not {
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        expression {
                            params.CJT == true ||
                                    params.APOLLO_R2_1_1 == true ||
                                    params.APOLLO_R2_1_2 == true ||
                                    params.EDU == true ||
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
                                    params.APOLLO_R2_1_1 == true ||
                                    params.APOLLO_R2_1_2 == true ||
                                    params.EDU == true ||
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
                                    params.APOLLO_R2_1_1 == true ||
                                    params.APOLLO_R2_1_2 == true ||
                                    params.EDU == true ||
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
                anyOf {
                    triggeredBy 'TimerTrigger'
                    expression {
                        params.APOLLO_R2_1_2 == true
                    }
                }
            }
            steps {
                triggerApolloR2_1_2(serviceBuildHelper, 'INT')
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

def triggerApolloR2_1_1(def serviceBuildHelper, String env) {
    echo 'Initiating Apollo R2.1.1'
    script {
        println("Building Testing Hub API Input Map - eStore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":" ' + env + ' ","executionname":"Apollo: R2.1.1 Test Report on ' + env + '","notificationemail":["ece.dcle.platform.automation@autodesk.com","Joe.Mcqueeney@autodesk.com","tanner.hirakida@autodesk.com","sreeparvathy.jayalekshmi@autodesk.com","piyush.laddha@autodesk.com","Satish.Jupalli@autodesk.com","brad.collins@autodesk.com"],"testcases":[' +
                '{"displayname":"GUAC - BiC Native Order US 1 MONTH AUTOCAD","testcasename":"validateBicNativeOrder","description":"BiC Native Order US 1 MONTH AUTOCAD","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","locale":"en_US","sku":"default:1","productName":"autocad","email":""}},' +
                '{"displayname":"GUAC - BiC Native Order US 1 YEAR 3DS MAX","testcasename":"validateBicNativeOrder","description":"BiC Native Order US 1 YEAR 3DS MAX","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","locale":"en_US","sku":"default:1","productName":"3ds-Max","term":"1-YEAR","email":""}},' +
                '{"displayname":"GUAC - BiC Native Order AUS 1 YEAR AUTOCAD","testcasename":"validateBicNativeOrder","description":"BiC Native Order US 1 YEAR AUTOCAD","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","locale":"en_GB","sku":"default:1","productName":"autocad","term":"1-YEAR","email":""}},' +
                '{"displayname":"GUAC - BiC Native Order UK 1 YEAR MAYA","testcasename":"validateBicNativeOrder","description":"BiC Native Order UK 1 YEAR MAYA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","sku":"default:1","productName":"maya","term":"1-YEAR","email":""}},' +
                '{"displayname":"GUAC - BiC Native Order CA 3 YEAR AUTOCAD LT","testcasename":"validateBicNativeOrder","description":"BiC Native Order CA 3 YEAR AUTOCAD LT ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","locale":"en_CA","sku":"default:1","productName":"autocad-lt","term":"3-YEAR","email":""}},' +
                '{"displayname":"GUAC - BiC Native Order IT 3 YEAR AUTOCAD LT","testcasename":"validateBicNativeOrder","description":" BiC Native Order IT 3 YEAR AUTOCAD LT ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-IT","locale":"it_IT","sku":"default:1","productName":"autocad-lt","term":"3-YEAR","email":""}}' +
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
                '{"displayname":"Flex Direct Order - AU - CC","testcasename":"4ef43ece","description":"Flex Direct Order - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O FLEX - Single Line Item - AU - CC","testcasename":"8e83688b","description":"Q2O FLEX - Single Line Item - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-flex","testMethod":"validateQuoteOrderFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O FLEX - Single Line Item - AU - PAYPAL","testcasename":"6751d047","description":"Q2O FLEX - Single Line Item - AU - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-flex","testMethod":"validateQuoteOrderFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"PAYPAL","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O FLEX - Single Line Item - AU - LOC","testcasename":"8e83688b","description":"Q2O FLEX - Single Line Item - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-flex","testMethod":"validateQuoteOrderFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"LOC","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Annual - AU - CC","testcasename":"10bb01af","description":"Q2O SUS - Single Line Item - Annual - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual","testMethod":"validateQuoteOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","isTaxed":"N","taxId":"11223491505",timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Annual - AU - PAYPAL","testcasename":"8d2a2601","description":"Q2O SUS - Single Line Item - Annual - AU - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual","testMethod":"validateQuoteOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"PAYPAL","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Annual - AU - LOC","testcasename":"10bb01af","description":"Q2O SUS - Single Line Item - Annual - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual","testMethod":"validateQuoteOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Annual - AU - LOC - Diff Payer","testcasename":"10bb01af","description":"Q2O SUS - Single Line Item - Annual - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual","testMethod":"validateQuoteOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","createPayer":"true","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - MYAB - AU - CC","testcasename":"6308c0bb","description":"Q2O SUS - Single Line Item - MYAB - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-myab","testMethod":"validateQuoteOrderMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","isTaxed":"N","taxId":"11223491505","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - MYAB - AU - PAYPAL","testcasename":"974cf38d","description":"Q2O SUS - Single Line Item - MYAB - AU - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-myab","testMethod":"validateQuoteOrderMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"PAYPAL","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - MYAB - AU - LOC","testcasename":"6308c0bb","description":"Q2O SUS - Single Line Item - MYAB Offer AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-myab","testMethod":"validateQuoteOrderMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Premium - AU - CC","testcasename":"e3ecfeb4","description":"Q2O SUS - Single Line Item - Premium - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-premium","testMethod":"validateQuoteOrderPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Premium - AU - LOC","testcasename":"e3ecfeb4","description":"Q2O SUS - Single Line Item - Premium - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-premium","testMethod":"validateQuoteOrderPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Premium - AU - ZIP","testcasename":"e3ecfeb4","description":"Q2O SUS - Single Line Item - Premium - AU - ZIP","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-premium","testMethod":"validateQuoteOrderPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"ZIP","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","sapValidation":"' + params.INVOICE_VALIDATION + '","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Returning User - Same Payment - AU - CC","testcasename":"67a7eeb8","description":"Q2O SUS - Single Line Item - Returning User - Same Payment - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-returning-user","testMethod":"validateQuoteOrderReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Returning User - Same Payment - AU - LOC","testcasename":"67a7eeb8","description":"Q2O SUS - Single Line Item - Returning User - Same Payment - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-returning-user","testMethod":"validateQuoteOrderReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","newPaymentType":"LOC","isSamePayer":"true","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Returning User - New Payment - AU - CC and LOC","testcasename":"67a7eeb8","description":"Q2O SUS - Single Line Item - Returning User - Same Payment - AU - CC and LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-returning-user","testMethod":"validateQuoteOrderReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","newPaymentType":"LOC","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Single Line Item - Returning User - New Payment - AU - LOC and CC","testcasename":"67a7eeb8","description":"Q2O SUS - Single Line Item - Returning User - Same Payment - AU - LOC and CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-returning-user","testMethod":"validateQuoteOrderReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","newPaymentType":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual and MYAB - AU - CC","testcasename":"a2afad9d","description":"Q2O SUS - Multi Line Item - Annual and MYAB - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-myab","testMethod":"validateQuoteOrderAnnualMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual and MYAB - AU - PAYPAL","testcasename":"cfd1bc73","description":"Q2O SUS - Multi Line Item - Annual and MYAB - AU - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-myab","testMethod":"validateQuoteOrderAnnualMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"PAYPAL","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual and MYAB - AU - LOC","testcasename":"a2afad9d","description":"Q2O SUS - Multi Line Item - Annual and MYAB - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-myab","testMethod":"validateQuoteOrderAnnualMYAB","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual and Flex - AU - CC","testcasename":"c26311ff","description":"Q2O SUS - Multi Line Item - Annual and Flex - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-flex","testMethod":"validateQuoteOrderAnnualFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual and Flex - AU - LOC","testcasename":"c26311ff","description":"Q2O SUS - Multi Line Item - Annual and Flex - AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-flex","testMethod":"validateQuoteOrderAnnualFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"LOC","emailType":"biz","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual, Flex, MYAB and Premium Offer AU - CC","testcasename":"57223646","description":"Q2O SUS - Multi Line Item - Annual, Flex, MYAB and Premium Offer AU - CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-flex-myab-premium","testMethod":"validateQuoteOrderAnnualMYABFLEXPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Q2O SUS - Multi Line Item - Annual, Flex, MYAB and Premium Offer AU - LOC","testcasename":"57223646","description":"Q2O SUS - Multi Line Item - Annual, Flex, MYAB and Premium Offer AU - LOC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-annual-flex-myab-premium","testMethod":"validateQuoteOrderAnnualMYABFLEXPremium","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","quantity1":"500","payment":"LOC","emailType":"biz","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU - CC","testcasename":"6eb74ce4","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-direct-order","testMethod":"createDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU - Multiline - CC","testcasename":"0b9bd2c4","description":"SUS O2P Direct CC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-multiline-order","testMethod":"createMultilineDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU - Returning User - PAYPAL","testcasename":"655f5063","description":"SUS Direct AU - Returning User - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"returning-user","testMethod":"createReturningUserDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"PAYPAL","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Refund Q2O SUS Single Line Item - Annual CC","testcasename":"27b5eaa9","description":"Refund Q2O Annual CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-refund-annual","testMethod":"validateQuoteRefundOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Refund Direct SUS Multi Line Item - Annual CC","testcasename":"d0a9f506","description":"Refund Direct Annual CC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"refund-multiline-order","testMethod":"createRefundMultilineDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Direct SUS Multi Line Item - Flex CC","testcasename":"ce1d5160","description":"Direct SUS Multi Line Item - Flex CC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-multiline-order-sus-flex","testMethod":"createMultilineDirectOrderSUSFlex","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Refund Direct SUS Multi Line Item - Annual LOC","testcasename":"3a51f9cb","description":"Refund Direct Annual LOC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"refund-multiline-order-loc","testMethod":"createRefundLOCMultilineDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","agentCSN":"5112991506","agentEmail":"partneruser_da_int_5112991506@letscheck.email"}},' +
                '{"displayname":"Terminate O2P SUS CC","testcasename":"d3eb65f3","description":"Terminate O2P SUS CC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"direct-order-subscription-status","testMethod":"validateDirectOrderSubscriptionStatus","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","subscriptionStatus":"TERMINATED","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Suspend O2P SUS CC","testcasename":"d3eb65f3","description":"Suspend O2P SUS CC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"direct-order-subscription-status","testMethod":"validateDirectOrderSubscriptionStatus","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","subscriptionStatus":"SUSPENDED","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Expire O2P SUS CC","testcasename":"d3eb65f3","description":"Expire O2P SUS CC","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"direct-order-subscription-status","testMethod":"validateDirectOrderSubscriptionStatus","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","subscriptionStatus":"EXPIRED","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Renew Q2O SUS CC","testcasename":"6b2be18d","description":"Renew Q2O SUS CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"renew-quote-order-annual","testMethod":"validateRenewQuoteOrderAnnual","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"False","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Terminate Q2O SUS CC","testcasename":"77e82e12","description":"Terminate Q2O SUS CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-subscription-status","testMethod":"validateQuoteOrderSubscriptionStates","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","subscriptionStatus":"TERMINATED","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"False","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Suspend Q2O SUS CC","testcasename":"77e82e12","description":"Suspend Q2O SUS CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-subscription-status","testMethod":"validateQuoteOrderSubscriptionStates","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","subscriptionStatus":"SUSPENDED","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"False","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Expire Q2O SUS CC","testcasename":"77e82e12","description":"Expire Q2O SUS CC","testClass":"com.autodesk.ece.bic.testsuites.QuoteOrder","testGroup":"quote-order-subscription-status","testMethod":"validateQuoteOrderSubscriptionStates","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","subscriptionStatus":"EXPIRED","store":"STORE-AUS","locale":"en_AU","pullFromDataStore":"False","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU Renew - CC","testcasename":"942382a8","description":"SUS O2P Direct renew","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"direct-order-renew","testMethod":"directOrderRenew","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU Renew - Paypal","testcasename":"942382a8","description":"SUS O2P Direct renew","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"direct-order-renew","testMethod":"directOrderRenew","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"PAYPAL","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU/Christmas - CC","testcasename":"6eb74ce4","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-direct-order","testMethod":"createDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@33 Phosphate Hill Rd@Christmas Island@6798@397202088@Christmas Island","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU/Norfolk - CC","testcasename":"6eb74ce4","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-direct-order","testMethod":"createDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@53 Hemus Road@Burnt Pine@2899@397202088@Norfolk Island","timezone":"Australia/Sydney"}},' +
                '{"displayname":"SUS Direct AU/Coco - CC","testcasename":"6eb74ce4","description":"SUS O2P Direct","testClass":"com.autodesk.ece.bic.testsuites.DirectOrder","testGroup":"create-direct-order","testMethod":"createDirectOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"CREDITCARD","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@RR6H FJ6@West Island@6799@397202088@Cocos (Keeling) Islands","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Pay Invoice for Q20 Single Flex - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Single Flex - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"SINGLE_FLEX"}},' +
                '{"displayname":"Pay Invoice for Q20 Single Annual - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Single Annual - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"SINGLE_ANNUAL"}},' +
                '{"displayname":"Pay Invoice for Q20 Single Annual - AU - PAYPAL","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Single Annual - AU - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"PAYPAL","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"SINGLE_ANNUAL"}},' +
                '{"displayname":"Pay Invoice for Q20 Single Annual - AU - Bank Transfer","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Single Annual - AU - Bank Transfer","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"WIRE_TRANSFER","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"SINGLE_ANNUAL"}},' +
                '{"displayname":"Pay Invoice for Q20 Single MYAB - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Single MYAB - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"SINGLE_MYAB"}},' +
                '{"displayname":"Pay Invoice for Q20 Single Premium - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Single Premium - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"SINGLE_PREMIUM"}},' +
                '{"displayname":"Pay Invoice for Q20 Multi Annual MYAB - AU - PAYPAL","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Multi Annual MYAB - AU - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"PAYPAL","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"MULTI_ANNUAL_MYAB"}},' +
                '{"displayname":"Pay Invoice for Q20 Multi Annual MYAB - AU - Bank Transfer","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Multi Annual MYAB - AU - Bank Transfer","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"WIRE_TRANSFER","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"MULTI_ANNUAL_MYAB"}},' +
                '{"displayname":"Pay Invoice for Q20 Multi Annual MYAB - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Multi Annual MYAB - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"MULTI_ANNUAL_MYAB"}},' +
                '{"displayname":"Pay Invoice for Q20 Multi Annual Flex - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Multi Annual Flex - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"MULTI_ANNUAL_FLEX"}},' +
                '{"displayname":"Pay Invoice for Q20 Multi Annual Flex MYAB Premium - AU - CC","testcasename":"0558f1b7","description":"Pay Invoice for Q20 Multi Annual Flex MYAB Premium - AU - CC","testClass":"com.autodesk.ece.bic.testsuites.PayInvoice","testGroup":"pay-invoice","testMethod":"validatePayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","email":"","password":"","sku":"default:1","payment":"LOC","newPaymentType":"CREDITCARD","emailType":"biz","store":"STORE-AUS","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney","scenario":"MULTI_ANNUAL_FLEX_MYAB_PREMIUM"}}' +
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

def triggerCJT(def serviceBuildHelper, String env) {
    echo 'Initiating Customer Lifecycle Tests - Regression'
    script {
        println("Building Testing Hub API Input Map - estore")
        def testingHubInputMap = [:]
        def authInputMap = [clientCredentialsId: 'testing-hub-clientid', patTokenId: 'testing-hub-pattoken']
        testingHubInputMap.authToken = serviceBuildHelper.ambassadorService.getForgeAuthToken(authInputMap)
        testingHubInputMap.testingHubApiEndpoint = 'https://api.testinghub.autodesk.com/hosting/v1/project/estore/testcase'
        testingHubInputMap.testingHubApiPayload = '{"env":"' + env + '","executionname":"CLT Regression on ' + env + '", "notificationemail":["ece.dcle.platform.automation@autodesk.com"],"testcases":[' +
                '{"displayname":"GUAC - BiC Native Multi line item Order","testcasename":"validateMultiLineItemBicNativeOrder","description":"BiC Native Multi line item Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-multiline-bicorder","testMethod":"validateMultiLineItemBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@Alabama"}},' +
                '{"displayname":"GUAC - BiC Native Order US","testcasename":"validateBicNativeOrder","description":"BiC Native Order - US ","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - BiC Native Order UK BACS","testcasename":"validateBicNativeOrder","description":"BiC Native Order - UK BACS","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","sku":"default:1","email":"","locale":"en_GB"}},' +
                '{"displayname":"GUAC - BiC Native Order DE  SEPA","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - SEPA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"}},' +
                '{"displayname":"GUAC - BiC Native Order DE  GIROPAY","testcasename":"validateBicNativeOrder","description":"BiC Native Order - DE - GIROPAY","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"}},' +
                '{"displayname":"GUAC - Add seats from GUAC","testcasename":"validateBicAddSeats","description":"Add seats from GUAC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-guac-addseats","testMethod":"validateBicAddSeats","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - MOE order US CC","testcasename":"validateBicNativeOrderMoe","description":"MOE order US","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - MOE order JP Konbini","testcasename":"validateBicNativeOrderMoe","description":"MOE order JP Konbini","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-nativeorder-moe","testMethod":"validateBicNativeOrderMoe","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"KONBINI","store":"STORE-JP","locale":"ja_JP","store_type":"FAMILY_MART","sku":"default:1","email":"","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111","timezone":"Japan/Tokyo"}},' +
                '{"displayname":"BiC Financing Order","testcasename":"validateBicNativeOrder","description":"BiC Financing Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-nativeorder","testMethod":"validateBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Refund Flex Order","testcasename":"ffae8105","description":"BiC Financing Refund Flex Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC Financing Renew Order","testcasename":"783c495f","description":"BiC Financing Renew Order","testClass":"com.autodesk.ece.bic.testsuites.BICFinancingOrder","testGroup":"bic-financing-renew-order","testMethod":"validateBicNativeFinancingRenewalOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"FINANCING","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - BiC refund order PAYPAL","testcasename":"validateBicRefundOrder","description":"BiC refund order - PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"PAYPAL","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - BiC refund order VISA","testcasename":"validateBicRefundOrder","description":"BiC refund order - VISA","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - BiC order with existing user","testcasename":"validateBicReturningUser","description":"BiC order with existing user","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-returningUser","testMethod":"validateBicReturningUser","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - BiC renew order","testcasename":"validateRenewBicOrder","description":"BiC renew recurring order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"renew-bic-order","testMethod":"validateRenewBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","address":"Autodesk@1245 Alpine Ave@Boulder@80304@9916800100@United States@CO"}},' +
                '{"displayname":"GUAC - BiC PromoCode order","testcasename":"promocodeBicOrder","description":"BiC Order with PromoCoder","testClass":"com.autodesk.ece.bic.testsuites.BICOrderPromoCode","testGroup":"bic-promocode-order","testMethod":"promocodeBicOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"GUAC - BiC refund order UK","testcasename":"validateBicRefundOrder","description":"BiC refund order - UK","testClass":"com.autodesk.ece.bic.testsuites.BICRefundOrder","testGroup":"bic-RefundOrder","testMethod":"validateBicRefundOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-UK","locale":"en_GB","sku":"default:1","email":""}},' +
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
                '{"displayname":"Account Portal - Add Seats","testcasename":"validateBicAddSeatNativeOrder","description":"Add Seats from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-addseat-native","testMethod":"validateBicAddSeatNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"Account Portal - Reduce Seats","testcasename":"validateBicReduceSeats","description":"Reduce Seats from Portal","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-reduceseats-native","testMethod":"validateBicReduceSeats","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"MASTERCARD","store":"STORE-NAMER","sku":"default:1","email":""}},' +
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
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with BACS - UK","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with BACS- UK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","store":"STORE-UK","currency":"GBP","purchaserEmail":"Reseller_UK_DCLE_2ZgMkv@letscheck.pw","csn":"5500971062","sku":"default:1","email":"","locale":"en_GB"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with SEPA - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-IT","purchaserEmail":"Reseller_IT_DCLE_9zbCbX@letscheck.pw","csn":"5500971063","sku":"default:1","email":"","locale":"it_IT"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with GIROPAY - Germany","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with GIROPAY- Germany","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","purchaserEmail":"Reseller_DE_PWS-Performance_kufcb@letscheck.pw","csn":"5500989129","sku":"default:1","email":"","locale":"de_DE"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - CA","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- CA","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-CA","purchaserEmail":"Reseller_CA_DCLE_s1wWRX@letscheck.pw","csn":"5500971257","sku":"default:1","email":"","locale":"en_CA"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - AU","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- AU","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-AUS","purchaserEmail":"Reseller_AU_DCLE_ppHDJ5@letscheck.pw","csn":"5500971071","sku":"default:1","email":"","locale":"en_AU"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-IT","purchaserEmail":"Reseller_IT_DCLE_9zbCbX@letscheck.pw","csn":"5500971063","sku":"default:1","email":"","locale":"it_IT"}}' +
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
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"thpu7OwL5kk97h@letscheck.pw","csn":"5501308785","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"thpu7OwL5kk97h@letscheck.pw","csn":"5501308785","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with BACS - UK","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with BACS- UK","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","currency":"GBP","store":"STORE-UK","purchaserEmail":"thpumurxsSnXxe@letscheck.pw","csn":"5501308790","sku":"default:1","email":"","locale":"en_GB"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with SEPA - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"SEPA","store":"STORE-IT","purchaserEmail":"thpuWib85OHCcS@letscheck.pw","csn":"5501276951","sku":"default:1","email":"","locale":"it_IT"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with GIROPAY - Germany","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with GIROPAY- Germany","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","purchaserEmail":"thpuRQSOrg6Sqg@letscheck.pw","csn":"5501308975","sku":"default:1","email":"","locale":"de_DE"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-NAMER","purchaserEmail":"thpu7OwL5kk97h@letscheck.pw","csn":"0070176510","sku":"default:1","email":"","locale":"en_US"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - CA","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- CA","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-CA","purchaserEmail":"thpuXa8oSCwst0@letscheck.pw","csn":"5501308786","sku":"default:1","email":"","locale":"en_CA"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - AU","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Wire Transfer- AU","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-AUS","purchaserEmail":"thpuRKbjdVbZwt@letscheck.pw","csn":"5501308787","sku":"default:1","email":"","locale":"en_AU"}},' +
                    '{"displayname":"MOAB - Reseller  Pay invoices with Wire Transfer - Italy","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with SEPA- Italy","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"WIRE_TRANSFER","store":"STORE-IT","purchaserEmail":"thpuxzsJzkISws@letscheck.pw","csn":"5501308820","sku":"default:1","email":"","locale":"it_IT"}}' +
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
                '{"displayname":"BiC order Flex US VISA","testcasename":"d27c5060","description":"BiC order new Flex US VISA","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":""}},' +
                '{"displayname":"BiC order Flex Refund CA PAYPAL","testcasename":"a1c54974","description":"BiC order Flex Refund CA PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","store":"STORE-CA","locale":"en_CA","sku":"default:1","address":"Autodesk CA@10 Rue Saint@Montréal@H2Y 1L2@9916800100@Canada@QC","email":""}},' +
                '{"displayname":"BiC order Flex DE GIROPAY","testcasename":"d27c5060","description":"BiC Flex Direct Order - DE - GIROPAY","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"GIROPAY","store":"STORE-DE","sku":"default:1","email":"","locale":"de_DE"}},' +
                '{"displayname":"Quote 2 Order Multi line item Order US PAYPAL","testcasename":"e803e4a4","description":"Quote 2 Order Multi line item Order US PAYPAL","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"multiline-quoteorder","testMethod":"validateMultiLineItemQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"PAYPAL","pullFromDataStore":"True","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","quantity1":"2000","quantity2":"4000","isMultiLineItem":"True","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/Los_Angeles"}},' +
                '{"displayname":"Quote 2 Order UK BACS","testcasename":"9d3de1c2","description":"Quote 2 Order UK BACS","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"BACS","pullFromDataStore":"True","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"Quote 2 Order AUS CREDIT CARD","testcasename":"9d3de1c2","description":"Quote 2 Order AUS CREDIT CARD","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","pullFromDataStore":"True","store":"STORE-AUS","sku":"default:1","email":"","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@114 Darlinghurst Rd@Darlinghurst@2010@397202088@Australia@NSW","timezone":"Australia/Sydney"}},' +
                '{"displayname":"Quote 2 Order SUS and CA CREDIT CARD","testcasename":"c5558739","description":"Quote 2 Order SUS and Quote Orders CA CREDIT CARD","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-sus-quote-orders","testMethod":"validateBicSUSAndQuoteOrders","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","isTaxed":"Y","locale":"en_CA","emailType":"biz","address":"AutodeskCA@2379 Kelly Cir SW@Edmonton@T6W 4G3@397202088@Canada@AB","timezone":"Canada/Pacific"}},' +
                '{"displayname":"BiC order Flex Direct Refund JP Credit Card","testcasename":"a1c54974","description":"Flex Direct Order Refund Japan CC","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexdirect-new-refund","testMethod":"validateFlexOrderNewCartRefund","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111"}},' +
                '{"displayname":"BiC order Flex - Japan Konbini","testcasename":"d27c5060","description":"BiC order Flex - Japan Konbini","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"KONBINI","store":"STORE-JP","sku":"default:1","email":"","locale":"ja_JP","store_type":"FAMILY_MART","address":"Autodesk@2-8-1 Nishi Shinjuku@Shinjuku Ku@160-0022@03-5321-1111"}},' +
                '{"displayname":"LOC Q2O Same Purchaser & Payer - US VISA","testcasename":"9d3de1c2","description":"LOC Q2O Same Purchaser & Payer - US VISA","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","pullFromDataStore":"True","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"TTR Q2O CA CREDIT CARD","testcasename":"9d3de1c2","description":"TTR Q2O CA CREDIT CARD","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CREDITCARD","newPaymentType":"CREDITCARD","pullFromDataStore":"True","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","submitTaxInfo":"true","locale":"en_CA","address":"Autodesk@721 Government St@Victoria@V8W 1W5@9916800100@Canada@BC","timezone":"Canada/Pacific"}},' +
                '{"displayname":"MOE O2P Order USA - Agent - New","testcasename":"e2ea9875","description":"MOE O2P Order USA - Agent - New user","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmAgent-moe","testMethod":"validateMoeOdmOpportunityFlowAgent","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","locale":"en_US"}},' +
                '{"displayname":"MOE O2P Order CA - Customer - Existing","testcasename":"97993340","description":"MOE O2P Order CA - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-basicFlowOdmCustomer-moe","testMethod":"validateMoeOdmOpportunityFlowCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","isTaxed":"Y","locale":"en_CA","address":"CompanyNameCA@4204 Av Northcliffe@Montreal@H4A 3L3@9916800100@Canada@QC"}},' +
                '{"displayname":"MOE DTC O2P Order UK - Customer - Existing","testcasename":"2363224d","description":"MOE DTC O2P Order UK - Customer - Existing","testClass":"com.autodesk.ece.bic.testsuites.MOEOrderFlows","testGroup":"bic-returningUserOdmDtc-moe","testMethod":"validateMoeOdmDtcFlowReturningCustomer","parameters":{"application":"ece"},"testdata":{"usertype":"existing","password":"","payment":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","isTaxed":"Y","locale":"en_GB"}}' +
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
                '{"displayname":"LOC Q2O CJT - Alabama(en_US)","testcasename":"9d3de1c2","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"LOC Q2O CJT - CA Ontario(en_CA)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","address":"Autodesk@246 Lynden Road@Vineland@L0R 2E0@9055624155@Canada@ON","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O CJT - UK(en_GB)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Q2O CJT - Germany(de_DE)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Q2O CJT - AUS Northern Territory(en_AU)","testcasename":"9d3de1c2","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-quoteorder","testMethod":"validateBicQuoteOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - Alabama(en_US)","testcasename":"9329504a","description":"Quote 2 Order US Alabama(en_US)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@AL","timezone":"America/New_York"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - CA Ontario(en_CA)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - CA(en_CA)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-CA","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_CA","address":"Autodesk@246 Lynden Road@Vineland@L0R 2E0@9055624155@Canada@ON","timezone":"Canada/Pacific"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - UK(en_GB)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - UK(en_GB)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-UK","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_GB","timezone":"Europe/London"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - Germany(de_DE)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - DE(de_DE)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-DE","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"de_DE","address":"Autodesk@Viktualienmarkt 3@Munchen@80331@65043235263@Deutschland","timezone":"Europe/Berlin"}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - AUS Northern Territory(en_AU)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}},' +
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
                '{"displayname":"GUAC - BiC Native Multi line item Order","testcasename":"validateMultiLineItemBicNativeOrder","description":"BiC Native Multi line item Order","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-multiline-bicorder","testMethod":"validateMultiLineItemBicNativeOrder","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","email":"","isTaxed":"Y","applyAnalytics":"True","address":"Autodesk@2300 Woodcrest Pl@Birmingham@35209@9916800100@United States@Alabama"}}' +
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
                '{"displayname":"BiC order Flex","testcasename":"d27c5060","description":"BiC order new Flex","testClass":"com.autodesk.ece.bic.testsuites.BICOrderCreation","testGroup":"bic-flexorder-new","testMethod":"validateFlexOrderNewCart","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"VISA","store":"STORE-NAMER","sku":"default:1","applyAnalytics":"True","email":""}},' +
                '{"displayname":"LOC Q2O CJT Pay Invoice - AUS Northern Territory(en_AU)","testcasename":"9329504a","description":"LOC Quote 2 Order Same Purchaser & Payer - AUS(en_AU)","testClass":"com.autodesk.ece.bic.testsuites.BICQuoteOrder","testGroup":"bic-loc-payinvoice","testMethod":"validateLocPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"LOC","newPaymentType":"CREDITCARD","store":"STORE-AUS","sku":"default:1","email":"","emailType":"biz","isTaxed":"Y","locale":"en_AU","applyAnalytics":"True","address":"AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT","timezone":"Australia/Sydney"}}' +
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
                '{"displayname":"MOAB - Reseller  Pay invoices with Cash - US","testcasename":"26497eda","description":"MOAB - Reseller Pay invoices with Cash  - US","testClass":"com.autodesk.ece.bic.testsuites.MOABOrder","testGroup":"moab-payinvoice","testMethod":"validateMOABPayInvoice","parameters":{"application":"ece"},"testdata":{"usertype":"new","password":"","payment":"CASH","store":"STORE-NAMER","purchaserEmail":"Reseller_US_DCLE_i4lJmK@letscheck.pw","csn":"5500971254","sku":"default:1","applyAnalytics":"True","email":"","locale":"en_US"}}' +
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
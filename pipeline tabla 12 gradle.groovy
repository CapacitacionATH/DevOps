[12:25 p. m.] Eduin David Rodriguez Russi
//Variables Sonarqube
def qualityGateId="AX-NvzKQBOweQL0CyIBz"
def credentialSonarId="JenkinsSonarToken"
def sonarOptions="-Dsonar.projectVersion=${Version} -Dsonar.coverage.exclusions=src/test/** -Dsonar.coverage.exclusions=src/test/** -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml"
//def sonarScannerTool="Sonar_Scanner_4.6"


pipeline {
    agent any

    tools {
        gradle 'Gradle V7'
    }
    stages{
        stage('checkout'){
            steps {
                cleanWs()
                git branch: 'main', url: 'https://github.com/CapacitacionATH/pruebaGradle.git'
            }
        }
        stage('Build'){
            steps{
                sh 'gradle clean build'
            }
        }
        stage('sonarqube analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    //sh 'gradle sonarqube -Dsonar.host.url=http:/172.18.0.2:9000 -Dsonar.verbose=true'
                    //sh  'gradle sonarqube -Dsonar.projectKey=PruebaQuintillesima. -Dsonar.host.url=http://localhost:9000 -Dsonar.login=84a0e6405f03a29450275f91961b2b0e17651b67'
                    sh 'gradle sonarqube'
                }
            }
        }
        stage('Quality gates'){
            steps{
                script{
                    def qualityGateSonar = "no calculado"
                    def fallo = false
                    def validaciones=0
                        while(qualityGateSonar=="no calculado" && validaciones<30){
                            try{
                                validaciones=validaciones+1
                                println("Quality Gate= "+qualityGateSonar+" Validacion #"+validaciones)
                                timeout(time: 30, unit: 'SECONDS') {
                                    qualityGateSonar = waitForQualityGate()
                                        if (qualityGateSonar.status != 'OK') {
                                            fallo=true
                                        }
                                    }
                                }
                            catch(Exception e){
                                qualityGateSonar = "NO CALCULATED"
                            }              
                        }
                }
            }
        }
        stage('rename'){
            steps{
                dir('app/build/libs'){
                    sh "ls"
                    sh "mv app.jar Tabla12.jar"
                }
            }
        }

        stage('upload Nexus'){
            steps{
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: '172.18.0.2:8081/repository/Tabla12',
                    groupId: 'com.gradle.Tabla12',
                    version: '1.0'+"_${currentBuild.number}",
                    repository: 'Tabla12',
                    credentialsId: 'nexuscredenciales',
                    artifacts: [
                        [
                            artifactId:'GradleTabla12',
                            classifier: '',
                            file: 'app/build/libs/Tabla12.jar',
                            type: 'jar'
                       ]
                    ]
                )
                cleanWs()
            }
        }
    }
}
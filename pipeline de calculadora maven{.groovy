pipeline {
    agent any

    tools {
        // Install the Maven version configured as "M3" and add it to the path.
        maven "MavenV3"
    }

    stages {
        stage('Checkout') {
            steps {
                // Get some code from a GitHub repository
                git branch: 'main', url: 'https://github.com/CapacitacionATH/pruebaMaven.git'
            }
        }

        stage('Build'){
            steps{
                // Run Maven on a Unix agent.
                sh "mvn clean install"

                // To run Maven on a Windows agent, use
                // bat "mvn -Dmaven.test.failure.ignore=true clean package"
            }
        }

        stage('sonarqube analysis'){
            environment{
               scannerHome = tool 'sonar-scanner';// mismo nombre que colocamos en la configuracion de "Global tool configuration".
            }
            steps{
                withSonarQubeEnv('sonarqube'){ // nombre que se definio en "configure system"
                //sh "${scannerHome}/bin/mvn -X clean deploy sonar:sonar" 
                sh "mvn sonar:sonar"
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
        stage('upload Nexus'){
            steps{
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: '172.18.0.2:8081/repository/maven-calculadora',
                    groupId: 'com.mycompany',
                    version: '1.0'+"_${currentBuild.number}",
                    repository: 'maven-calculadora',
                    credentialsId: 'nexuscredenciales',
                    artifacts: [
                        [
                            artifactId:'pruebamaven',
                            classifier: '',
                            file: 'target/pruebamaven-1.0.jar',
                            type: 'jar'
                       ]
                    ]
                )
            }
        }
        stage('climb a desk'){
            steps{
                dir('target'){
                    sh "ls"
                    sh "cp pruebamaven-1.0.jar C:\\Users\\chicu\\Documents\\ATH\\Trabajo\\mavencalc"
                }
            }
        }
    }
}

node {

	def err = null
	def buildVersion = "UNKNOWN"
	currentBuild.result = "SUCCESS"
	def buildStarted = false
	def timestamp = Calendar.getInstance().getTime().format('YYYYMMddHHmm',TimeZone.getTimeZone('GMT'))
	def buildDisplayName = "${env.JOB_NAME}".replaceAll('%2F','/');	

	try {

		def scriptState
		def branchname = "${env.BRANCH_NAME}"
		if (branchname == null || branchname == "null") {
		    branchname = "test-build"
		}
		branchname = "${branchname}".replaceAll("/","-")
		def versionState = "${branchname}".replaceAll("\\.","_")
				if (branchname == "master") {
			scriptState = ""
			branchname = "release"
		} else if (branchname == "develop") {
			scriptState = "beta"
			branchname = "beta"
		} else if (branchname == "release") {
			scriptState = "rc"
			branchname = "rc"
		}
		
		def branchLabel = "${branchname}".replaceAll("\\.","_")
		scriptState = "-${branchLabel}"
		
		echo "Building: ${buildDisplayName}"
		milestone()
				
		lock(resource: "etcd-provider-${branchname}", inversePrecedence: true) {
			milestone()
			buildStarted = true;
			
			def workspaceFolder = "${env.WS_FOLDER}/etcd-provider/${branchname}"
			echo "Workspace Folder: ${workspaceFolder}"		
			def anthome = tool 'Ant_1.10'
			def gradlehome = tool 'Gradle_6.3'
			
			def mvnhome
			if (isUnix()) {
				mvnhome = tool 'Maven_3.6'
			} else {
				// Due to a bug, use Maven 3.3 for Windows builds
				mvnhome = tool 'Maven_3.3'
			}
			
			def workingFolder
			def jfxHome = "${env.JAVA_FX_11}"
			def eclipsehome = "${env.ECLIPSE_4_7}"
			def skipJarSign = "${env.SKIP_JAR_SIGN}"
			
			def propFolder
			def commonProps
			def version
			def dlversion
			def mavenVersion
			def dlmavenVersion
			def dlSharedVersion
			def versionProps
			def dlp2VersionFolder
			def buildVersionShort
			def buildVersionMid
			def buildVersionGroup
			def dlbuildVersionGroup
			
			def cyberSharedVersion
			def repoChampionClientURL
			
			def champBranch
			def champGroup
			def repoCyberClientURL
			
			def pubServer = "${env.PUB_SERVER}"
			def webServerURL = "${env.WEB_SERVER_URL}"
		
			ws("${workspaceFolder}") {
			
				stage('Checkout') {
					checkout scm
				}
					
				stage('Setup Build') {

					env.JAVA_HOME = tool 'OPEN_JDK_11'
					env.WORKSPACE = pwd()
					workingFolder = pwd()

					properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '90', artifactNumToKeepStr: '2', daysToKeepStr: '', numToKeepStr: '']]])

				}
				
				withCredentials([string(credentialsId: 'KEY_PASS', variable: 'KEY'), string(credentialsId: 'STORE_PASS', variable: 'STORE'), string(credentialsId: 'PFX_PASS', variable: 'PFX')]) {
					stage('Build') {
						if (isUnix()) {
							sh "cd ${workingFolder}\n${mvnhome}/bin/mvn clean package deploy -f ./pom.xml -Dtycho.localArtifacts=ignore -Dmaven.test.skip=true"
						} else {
							bat "cd ${workingFolder}\n${mvnhome}\\bin\\mvn clean package deploy -f .\\pom.xml -Dtycho.localArtifacts=ignore -Dmaven.test.skip=true"
						}
					}

					stage('Record Results') {
						recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), taskScanner(highTags: 'FIXME', ignoreCase: true, includePattern: '*/src/**/*.java', lowTags: 'XXX', normalTags: 'TODO')]
//						currentBuild.description="Version: ${buildVersion}"
						junit allowEmptyResults: true, healthScaleFactor: 0.0, testResults: '**/generated/test-reports/**/*.xml'
						jacoco classPattern: '**/target/classes, **/bin', exclusionPattern: '**/*Test*.class', sourcePattern: '*plugins/**/src,*/src'
						step([$class: 'TeamUpdateWorkItemPostBuildAction'])
					}

					stage('Archive Artifacts') {
//						step([$class: 'ArtifactArchiver', artifacts: '**/generated/distributions/executable/*.jar', excludes: null])
					}

				}
			
			}
			milestone()
		}
	} catch (Exception ex) {
		echo "Build State: ${currentBuild.result}"
		
		if (buildStarted == true) {
			err = ex
			currentBuild.result = "FAILURE"
		}
		
	} finally {
		
		if (currentBuild.result == "FAILURE" && buildStarted == true) {
			emailext body: '''$BUILD_STATUS - $PROJECT_NAME - Build # $BUILD_NUMBER:

Check console output at $BUILD_URL to view the results.

Error Message: err.getMessage()''', recipientProviders: [brokenBuildSuspects(), upstreamDevelopers(), culprits(), requestor(), developers()], subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!'	
		
		}

		if (err != null) {
		    throw err;
		}
	}

}
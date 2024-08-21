package org.devops 

//git clone

def buildInfo(String[] args){
    wrap([$class: 'BuildUser']) {
        def deploylog="${BUILD_USER} use pipeline  '${JOB_NAME}(${BUILD_NUMBER})' "
        println deploylog
        buildName "#${BUILD_NUMBER}  ${BUILD_USER}"
        BUILD_USER = "${env.BUILD_USER_ID}"
        }
}

def checkout(String[] args){
    checkout scmGit(branches: [[name: "${branch}"]], extensions: [submodule(parentCredentials: true, recursiveSubmodules: true, reference: ''),[$class: 'RelativeTargetDirectory', relativeTargetDir: '${JOB_NAME}']], userRemoteConfigs: [[credentialsId: 'gitlab', url: "${repository}"]])
}

def checkout_no_submodule(String[] args){
    checkout scmGit(branches: [[name: '${branch}']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${JOB_NAME}']], userRemoteConfigs: [[credentialsId: 'gitlab', url: "${repository}"]])
}
def checkout_stor(String[] args){
checkout scmGit(branches: [[name: '${branch}']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${JOB_NAME}'], submodule(recursiveSubmodules: true, reference: '')], userRemoteConfigs: [[credentialsId: 'gitlab', url: "${repository}"]])
}

def checkout_client_common(String[] args){
    checkout scmGit(branches: [[name: "${client_common_branch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'hydro-client-common']] , userRemoteConfigs: [[credentialsId: 'gitlab', url: "${client_common_repository}"]])
}

def checkout_storage_rs(String[] args){
checkout scmGit(branches: [[name: "${storage_branch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${JOB_NAME}/qs-storage-rs']], userRemoteConfigs: [[credentialsId: 'gitlab', url: "${qs_storage_repository}"]])
}

def make_tgz(String[] args){
    dir("${WORKSPACE}/${JOB_NAME}") {
        sh """
            make release tgz
        """
    }
}



def npm_make(String[] args){
    dir("${WORKSPACE}/${JOB_NAME}/") {
        sh """
            npm install --legacy-peer-deps 
        """
    }
}


def hydro_ftp_make(String[] args){
    dir("${WORKSPACE}/${JOB_NAME}") {
        sh """
            source ~/.bashrc ~/.bash_profile /etc/profile
            make release tgz
        """
    }
}

def push_tgz(String[] args){
    PROJECT = sh(returnStdout: true, script: "cat ./${JOB_NAME}/Makefile.environment  | grep PROGRAM | awk -F '=' '{print \$2}'").trim()
    PRODUCT_VERSION = sh(returnStdout: true, script: "cat ./${JOB_NAME}/Makefile.environment  | grep PRODUCT_VERSION | awk -F '=' '{print \$2}'").trim()
    BUILD_DIR = sh(returnStdout: true, script: "cat ./${JOB_NAME}/Makefile.environment  | grep BUILD_DIR | awk -F '=' '{print \$2}'").trim()
    GIT_COMMIT = sh(returnStdout: true, script: "cat ./${JOB_NAME}/Makefile.environment  | grep GIT_COMMIT | awk -F '=' '{print \$2}'").trim()
  //  tar_name = sh(returnStdout: true, script: "${PROJECT}-${PRODUCT_VERSION}-${GIT_COMMIT}.tar.gz").trim()
    def destination = env.destination
    if ( destination == 'ectest' ) {
        port = 6024
    }
    def change_commit_id = env.change_commit_id
    if ( 'yes' == 'yes') {
        CHANGE_COMMIT = sh(returnStdout: true, script: "cat ./${JOB_NAME}/Makefile.environment  | grep GIT_COMMIT | awk -F '=' '{print \$2\"_${BUILD_ID}\"}'").trim()
        sh """
        cp ./${JOB_NAME}/${BUILD_DIR}/${PROJECT}-${PRODUCT_VERSION}-${GIT_COMMIT}.tar.gz ./${JOB_NAME}/${BUILD_DIR}/${PROJECT}-${PRODUCT_VERSION}-${CHANGE_COMMIT}.tar.gz
        scp -P ${port} ./${JOB_NAME}/${BUILD_DIR}/${PROJECT}-${PRODUCT_VERSION}-${CHANGE_COMMIT}.tar.gz root@${destination}:/qingstor/upgrade/${qingstor_version}/
    """
    } else {
        CHANGE_COMMIT = sh(returnStdout: true, script: "echo ${GIT_COMMIT}").trim()
        sh """
           echo  ${port} ./${JOB_NAME}/${BUILD_DIR}/${PROJECT}-${PRODUCT_VERSION}-${CHANGE_COMMIT}
           scp -P ${port} ./${JOB_NAME}/${BUILD_DIR}/${PROJECT}-${PRODUCT_VERSION}-${CHANGE_COMMIT}.tar.gz root@${destination}:/qingstor/upgrade/${qingstor_version}/
        """
    }
}

def check_version(String[] args){
    Previous_version = sh(returnStdout: true, script: "echo ' '").trim()
    //version_id = sh(returnStdout: true, script: "echo \"${check_version}\"").trim()
    sh "echo ${Previous_version}"
}

def upgrade_stor(String[] args){
    chekc_file = sh(returnStdout: true, script: "ssh -p ${port} root@${destination} 'if [ -e /qingstor/upgrade/${qingstor_version}/${PROJECT}-${PRODUCT_VERSION}-${CHANGE_COMMIT}.tar.gz ];then echo 0;else exit 1;fi'").trim()

                        sh """
                        echo ${chekc_file}
                        ssh -p ${port} root@${destination} '/root/qs-installer/qs_installer upgrade --${PROJECT} -e skip_repair=${skip_repair}'
                        """
}

def upgrade(String[] args){
    chekc_file = sh(returnStdout: true, script: "ssh -p ${port} root@${destination} 'if [ -e /qingstor/upgrade/${qingstor_version}/${PROJECT}-${PRODUCT_VERSION}-${CHANGE_COMMIT}.tar.gz ];then echo 0;else exit 1;fi'").trim()
                        sh """
                        echo ${chekc_file}
                        ssh -p ${port} root@${destination} '/root/qs-installer/qs_installer upgrade --${PROJECT}'
                        """
}


def success(String[] args){
                wrap([$class: 'BuildUser'])
                {
                    sh """
                    curl 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=<key_id>' \
   -H 'Content-Type: application/json' \
   -d '
	{
		"msgtype": "markdown",
		"markdown": {
			"content": "### 今日事今日毕 \n
>项目:<font color=\\"comment\\">${JOB_NAME}</font>
>用户:<font color=\\"comment\\"><@${BUILD_USER_ID}></font>
>分支:<font color=\\"comment\\">${branch}</font>
>上个版本:<font color=\\"comment\\">${Previous_version}</font>
>当前版本:<font color=\\"comment\\">${GIT_COMMIT}</font>
>环境:<font color=\\"comment\\">${destination}</font>
>状态:<font color=\\"info\\">SUCCESS</font>
>备注:<font color=\\"info\\">${note}</font>
[日志详情戳我](${BUILD_URL}console)"
		}
	}'
	            """
                }
}





def failure(String[] args){
                wrap([$class: 'BuildUser'])
                {
                    sh """
                    curl 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=<key_id>' \
   -H 'Content-Type: application/json' \
   -d '
	{
		"msgtype": "markdown",
		"markdown": {
			"content": "### 今日事今日毕 \n
>项目:<font color=\\"comment\\">${JOB_NAME}</font>
>用户:<font color=\\"comment\\"><@${BUILD_USER_ID}></font>
>分支:<font color=\\"comment\\">${branch}</font>
>上个版本:<font color=\\"comment\\">${Previous_version}</font>
>当前版本:<font color=\\"comment\\">${GIT_COMMIT}</font>
>环境:<font color=\\"comment\\">${destination}</font>
>状态:<font color=\\"warning\\">FAILURE</font>
>备注:<font color=\\"info\\">${note}</font>
[日志详情戳我](${BUILD_URL}console)"
		}
	}'
	            """
                }
}


def aborted(String[] args){
                wrap([$class: 'BuildUser'])
                {
                    sh """
                    curl 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=<key_id>' \
   -H 'Content-Type: application/json' \
   -d '
	{
		"msgtype": "markdown",
		"markdown": {
			"content": "### 今日事今日毕 \n
>项目:<font color=\\"comment\\">${JOB_NAME}</font>
>用户:<font color=\\"comment\\"><@${BUILD_USER_ID}></font>
>分支:<font color=\\"comment\\">${branch}</font>
>上个版本:<font color=\\"comment\\">${Previous_version}</font>
>当前版本:<font color=\\"comment\\">${GIT_COMMIT}</font>
>环境:<font color=\\"comment\\">${destination}</font>
>状态:<font color=\\"warning\\">ABORTED</font>
>备注:<font color=\\"info\\">${note}</font>
[日志详情戳我](${BUILD_URL}console)"
		}
	}'
	            """
                }
}

package org.devops 

//saltstack
def SaltDeploy(hosts,func){
    sh " echo \"${hosts}\" ${func} "
}

//git clone

def GitClone(Branches,RepositoryURL){
    git branch: '${Branches}', credentialsId: 'cb4b29b4-190f-4e3e-8fb9-aa990ca2ff16', url: "${RepositoryURL}"
}


def buildInfo(BUILD_USER,JOB_NAME,BUILD_NUMBER,info){
                        def deploylog="${BUILD_USER} use pipeline  '${JOB_NAME}(${BUILD_NUMBER})' "
                        println deploylog
                        buildName "#${BUILD_NUMBER}  ${BUILD_USER}"
                        //修改Description"
                        buildDescription "${info}" 
}

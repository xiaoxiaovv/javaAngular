package com.istar.mediabroken.service.account

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.entity.account.Organization
import com.istar.mediabroken.entity.app.Agent
import com.istar.mediabroken.repo.PrimaryKeyRepo
import com.istar.mediabroken.repo.account.OrganizationRepo
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by zxj on   2018/1/26
 */
@Service
@Slf4j
class OrganizationService {

    @Autowired
    OrganizationRepo organizationRepo
    @Autowired
    AccountService accountSrv
    @Autowired
    PrimaryKeyRepo primaryKeyRepo

    def getOrgInfoByOrgIdAndAgentId(String orgId, String agentId){
        Organization org = organizationRepo.getOrgInfoByOrgIdAndAgentId(orgId, agentId)
        return org
    }

    /**
     * 移动云对接 开通机构
     * @return
     */
    def createOrg4Ecloud(def data){

/* data 结构
{
    "applyno":"CIDC-O-DDF12ADB96874484BF2FA8DD8F06111B",
    "ecordercode":"121243544",
    "opttype":0,
    "trial":0,
    "bossorderid":12312434,
    "custid":12312312,
    "custcode":"21436575564",
    "custtype":2,
    "registersource":0,
    "custname":"中国移动",
    "userid":1001,
    "username":"用户名",
    "useralias":"姓名",
    "mobile":"13800138000",
    "email":"13800138000@139.com",
    "productcode":"2121212",
    "begintime":1411718510123,
    "endtime":1411718510123,
    "productparas":[
        {"key":"sad",
            "value":"111"},
        {"key":"qweqw",
            "value":"222"}
    ],
    "services":[
        {"opttype":0,
            "code":"111",
            "begintime":1411718510123,
            "endtime":1411718510123,
            "serviceparas":[
                {"key":"sad",
                    "value":"111"},
                {"key":"qweqw",
                    "value":"222"}
            ]
        }
    ]
}
*/
        String orgId = Organization.getEcloudOrgId(data.custid as int)
        Organization org = new Organization([
                id          : orgId,
                orgName     : data.custname as String,
                agentId     : Agent.AGENT_BJJ_ID,
                appId       : '',
                secret      : '',
                canPushNews : false,
                maxUserCount: 50,
                createTime  : new Date(),
                ecloudOrgInfo   : data
        ])

        Organization organization = organizationRepo.getOrgInfoByOrgIdAndAgentId(orgId, Agent.AGENT_BJJ_ID)
        if(!organization) {
            log.info(['ecloud', 'SaaS0001', '新创建机构', JSONObject.toJSONString(data)].join(':::') as String)
            //创建机构
            organizationRepo.save(org)
            //创建机构管理员 账号订购时，开通管理员用户，做管理员用户的默认授权
            def orgUserData = [
                    "custid" : data.custid,
                    "users" : [
                            [
                                    "userid" : data.userid,
                                    "endtime" : data.endtime,
                                    "useralias" : data.useralias,
                                    "mobile" : data.mobile,
                                    "email" : data.email
                            ]
                    ]
            ]
            accountSrv.createOrgUser4Ecloud(orgUserData)
        } else {
            organization.updateTime = new Date()
            organization.ecloudOrgInfo = data
            log.info(['ecloud', 'SaaS0001', '机构信息变更', JSONObject.toJSONString(data)].join(':::') as String)
            organizationRepo.save(organization)
        }
    }

}

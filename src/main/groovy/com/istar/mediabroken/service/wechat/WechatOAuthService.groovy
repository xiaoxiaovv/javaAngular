package com.istar.mediabroken.service.wechat

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.entity.app.Agent
import com.istar.mediabroken.repo.WechatAccessTokenRepo
import com.istar.mediabroken.repo.WechatAuthorizationInfoRepo
import com.istar.mediabroken.repo.WechatPreAuthCodeRepo
import com.istar.mediabroken.repo.WechatVerifyTicketRepo
import com.istar.mediabroken.service.ShareChannelService
import com.istar.mediabroken.service.shareChannel.ChannelOAuthService
import com.istar.mediabroken.utils.HttpClientUtil
import com.istar.mediabroken.utils.UrlUtils
import com.istar.mediabroken.utils.XmlUtil
import com.qq.weixin.mp.aes.WXBizMsgCrypt
import groovy.util.logging.Slf4j
import org.apache.http.protocol.HTTP
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.servlet.http.HttpServletRequest

/**
 * Author : YCSnail
 * Date   : 2017-05-18
 * Email  : liyancai1986@163.com
 */
@Service
@Slf4j
class WechatOAuthService extends ChannelOAuthService<WechatOAuthConfig> {

    @Value('${wechat.open.app.id}')
    public String appId

    @Value('${wechat.open.app.secret}')
    public String appSecret

    @Value('${wechat.open.oauth.callback.url}')
    public String oauthCallbackUrl

    @Value('${wechat.open.component.encode.aes.key}')
    public String encodingAesKey

    @Value('${wechat.open.component.token}')
    public String componentToken

    @Autowired
    private WechatVerifyTicketRepo verifyTicketRepo
    @Autowired
    private WechatAccessTokenRepo accessTokenRepo
    @Autowired
    private WechatPreAuthCodeRepo preAuthCodeRepo
    @Autowired
    private WechatAuthorizationInfoRepo authorizationInfoRepo
    @Autowired
    private ShareChannelService shareChannelSrv

    String getOAuthRequestUrl(HttpServletRequest request){

        config = getOAuthConfig(request)

        String url = 'https://mp.weixin.qq.com/cgi-bin/componentloginpage'

        def params = [
                component_appid : config.appId,
                pre_auth_code   : this.getPreAuthCode(request),
                redirect_uri    : UrlUtils.wrapNotifyUrl(request, oauthCallbackUrl)
        ]

        return UrlUtils.wrapGetUrl(url, params)
    }

    void wechatNotify(HttpServletRequest request, String nonce, String timestamp, String msgSignature, String xml) {

        config = getOAuthConfig(request)

        WXBizMsgCrypt pc = new WXBizMsgCrypt(config.componentToken as String, config.componentAesKey as String, config.appId as String)
        String newXml = pc.DecryptMsg(msgSignature, timestamp, nonce, xml);
        log.info('wechat:::?????????xml:::'.concat(newXml))

        Map<String, String> xmlMap = XmlUtil.toXml(newXml)
        if (xmlMap.get('AppId') == config.appId) {
            switch (xmlMap.get('InfoType')) {
                case 'component_verify_ticket':
//                    <xml>
//                        <AppId><![CDATA[wx33a29886d374e0e5]]></AppId>
//                        <CreateTime>1499075185</CreateTime>
//                        <InfoType><![CDATA[component_verify_ticket]]></InfoType>
//                        <ComponentVerifyTicket><![CDATA[ticket@@@OWR_6RX96n3rbRRhcmFK4Qy5lfOKDFCHoNOrp5c27z6OUOVSBzgrPP6wsMMKLq5KO0Ys114Dm_MGNjJ4mGfarA]]></ComponentVerifyTicket>
//                    </xml>
                    this.setVerifyTicket(request, xmlMap)
                    break
                case 'unauthorized':
//                    <xml>
//                        <AppId><![CDATA[wx33a29886d374e0e5]]></AppId>
//                        <CreateTime>1499075237</CreateTime>
//                        <InfoType><![CDATA[unauthorized]]></InfoType>
//                        <AuthorizerAppid><![CDATA[wxd517bf9f1fecf729]]></AuthorizerAppid>
//                    </xml>
                    shareChannelSrv.delWechatShareChannel(xmlMap.get('AuthorizerAppid'))
                    break
            }
        }
    }

    /**
     * ?????????????????????????????????component_verify_ticket?????????????????????10??????????????????
     */
    void setVerifyTicket(HttpServletRequest request, Map<String, String> xmlMap) {

        config = getOAuthConfig(request)

//        AppId	???????????????appid
//        CreateTime	?????????
//        InfoType	component_verify_ticket
//        ComponentVerifyTicket	Ticket??????

        if(xmlMap.get('AppId') == config.appId && xmlMap.get('InfoType') == 'component_verify_ticket'){
            verifyTicketRepo.add(
                    config.appId as String,
                    xmlMap.get('InfoType'),
                    xmlMap.get('CreateTime'),
                    xmlMap.get('ComponentVerifyTicket')
            )
        }
    }

    /**
     * ?????????????????????????????????component_access_token
     * @return
     */
    String getComponentAccessToken(HttpServletRequest request) {

        config = getOAuthConfig(request)

        //????????????????????????????????????????????????????????????????????????
        def accessTokenObj = accessTokenRepo.get(config.appId as String)

        if(accessTokenObj && accessTokenIsValid(accessTokenObj)) {
            log.info(['wechat', '????????????accessToken', accessTokenObj.accessToken].join(':::') as String)
            return accessTokenObj.accessToken
        } else {
            def verifyTicket = verifyTicketRepo.getNewest(config.appId as String)
            if(!verifyTicket) {
                return ''
            } else {
                String url = 'https://api.weixin.qq.com/cgi-bin/component/api_component_token'
                def params = [
                        component_appid         : config.appId,
                        component_appsecret     : config.appSecret,
                        component_verify_ticket : verifyTicket.ticket as String
                ]
                try {
                    def res = HttpClientUtil.doPost(url, JSONObject.toJSONString(params), HTTP.UTF_8)
                    def result = JSONObject.parseObject(res)
                    log.info(['wechat', '??????accessToken??????', res].join(':::') as String)

                    if(result.errcode){
                        log.error(['wechat', '??????accessToken????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                        return ''
                    }
//                    {
//                        "component_access_token"    : "RQQJgGxudjzOPPUlylh5WxK8UYIThHutwrGsjNHTCl4W4uIQfa9DClf7NLf-JVVq62zESvL99K0obth6voKlKNGD9M-oQFYeWVEuKoDVizr_JTLWs6PjEJxmEobqKFStOJDhAAAZUO",
//                        "expires_in"                : 7200
//                    }
                    //?????????
                    accessTokenRepo.update(config.appId as String, result.component_access_token as String, result.expires_in as long)
                    //????????????
                    return result.component_access_token as String
                }catch (Exception e) {
                    e.printStackTrace()
                    return ''
                }
            }
        }
    }

    /**
     * ??????????????????????????????pre_auth_code
     * @return
     */
    String getPreAuthCode(HttpServletRequest request) {

        config = getOAuthConfig(request)

        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????

        String accessToken = this.getComponentAccessToken(request)
        if(!accessToken) return ''

        String url = 'https://api.weixin.qq.com/cgi-bin/component/api_create_preauthcode?component_access_token=' + accessToken
        def params = [
                component_appid         : config.appId
        ]
        try {
            def res = HttpClientUtil.doPost(url, JSONObject.toJSONString(params), HTTP.UTF_8)
            def result = JSONObject.parseObject(res)
            log.info(['wechat', '??????preAuthCode??????', res].join(':::') as String)

            if(result.errcode){
                log.error(['wechat', '??????preAuthCode????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return ''
            }
            //????????????
            //            {
            //                "pre_auth_code":"Cx_Dk6qiBE0Dmx4EmlT3oRfArPvwSQ-oa3NL_fwHM7VI08r52wazoZX2Rhpz1dEw",
            //                "expires_in":600
            //            }
            return result.pre_auth_code as String
        }catch (Exception e) {
            e.printStackTrace()
            return ''
        }
    }

    /**
     * ???????????????????????????????????????????????????
     * @return
     * {
     "authorization_info":{
            "authorizer_appid"          :"wxf8b4f85f3a794e77",
            "authorizer_access_token"   :"QXjUqNqfYVH0yBE1iI_7vuN_9gQbpjfK7hYwJ3P7xOa88a89-Aga5x1NMYJyB8G2yKt1KCl0nPC3W9GJzw0Zzq_dBxc8pxIGUNi_bFes0qM",
            "expires_in"                :7200,
            "authorizer_refresh_token"  :"dTo-YCXPL4llX-u1W1pPpnp8Hgm4wpJtlR6iV0doKdY",
            "func_info":[
                 {
                     "funcscope_category":{
                         "id":1
                     }
                 },
                 {
                     "funcscope_category":{
                         "id":2
                     }
                 },
                 {
                     "funcscope_category":{
                         "id":3
                     }
                 }
             ]
        }
     }
     */
    def getAuthorizationInfo(HttpServletRequest request, String authCode) {

        config = getOAuthConfig(request)

        String accessToken = this.getComponentAccessToken(request)
        if(!accessToken) return []

        String url = 'https://api.weixin.qq.com/cgi-bin/component/api_query_auth?component_access_token=' + accessToken
        def params = [
                component_appid         : config.appId,
                authorization_code      : authCode
        ]
        try {
            def res = HttpClientUtil.doPost(url, JSONObject.toJSONString(params), HTTP.UTF_8)
            def result = JSONObject.parseObject(res)
            log.info(['wechat', '??????authorizationInfo??????', res].join(':::') as String)

            if(result.errcode){
                log.error(['wechat', '??????authorizationInfo????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return ''
            }
            //?????????
            authorizationInfoRepo.update(config.appId as String, result.authorization_info)
            //????????????
            return result.authorization_info
        }catch (Exception e) {
            e.printStackTrace()
            return ''
        }
    }

    /**
     * ????????????????????????????????????
     * @return {
         "nick_name"    : "??????SDK Demo Special",
         "head_img"     : "http://wx.qlogo.cn/mmopen/GPy",
         "service_type_info"    : {
             "id":2
         },
         "verify_type_info"     : {
            "id":0
         },
         "user_name"    : "gh_eb5e3a772040",
         "principal_name" : "?????????????????????????????????",
         "business_info"    : {
             "open_store"   :0,
             "open_scan"    :0,
             "open_pay"     :0,
             "open_card"    :0,
             "open_shake"   :0
         },
         "qrcode_url"   :"URL",
         "alias"        :"paytest01"
     }
     */
    def getWechatAccountInfo(HttpServletRequest request, String authorizerAppId) {

        config = getOAuthConfig(request)

        String accessToken = this.getComponentAccessToken(request)
        if(!accessToken) return []

        String url = 'https://api.weixin.qq.com/cgi-bin/component/api_get_authorizer_info?component_access_token=' + accessToken
        def params = [
                component_appid         : config.appId,
                authorizer_appid        : authorizerAppId
        ]
        try {
            def res = HttpClientUtil.doPost(url, JSONObject.toJSONString(params), HTTP.UTF_8)
            def result = JSONObject.parseObject(res)
            log.info(['wechat', '??????wechatAccountInfo??????', res].join(':::') as String)

            if(result.errcode){
                log.error(['wechat', '??????wechatAccountInfo????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return ''
            }
            //????????????
            return result.authorizer_info
        }catch (Exception e) {
            e.printStackTrace()
            return ''
        }
    }


    boolean accessTokenIsValid(def accessTokenObj) {
        if(!accessTokenObj){
            return false
        }
        //?????????????????????????????????10???????????????????????????
        return  (new Date().time - accessTokenObj.updateTime.time) < (accessTokenObj.expiresIn - 600) * 1000
    }

    boolean preAuthCodeIsValid (def preAuthCodeObj) {
        if(!preAuthCodeObj){
            return false
        }
        //?????????????????????????????????1???????????????????????????
        return  (new Date().time - preAuthCodeObj.updateTime.time) < (preAuthCodeObj.expiresIn - 60) * 1000
    }

    boolean authorizationAccessTokenIsValid (def authorizationInfo) {
        if(!authorizationInfo){
            return false
        }
        //?????????????????????????????????10???????????????????????????
        return (new Date().time - authorizationInfo.updateTime.time) < (authorizationInfo.expiresIn - 600) * 1000
    }

    /**
     * ??????????????????????????????
     * @param authorizerAppId
     * @return
     */
    String getFuncInfo (HttpServletRequest request, String authorizerAppId) {

        config = getOAuthConfig(request)

        def authorizationInfo = authorizationInfoRepo.get(config.appId as String, authorizerAppId)
        return authorizationInfo ? authorizationInfo.funcInfo : []
    }

    /**
     * ???????????????????????????accessToken
     * @return
     */
    String getAuthorizationAccessToken (HttpServletRequest request, String authorizerAppId) {

        config = getOAuthConfig(request)

        def authorizationInfo = authorizationInfoRepo.get(config.appId as String, authorizerAppId)

        if(authorizationInfo && authorizationAccessTokenIsValid(authorizationInfo)) {
            return authorizationInfo.authorizerAccessToken
        }else {
            String accessToken = this.getComponentAccessToken(request)
            if(!accessToken) return ''

            String url = 'https://api.weixin.qq.com/cgi-bin/component/api_authorizer_token?component_access_token=' + accessToken
            def params = [
                    component_appid         : config.appId,
                    authorizer_appid        : authorizerAppId,
                    authorizer_refresh_token: authorizationInfo.authorizerRefreshToken as String
            ]
            try {
                def res = HttpClientUtil.doPost(url, JSONObject.toJSONString(params), HTTP.UTF_8)
                def result = JSONObject.parseObject(res)
                log.info(['wechat', '??????AuthorizationAccessToken??????', res].join(':::') as String)

                if(result.errcode){
                    log.error(['wechat', '??????AuthorizationAccessToken????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                    return ''
                }
//                {
//                    "authorizer_access_token": "aaUl5s6kAByLwgV0BhXNuIFFUqfrR8vTATsoSHukcIGqJgrc4KmMJ-JlKoC_-NKCLBvuU1cWPv4vDcLN8Z0pn5I45mpATruU0b51hzeT1f8",
//                    "expires_in": 7200,
//                    "authorizer_refresh_token": "BstnRqgTJBXb9N2aJq6L5hzfJwP406tpfahQeLNxX0w"
//                }
                //?????????
                result.authorizer_appid = authorizerAppId
                authorizationInfoRepo.updateToken(config.appId as String, result)
                //????????????
                return result.authorizer_access_token as String
            }catch (Exception e) {
                e.printStackTrace()
                return ''
            }
        }
    }

    @Override
    WechatOAuthConfig getOAuthConfig(HttpServletRequest request) {

        Agent agent = agentService.getAgent(request)

        config = configMap.get(agent.agentKey)

        if(!config) {
            def obj = getShareChannelOAuthConfig(agent)

            config = new WechatOAuthConfig([
                    appId       : obj.wechat.appKey,
                    appSecret   : obj.wechat.appSecret,
                    componentAesKey : obj.wechat.componentAesKey,
                    componentToken  : obj.wechat.componentToken
            ])
            configMap.put(agent.agentKey, config)
        }
        return config
    }

}

enum WechatAuthorityEnum {

    mass_send_and_notify(7, '?????????????????????'),
    material(11, '??????????????????')

    private int index
    private String value

    int getIndex() {
        return index
    }

    String getValue() {
        return value
    }

    WechatAuthorityEnum(int index, String value) {
        this.index = index
        this.value = value
    }

//    1-15
//    ??????????????????
//    ??????????????????
//    ??????????????????
//    ??????????????????
//    ??????????????????
//    ?????????????????????
//    ?????????????????????
//    ??????????????????
//    ?????????????????????
//    ?????????WIFI??????
//    ??????????????????
//    ?????????????????????
//    ??????????????????
//    ??????????????????
//    ?????????????????????
}

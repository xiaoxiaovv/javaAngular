package com.istar.mediabroken.service

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.api.ShareResult
import com.istar.mediabroken.entity.app.Agent
import com.istar.mediabroken.entity.compile.Material
import com.istar.mediabroken.entity.compile.ShareChannelResult
import com.istar.mediabroken.repo.ShareChannelRepo
import com.istar.mediabroken.repo.compile.MaterialRepo
import com.istar.mediabroken.service.app.AgentService
import com.istar.mediabroken.service.compile.MaterialService
import com.istar.mediabroken.service.qqom.QqomOAuthService
import com.istar.mediabroken.service.qqom.QqomShareService
import com.istar.mediabroken.service.toutiao.ToutiaoOAuthService
import com.istar.mediabroken.service.toutiao.ToutiaoShareService
import com.istar.mediabroken.service.wechat.WechatOAuthService
import com.istar.mediabroken.service.wechat.WechatShareService
import com.istar.mediabroken.service.weibo.WeiboOAuthService
import com.istar.mediabroken.service.weibo.WeiboShareService
import com.istar.mediabroken.utils.StringUtils
import com.istar.mediabroken.utils.UrlUtils
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sun.misc.BASE64Decoder

import javax.servlet.http.HttpServletRequest
import static com.istar.mediabroken.api.ApiResult.apiResult
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Author : YCSnail
 * Date   : 2017-03-30
 * Email  : liyancai1986@163.com
 */
@Service
@Slf4j
class ShareChannelService {

    @Value('${image.upload.path}')
    public String uploadPath

    public static final int CHANNEL_TYPE_WEIBO = 1
    public static final int CHANNEL_TYPE_WECHAT = 2
    public static final int CHANNEL_TYPE_TOUTIAO = 3
    public static final int CHANNEL_TYPE_QQOM = 4

    public static final int MAX_CHANNEL_NUM_WEIBO = 2
    public static final int MAX_CHANNEL_NUM_WECHAT = 2
    public static final int MAX_CHANNEL_NUM_TOUTIAO = 2
    public static final int MAX_CHANNEL_NUM_QQOM = 2

    @Autowired
    private ShareChannelRepo channelRepo
    @Autowired
    private WeiboOAuthService weiboOAuthSrv
    @Autowired
    private WechatOAuthService wechatOAuthSrv
    @Autowired
    private ToutiaoOAuthService toutiaoOAuthSrv
    @Autowired
    private QqomOAuthService qqomOAuthSrv
    @Autowired
    private WeiboShareService weiboShareSrv
    @Autowired
    private WechatShareService wechatShareSrv
    @Autowired
    private ToutiaoShareService toutiaoShareSrv
    @Autowired
    private QqomShareService qqomShareSrv
    @Autowired
    private CaptureService captureSrv
    @Autowired
    private ICompileService iCompileSrv
    @Autowired
    private MaterialService materialSrv
    @Autowired
    private MaterialRepo materialRepo
    @Autowired
    MaterialService materialService
    @Autowired
    private AgentService agentSrv

    /**
     * ????????????????????????????????????
     * @param userId
     * @param code
     * @return
     */
    def addWeiboShareChannel(HttpServletRequest request, Long userId, String code){

        def list = this.getShareChannels(userId, CHANNEL_TYPE_WEIBO)
        if(list && list.size() >= MAX_CHANNEL_NUM_WEIBO) return null

        def res = weiboOAuthSrv.getAccessToken(request, code)

        if(res?.status == 0) {
            return null
        }

        String uid          = res?.data?.getString('uid')
        String accessToken  = res?.data?.getString('access_token')

        //??????????????????????????????
        def weiboUser = weiboOAuthSrv.getWeiboUser(accessToken, uid)
        if(!weiboUser){
            return null
        }

        // ?????????weiboUser??????????????????????????????????????????????????????????????????weiboUser??????insert mongodb??????????????????????????????
        def accountInfo = [
                "screen_name"   : weiboUser.screen_name,
                "id"            : weiboUser.id,
                "verified_type" : weiboUser.verified_type,
                "name"          : weiboUser.name,
                "idstr"         : weiboUser.idstr,
                "avatar_hd"     : weiboUser.avatar_hd,
                "profile_image_url" : weiboUser.profile_image_url,
                "avatar_large"  : weiboUser.avatar_large,
                "location"      : weiboUser.location
        ]

        //??????????????????
        def result = channelRepo.add(userId, CHANNEL_TYPE_WEIBO, uid, weiboUser?.screen_name as String, weiboUser?.avatar_large as String, accountInfo)

        return result
    }

    /**
     * ????????????????????????????????????????????????
     * @param userId
     * @param code
     * @return
     */
    def addWechatShareChannel(HttpServletRequest request, Long userId, String authCode){

        def list = this.getShareChannels(userId, CHANNEL_TYPE_WECHAT)
        if(list && list.size() >= MAX_CHANNEL_NUM_WECHAT) return null

        def authorizationInfo = wechatOAuthSrv.getAuthorizationInfo(request, authCode)

        if(!authorizationInfo) {
            return null
        }

        //???????????????????????????????????????
        def wechatAccount = wechatOAuthSrv.getWechatAccountInfo(request, authorizationInfo.authorizer_appid as String)
        if(!wechatAccount){
            return null
        }

        //??????????????????
        def result = channelRepo.add(userId, CHANNEL_TYPE_WECHAT, authorizationInfo.authorizer_appid as String, wechatAccount?.nick_name as String, wechatAccount?.head_img as String, wechatAccount)

        return result
    }

    /**
     * ??????????????????????????????????????????
     * @param userId
     * @param code
     * @return
     */
    def addToutiaoShareChannel(HttpServletRequest request, Long userId, String code){

        def list = this.getShareChannels(userId, CHANNEL_TYPE_TOUTIAO)
        if(list && list.size() >= MAX_CHANNEL_NUM_TOUTIAO) return null

        def res = toutiaoOAuthSrv.getAccessToken(request, code)
        if(res?.status == 0) {
            return null
        }

        String uid          = res?.data?.uid
        String accessToken  = res?.data?.access_token

        //?????????????????????????????????
        def toutiaoUser = toutiaoOAuthSrv.getToutiaoUser(request, accessToken, uid)
        if(!toutiaoUser){
            return null
        }

        def accountInfo = JSONObject.parse(toutiaoUser.toString())

        if(accountInfo.extra) {
            accountInfo.extra = JSONObject.toJSONString(accountInfo.extra)
        }
        //??????????????????
        def result = channelRepo.add(userId, CHANNEL_TYPE_TOUTIAO, toutiaoUser?.uid as String, toutiaoUser?.screen_name as String, toutiaoUser?.avatar_url as String, accountInfo)

        return result
    }

    /**
     * ??????????????????????????????????????????
     */
    def addQqomShareChannel(HttpServletRequest request, Long userId, String code){

        def list = this.getShareChannels(userId, CHANNEL_TYPE_QQOM)
        if(list && list.size() >= MAX_CHANNEL_NUM_QQOM) return null

        def res = qqomOAuthSrv.getAccessToken(request, code)

        if(res?.status == 0) {
            return null
        }

        String uid          = res?.data?.getString('openid')
        String accessToken  = res?.data?.getString('access_token')

        //??????????????????????????????
        def qqomUser = qqomOAuthSrv.getQqomUser(accessToken, uid)
        if(!qqomUser){
            return null
        }

        def accountInfo = JSONObject.parse(qqomUser.toString())
        //??????????????????
        def result = channelRepo.add(userId, CHANNEL_TYPE_QQOM, uid, qqomUser?.nick as String, qqomUser?.header as String, accountInfo)

        return result
    }

    /**
     * ?????????????????????????????????????????????
     * @param userId
     * @return
     */
    List getShareChannels(Long userId){

        def result = channelRepo.getShareChannels(userId)

        return result
    }

    List getShareChannels(Long userId, int channelType){
        return channelRepo.getShareChannels(userId, channelType)
    }

    /**
     * ????????????ID??????????????????????????????
     * @param userId
     * @param channelId
     * @return
     */
    def delShareChannel(HttpServletRequest request, Long userId, Long channelId){

        def channel = channelRepo.getChannelById(channelId)
        if(!channel) return

        //????????????
        def res = 1
        if(channel.channelType == CHANNEL_TYPE_WEIBO) {
            String accessToken = weiboOAuthSrv.getAccessTokenFromMongo(request, channel?.channelInfo?.uid as String)
            res = weiboOAuthSrv.cancel(accessToken)
        }
        channelRepo.del(channelId)
        return res
    }
    def delWechatShareChannel (String authorizerAppId) {
        channelRepo.delWechatChannel(authorizerAppId)
    }
    //??????????????????????????????????????????
    def shareMaterialAndAddHistory(HttpServletRequest request, Long userId, String agentId, String orgId, String teamId, String materialId, List<Long> channelIds, String timeStamp, int wechatSyncType) {
        def res = this.shareMaterial(request, userId, agentId, orgId, teamId, materialId, channelIds, timeStamp, wechatSyncType)
        try {
            def object = res.result[0]
            def map = [createTime: new Date()]
            map.putAll(object)
            def history = materialRepo.addHistory(materialId, map)
        } catch (e) {
            log.info("????????????????????????:{},???????????????{}", e, res)
        } finally {
            return res
        }
    }

    def shareMaterial(HttpServletRequest request, Long userId, String agentId, String orgId, String teamId, String materialId, List<Long> channelIds, String timeStamp, int wechatSyncType) {
        //1 ????????????
        Material material = materialRepo.getUserMaterial(userId, materialId)
        if(!material) {
            return apiResult(HttpStatus.SC_NOT_FOUND, '????????????????????????')
        }
        def channelList = channelRepo.getChannelsByIds(userId, channelIds)

        //2 ??????????????????
        //???????????????????????????????????????material->articleOperation???, ????????????????????????????????????????????????????????????
        def articleSyncResult = channelList ? materialSrv.addArticle(userId, orgId, materialId, MaterialRepo.ARTICLE_TYPE_SYNC) : null

        //3 ??????????????????
        def article = articleSyncResult?.msg
        def shareContent = this.wrapArticleSyncContent(request, article, wechatSyncType)

        //4 share
        def result = this.share(request, channelList, shareContent)

        ShareChannelResult shareChannelResult = new ShareChannelResult(
                "_id" : UUID.randomUUID().toString(),
                orgId: orgId,
                timeStamp: timeStamp,
                shareResult: result[0]
        )
        //5 todo ?????????????????????????????????????????????????????????????????????????????????????????????newsOperation???operationType = 5?????????????????????
        if (result.detail[0].status == ShareResult.success)
        {
            channelRepo.addShareChannelResult(shareChannelResult)
            materialService.findAndModifyArticlePush(material, userId, agentId, orgId, teamId, 5, result, timeStamp)
        }

        //6 ???????????????????????????
        def successList = result?.find { it.detail.status == ShareResult.success }
        def failure = result?.find { it.detail.status == ShareResult.failure }
        if(successList) {
            materialSrv.updateMaterialStatus2Published(userId, materialId, 1)
        }
        if (failure) {
            materialSrv.updateMaterialStatus2Published(userId, materialId, 4)
        }

        return apiResult([result : result])
    }

    def wrapArticleSyncContent(HttpServletRequest request, def article, int wechatSyncType) {

        Agent agent = agentSrv.getAgent(request)

        String qrcode = (agent.qrcode) ?: (UrlUtils.getBjjHost(request) + '/theme/default/images/qrcode.png')

        String bjjFooterDom = """<div style="text-align:center;">
            <img src="${qrcode}">
            <div class="">?????????${agent.siteName}??????</div>
        </div>
        """
        Material material = new Material(article.material as Map)

        def shareContent = [
                videoUrl    : StringUtils.getFirstVideoUrl(material.content),
                content     : material.content + bjjFooterDom,
                picUrls     : material.picUrl,      //?????????????????????????????????????????????
                title       : material.title,
                author      : material.author,
                source      : material.source,
                sourceUrl   : this.articleUrl(request, article.id as String),
                digest      : material.contentAbstract,
                thumbUrl    : material.picUrl,  //???????????????????????????????????????????????????????????????
                wechatSyncType  : wechatSyncType    //?????????????????????0-??????????????? 1-????????????
        ]
        return shareContent
    }

    def share(HttpServletRequest request, def channelList, def shareContent) {

        def result = []

        if (shareContent.videoUrl){
            //????????????
            channelList?.each {
                def res = null
                //???????????????????????????????????????????????????????????????????????????????????????????????????
                switch (it.channelType as int) {
                    case CHANNEL_TYPE_WEIBO:
                        res = weiboShareSrv.createWeibo(request, it.channelInfo.uid as String, shareContent)
                        break;
                    case CHANNEL_TYPE_WECHAT:
                        res = wechatShareSrv.createWechat(request, it.channelInfo.uid as String, shareContent)
                        break;
                    case CHANNEL_TYPE_TOUTIAO:
                        res = toutiaoShareSrv.createToutiaoWithVideo(request, it.channelInfo.uid as String, shareContent)
                        break;
                    case CHANNEL_TYPE_QQOM:
                        res = qqomShareSrv.createQqShare(request, it.channelInfo.uid as String, shareContent)
                        break;
                }

                result << [
                        id       : it.id,
                        channelType     : it.channelType,
                        channelUsername : it.channelInfo.name,
                        detail          : res
                ]
            }
        }else {
            //????????????
            channelList?.each {
                def res = null
                //???????????????????????????????????????????????????????????????????????????????????????????????????
                switch (it.channelType as int) {
                    case CHANNEL_TYPE_WEIBO:
                        res = weiboShareSrv.createWeibo(request, it.channelInfo.uid as String, shareContent)
                        break;
                    case CHANNEL_TYPE_WECHAT:
                        res = wechatShareSrv.createWechat(request, it.channelInfo.uid as String, shareContent)
                        break;
                    case CHANNEL_TYPE_TOUTIAO:
                        res = toutiaoShareSrv.createToutiao(request, it.channelInfo.uid as String, shareContent)
                        break;
                    case CHANNEL_TYPE_QQOM:
                        res = qqomShareSrv.createQqShare(request, it.channelInfo.uid as String, shareContent)
                        break;
                }

                result << [
                        id       : it.id,
                        channelType     : it.channelType,
                        channelUsername : it.channelInfo.name,
                        detail          : res
                ]
            }
        }


        return result
    }

    /**
     * ????????????????????????
     * @param pic
     * @return
     */
    String picLocalUrl(String pic) {
        if(pic?.startsWith('data:image')) {
            pic = convertBase64DataToImage(pic)
        }

        Pattern pattern = Pattern.compile('(\\/upload\\/(share|editor|avatar|style|sucai|img)\\/[0-9]{4}\\/[0-9]{2}\\/[0-9]{2}\\/)(.+.(?i)[png|jpeg|jpg|gif])');

        Matcher matcher = pattern.matcher(pic);
        return matcher.find() ? uploadPath + matcher.group(0) : ''
    }

    /**
     * ???base64???????????????????????????????????????????????????
     * @param base64Img
     * @return
     */
    String convertBase64DataToImage(String base64Img) {
        if(!base64Img) return ''
        try {
            //??????????????????
            String base64ImgData = base64Img.replaceAll('^data:image/.+;base64,', '')
            //??????????????????
            String completeFilePath = new StringBuilder()
                    .append('/upload/')
                    .append('img')
                    .append(DateFormatUtils.format(new Date(), "/yyyy/MM/dd/"))
                    .toString()
            String fileName = new StringBuilder()
                    .append(DateFormatUtils.format(new Date(), "HHmmssSSS"))
                    .append(RandomUtils.nextInt(0, 1000))
                    .append('.')
                    .append('png')
                    .toString()

            File fileDirectory = new File(uploadPath + completeFilePath);
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs()
            }
            String filePath = completeFilePath.concat(fileName)

            BASE64Decoder decoder = new BASE64Decoder();
            byte[] bs = decoder.decodeBuffer(base64ImgData);
            FileOutputStream os = new FileOutputStream(uploadPath + filePath);
            os.write(bs)
            os.flush();
            os.close();
            return filePath
        } catch (IOException e) {
            log.error('{}', e)
            return ''
        }
    }

    String dealBase64Image2Local(HttpServletRequest request, String content) {
        String host = UrlUtils.getBjjHost(request)
        def imgList = StringUtils.extractImgUrl(content)
        imgList?.each {
            if(it?.startsWith('data:image')) {
                def img = this.convertBase64DataToImage(it as String)
                it = it.replaceAll('\\+', '\\\\+')
                content = content.replaceAll(it as String, host.concat(img))
            } else if (it?.startsWith('//')) {
                String url = "https:" + it
                content = content.replace(it as String, url)
            }
        }
        return content
    }

    /**
     * ????????????ID??????????????????++???????????????
     * @param request
     * @param articleId
     * @return
     */
    String articleUrl (HttpServletRequest request, String articleId) {
        return UrlUtils.getBjjHost(request) + '/compile/article.html?id=' + articleId
    }

    public static void main(String[] args){

        String u = 'http://bjj.istarshine.com/upload/share/2017/11/03/12343.pNga12'

        def srv = new ShareChannelService()
        println srv.picLocalUrl(u)
    }

}

package com.istar.mediabroken.service.wechat

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.api.ShareResult
import com.istar.mediabroken.api.WechatErrorEnum
import com.istar.mediabroken.entity.SystemSetting
import com.istar.mediabroken.repo.admin.SettingRepo
import com.istar.mediabroken.service.ShareChannelService
import com.istar.mediabroken.utils.HttpClientUtil
import com.istar.mediabroken.utils.StringUtils
import com.mashape.unirest.http.Unirest
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest

import static com.istar.mediabroken.api.ShareResult.*

/**
 * Author : YCSnail
 * Date   : 2017-05-22
 * Email  : liyancai1986@163.com
 */
@Service
@Slf4j
class WechatShareService {

    @Value('${image.upload.path}')
    public String uploadPath

    @Autowired
    private WechatOAuthService wechatOAuthSrv
    @Autowired
    private ShareChannelService shareChannelSrv
    @Autowired
    private SettingRepo settingRepo

    public static final int MATERIAL_UPLOAD_TEMPORARY = 0
    public static final int MATERIAL_UPLOAD_PERMANENT = 1

    boolean hasMaterialAuthority(HttpServletRequest request, String authorizerAppId) {
        def funcInfo = wechatOAuthSrv.getFuncInfo(request, authorizerAppId)
        def funcInfoList = JSONObject.parseArray(funcInfo)
        if(!funcInfoList) return false

        boolean res = false
        funcInfoList?.each {
            if(it.funcscope_category.id == WechatAuthorityEnum.material.index) {
                res = true
                return
            }
        }
        return res
    }
    boolean hasMassSendAndNotifyAuthority(HttpServletRequest request, String authorizerAppId) {
        def funcInfo = wechatOAuthSrv.getFuncInfo(request, authorizerAppId)
        def funcInfoList = JSONObject.parseArray(funcInfo)
        if(!funcInfoList) return false

        boolean res = false
        funcInfoList?.each {
            if(it.funcscope_category.id == WechatAuthorityEnum.mass_send_and_notify.index) {
                res = true
                return
            }
        }
        return res
    }

    def createWechat(HttpServletRequest request, String uid, def shareContent) {

        //??????????????????????????????
        if (!hasMaterialAuthority(request, uid)){
            return [
                    status  : 0,
                    msg     : '?????????????????????' + WechatAuthorityEnum.material.value
            ]
        }

        String accessToken = wechatOAuthSrv.getAuthorizationAccessToken(request, uid)
        if(!accessToken) {
            return [
                    status  : 0,
                    msg     : 'token?????????????????????????????????????????????'
            ]
        }

        //??????????????????????????????????????????????????????????????????
        String content = this.dealContentImage(accessToken, shareContent.content as String)
        shareContent.content = content

        boolean isMassSend = (shareContent.wechatSyncType == 1)
        // ???????????????????????????media_id
        def materialInfo = this.addMaterial(
                accessToken,
                shareContent.thumbUrl as String,
                isMassSend ? MATERIAL_UPLOAD_TEMPORARY : MATERIAL_UPLOAD_PERMANENT
        )
        if(materialInfo.status != ShareResult.success) {
            return shareFailure(materialInfo.msg, materialInfo.code)
        }
        shareContent.thumbMediaId = materialInfo.meidaId

        if(!isMassSend){
            //?????????????????????????????????
            return this.addWechatMaterialNews(accessToken, shareContent)
        } else {
            //??????????????????????????????
            if (!hasMassSendAndNotifyAuthority(request, uid)){
                return [
                        status  : 0,
                        msg     : '?????????????????????' + WechatAuthorityEnum.mass_send_and_notify.value
                ]
            }

            //????????????????????????
            def result = this.addWechatMessageNews(accessToken, shareContent)
            if(result.status == ShareResult.success){

                def massSendSetting = this.getWechatMassSendSetting()

                return massSendSetting.massSend ?
                        this.sendWechatMessage(accessToken, result.msg) :                     //??????????????????
                        this.previewWechatMessage(accessToken, result.msg, massSendSetting.wechatName as String)                //??????
            } else {
                return result
            }
        }
    }

    //??????????????????????????????????????????????????????????????????????????????????????????
    def createWechatWithVideo(HttpServletRequest request, String uid, def shareContent) {

        //??????????????????????????????
        if (!hasMaterialAuthority(request, uid)){
            return [
                    status  : 0,
                    msg     : '?????????????????????' + WechatAuthorityEnum.material.value
            ]
        }

        String accessToken = wechatOAuthSrv.getAuthorizationAccessToken(request, uid)
        if(!accessToken) {
            return [
                    status  : 0,
                    msg     : 'token?????????????????????????????????????????????'
            ]
        }

        String videoUrl = shareContent.videoUrl//????????????????????????????????????
        videoUrl = "http://zhxg-01.oss-cn-beijing.aliyuncs.com/1548295224546/my-first-key3.mp4"

        //????????????
        String url = "https://api.weixin.qq.com/cgi-bin/material/add_material?access_token=" + accessToken+"&type=video";
//        String url = "http://file.api.weixin.qq.com/cgi-bin/media/upload?access_token=" + accessToken+"&type=video";
        String media_id = ""
        try {
            String title = shareContent.title
            String introduction = shareContent.digest
            def result = uploadVideo(url, videoUrl, title, introduction)

          /*  if (result != null){
                media_id = JSON.parse(result).media_id
                String article_url = "https://api.weixin.qq.com/cgi-bin/material/get_material?access_token=${accessToken}"
                def article_params = [
                        media_id    : media_id
                ]
                def article_res = Unirest.post(article_url).field("media_id", media_id)*//*fields(article_params as Map<String, String>)*//*.asJson()
                def article_result = article_res.body.object
                return ('success' == article_result?.message) ? shareSuccess() : shareFailure(article_result?.isNull('data') ? '' : article_result.data, 0)
            }*/

//            createWechat(request, uid, shareContent)
            return result == null ? shareFailure() : shareSuccess()
        } catch (Exception e) {
            e.printStackTrace()
            return shareFailure()
        }

    }

    public static String uploadVideo(String url, String filePath, String title, String introduction) {
        String result = null;

        HttpURLConnection downloadCon = null;
        InputStream inputStream = null;
        try {
            URL urlFile = new URL(filePath);
            downloadCon = (HttpURLConnection) urlFile.openConnection();
            inputStream = downloadCon.getInputStream();

            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            String boundary = "-----------------------------"+System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

            OutputStream output = conn.getOutputStream();
            output.write(("--" + boundary + "\r\n").getBytes());
            String regex = ".*/([^\\.]+)";
            output.write(String.format("Content-Disposition: form-data; name=\"media\"; filename=\"%s\"\r\n", filePath.replaceAll(regex, "\$1")).getBytes());
            output.write("Content-Type: video/mp4 \r\n\r\n".getBytes());
            int bytes = 0;
            byte[] bufferOut = new byte[1024];
            while ((bytes = inputStream.read(bufferOut)) != -1) {
                output.write(bufferOut, 0, bytes);
            }
            inputStream.close();

            output.write(("--" + boundary + "\r\n").getBytes());
            output.write("Content-Disposition: form-data; name=\"description\";\r\n\r\n".getBytes());
            output.write(String.format("{\"title\":\"%s\", \"introduction\":\"%s\"}",title,introduction).getBytes());
            output.write(("\r\n--" + boundary + "--\r\n\r\n").getBytes());
            output.flush();
            output.close();
            inputStream.close();
            InputStream resp = conn.getInputStream();
            StringBuffer sb = new StringBuffer();
            while((bytes= resp.read(bufferOut))>-1)
                sb.append(new String(bufferOut,0,bytes,"utf-8"));
            resp.close();
            result = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }


    String dealContentImage(String accessToken, String content) {
        if(!content) return
        def imgList = StringUtils.extractImgUrl(content)
        imgList?.each {
            String wechatImg = this.getWechatImageUrl(accessToken, it as String)
            if(wechatImg){
                content = content.replaceAll(it as String, wechatImg)
            }
        }
        return content
    }

    /**
     * ??????????????????
     * @param accessToken
     * @param shareContent
     * @return
     */
    def addWechatMaterialNews (String accessToken, def shareContent) {
        String url = 'https://api.weixin.qq.com/cgi-bin/material/add_news?access_token=' + accessToken
        def params = [
                "articles"  : [
                        [
                                "title"            : shareContent.title as String,
                                "thumb_media_id"   : shareContent.thumbMediaId as String,
                                "author"           : shareContent.author,
                                "digest"           : org.apache.commons.lang3.StringUtils.substring((shareContent.digest?:'') as String, 0, 120),
                                "show_cover_pic"   : 0,        //?????????????????????0???false??????????????????1???true????????????
                                "content"          : shareContent.content as String,
                                "content_source_url"    : shareContent.sourceUrl
                        ]
                        //?????????????????????????????????????????????????????????articles??????
                ]
        ]
        try {
            def res = Unirest.post(url)
                    .header("accept", "application/json")
                    .body(JSONObject.toJSONString(params))
                    .asJson()
            def result = res.body.object
            log.info(['wechat', '????????????????????????', result].join(':::') as String)


            if(!result.isNull('errcode')){
                log.error(['wechat', '????????????????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return shareFailure(WechatErrorEnum.getErrorMsg(result.errcode as int), result.errcode as int)
            }
            //????????????
            return shareSuccess(result?.media_id as String)

        } catch (Exception e) {

            log.error(['wechat', '????????????????????????', e.message].join(':::') as String)
            log.error('wechat:::', e)
            return shareFailure('??????????????????', 0)
        }
    }

    /**
     * ????????????<b>??????</b>???????????????????????????????????????????????????
     * ????????????????????????????????????????????????????????????media_id?????????
     */
    def addWechatMessageNews (String accessToken, def shareContent) {
        String url = 'https://api.weixin.qq.com/cgi-bin/media/uploadnews?access_token=' + accessToken
        def params = [
                "articles"  : [
                        [
                                "title"            : shareContent.title as String,
                                "thumb_media_id"   : shareContent.thumbMediaId as String,
                                "author"           : shareContent.author,
                                "digest"           : org.apache.commons.lang3.StringUtils.substring((shareContent.digest?:'') as String, 0, 120),
                                "show_cover_pic"   : 0,        //?????????????????????0???false??????????????????1???true????????????
                                "content"          : shareContent.content as String,
                                "content_source_url"    : shareContent.sourceUrl
                        ]
                        //?????????????????????????????????????????????????????????articles??????
                ]
        ]

        try {
            def res = Unirest.post(url)
                    .header("accept", "application/json")
                    .body(JSONObject.toJSONString(params))
                    .asJson()
            def result = res.body.object
            log.info(['wechat', '??????????????????????????????', result].join(':::') as String)


            if(!result.isNull('errcode')){
                log.error(['wechat', '??????????????????????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return shareFailure(WechatErrorEnum.getErrorMsg(result.errcode as int), result.errcode as int)
            }
            //????????????
            return shareSuccess(result?.media_id as String)

        } catch (Exception e) {

            log.error(['wechat', '??????????????????????????????', e.message].join(':::') as String)
            log.error('wechat:::', e)
            return shareFailure('??????????????????', 0)
        }
    }

    /**
     * ??????????????????
     * @param accessToken
     * @param shareContent
     * @return
     */
    def sendWechatMessage(String accessToken, String mediaId) {
        String url = 'https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=' + accessToken

        def params = [
                "filter"    : [ "is_to_all" : true ],
                "mpnews"    : [ "media_id"  : mediaId ],
                "msgtype"   : "mpnews",
                send_ignore_reprint : 1     //?????????????????????????????????????????????????????????????????????
        ]

        try {
            def res = Unirest.post(url)
                    .header("accept", "application/json")
                    .body(JSONObject.toJSONString(params))
                    .asJson()
            def result = res.body.object
            log.info(['wechat', '??????????????????', result].join(':::') as String)

            if(!result.isNull('errcode') && (result.errcode as int) > 0){
                log.error(['wechat', '??????????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return shareFailure(WechatErrorEnum.getErrorMsg(result.errcode as int), result.errcode as int)
            }
            //????????????
            return shareSuccess()

        } catch (Exception e) {

            log.error(['wechat', '??????????????????', e.message].join(':::') as String)
            log.error('wechat:::', e)
            return shareFailure('??????????????????', 0)
        }
    }

    /**
     * ??????????????????
     * @param accessToken
     * @param mediaId
     * @return
     */
    def previewWechatMessage(String accessToken, String mediaId, String wechatName) {
        String url = 'https://api.weixin.qq.com/cgi-bin/message/mass/preview?access_token=' + accessToken

        def params = [
                "towxname"  : wechatName,
                "mpnews"    : [ "media_id"  : mediaId ],
                "msgtype"   : "mpnews"
        ]

        try {
            def res = Unirest.post(url)
                    .header("accept", "application/json")
                    .body(JSONObject.toJSONString(params))
                    .asJson()
            def result = res.body.object
            log.info(['wechat', '??????????????????', result].join(':::') as String)

            if(!result.isNull('errcode') && (result.errcode as int) > 0){
                log.error(['wechat', '??????????????????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return shareFailure(WechatErrorEnum.getErrorMsg(result.errcode as int), result.errcode as int)
            }
            //????????????
            return shareSuccess()

        } catch (Exception e) {

            log.error(['wechat', '??????????????????', e.message].join(':::') as String)
            log.error('wechat:::', e)
            return shareFailure('??????????????????', 0)
        }
    }

    /**
     * ??????????????????????????????    ??????type????????????????????????????????????
     * @param accessToken
     * @param img
     * @return
     */
    def addMaterial (String accessToken, String materialUrl, int type) {

        if(!materialUrl) return shareFailure(WechatErrorEnum.ERROR_INVALID_MEDIA_ID)

        try {
            String localPicPath = shareChannelSrv.picLocalUrl(materialUrl)
            if(!localPicPath && materialUrl.startsWith('http')) {
                localPicPath = HttpClientUtil.downLoadFromUrl(materialUrl, UUID.randomUUID().toString().concat('.png'), uploadPath.concat('/upload/share'))
            }

            String url = ''
            switch (type) {
                case MATERIAL_UPLOAD_TEMPORARY:
                    url = "https://api.weixin.qq.com/cgi-bin/media/upload?access_token=${accessToken}&type=image"
                    break
                case MATERIAL_UPLOAD_PERMANENT:
                    url = "https://api.weixin.qq.com/cgi-bin/material/add_material?access_token=${accessToken}&type=image"
                    break
            }

            def res = HttpClientUtil.doPost(url, new File(localPicPath))
            def result = JSONObject.parseObject(res)
            log.info(['wechat', '????????????????????????????????????mediaId??????', result].join(':::') as String)

            if(result.errcode){
                log.error(['wechat', '????????????????????????????????????mediaId??????', result.errcode as String, result.errmsg as String].join(':::') as String)
                return [
                        status  : 0,
                        msg     : result.errmsg,
                        code    : result.errcode,
                        meidaId : ''
                ]
            }

            return [
                    status  : 1,
                    msg     : result?.url,
                    meidaId : result?.media_id
            ]
        } catch (Exception e) {
            e.printStackTrace()
            return [
                    status  : 0,
                    msg     : '????????????????????????',
                    code    : HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    meidaId : ''
            ]
        }
    }

    /**
     * ????????????????????????????????????
     * @param url
     * @return
     */
    String getWechatImageUrl(String accessToken, String image){

        String localPicPath = shareChannelSrv.picLocalUrl(image)
        if(!localPicPath && !image.startsWith('data:image')) {
            localPicPath = HttpClientUtil.downLoadFromUrl(image, UUID.randomUUID().toString().concat('.png'), uploadPath.concat('/upload/share'))
        }

        String url = 'https://api.weixin.qq.com/cgi-bin/media/uploadimg?access_token=' + accessToken
        try {
            def res = HttpClientUtil.doPost(url, new File(localPicPath))
            log.info(['wechat', '??????????????????????????????', res].join(':::') as String)
            if(res) {
                def result = JSONObject.parseObject(res)
                return result?.url
            }else {
                return ''
            }
        }catch (Exception e) {
            e.printStackTrace()
            return ''
        }
    }

    /**
     * ?????????????????????
     */
    String getWechatVideoUrl(String accessToken, String videoUrl){

//        String localPicPath = shareChannelSrv.picLocalUrl(image)
//        if(!localPicPath && !image.startsWith('data:image')) {
//            localPicPath = HttpClientUtil.downLoadFromUrl(image, UUID.randomUUID().toString().concat('.png'), uploadPath.concat('/upload/share'))
//        }

        String url = 'https://api.weixin.qq.com/cgi-bin/material/add_material?access_token=' + accessToken
        try {
            JSON map = new JSONObject()
            map.put("title","????????????title")
            map.put("introduction","??????introduction")

            def res = Unirest.post(url).field("media",new File("E://huajuan.mp4"))
                    .field("type","video")
                    .field("description",map)
                    .asJson()
//            def res = HttpClientUtil.doPost(url, new File(localPicPath))
            log.info(['wechat', '??????????????????????????????', res].join(':::') as String)
            if(res) {
                def result = JSONObject.parseObject(res)
                return result?.url
            }else {
                return ''
            }
        }catch (Exception e) {
            e.printStackTrace()
            return ''
        }
    }

    /**
     * ?????????????????????????????????????????????
     * @return {
     *     massSend     : true|false
     *     wechatName   : $string
     * }
     */
    def getWechatMassSendSetting(){
        SystemSetting setting = settingRepo.getSystemSetting("wechat","massSendOrPreview")
        return setting ? setting.content : [
                massSend    : false,
                wechatName  : 'flame_Liyc'
        ]
    }


}

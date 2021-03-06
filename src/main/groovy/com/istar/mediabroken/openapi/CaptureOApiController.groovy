package com.istar.mediabroken.openapi

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.service.CaptureService
import com.istar.mediabroken.utils.DateUitl
import com.istar.mediabroken.utils.HtmlUtil
import com.istar.mediabroken.utils.StringUtils
import com.istar.mediabroken.utils.WechatHtmlUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.istar.mediabroken.api.ApiResult.apiResult;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET

@RequestMapping(value = "/openapi/capture")
@RestController
@Slf4j
class CaptureOApiController {

    @Autowired
    private CaptureService captureSrv

    /*
    *
    * 查询未推送的新闻数据,最多10条, 按创建时间正序排序
    *
    *
    * 返回:
    *
    * * 新闻列表
    String title
    String publishTime
    String source
    String author
    String keyword
    String url
    String newsId
    String content
    String contentAbstract
    */

    @RequestMapping(value = "/newsPush", method = GET)
    Map getNewsPushList(
            @OrgId String orgId,
            @Params MapWraper data
    ) {
        JSONObject params = data.getMap()
        def styleFlag = params.get("styleFlag") ?: "1"//默认获取带样式的新闻,1
        def list = []
        captureSrv.getOpenNewsPushList(orgId)?.each {
            def news = it.news
            StringBuilder imgUrls = new StringBuilder()
            String[] imgs = news.imgUrls
            if (imgs) {
                for (int i = 0; i < imgs.size(); i++) {
                    imgUrls.append("<img style='max-width:100%;' src='" + imgs[i] + "'/>")
                }
            }
            def keywords = news.keywords
            String keyword = ""
            if (it.pushType == 1) {
                try {
                    JSONArray keywordsList = JSONArray.parseArray(keywords);
                    keywordsList.each {
                        keyword += it.word + ","
                    }
                } catch (Exception e) {
                    log.error("关键词转换错误" + news.keywords, e)
                }
            } else if (it.pushType == 4) {
                try {
                    def keywordsList = keywords.split(",")
                    keywordsList.each {
                        keyword += it + ","
                    }
                } catch (Exception e) {
                    log.error("关键词转换错误" + news.keywords, e)
                }
            }
            String content = ""
            if (news.content) {
                content = news.content
                if ("0".equals(styleFlag)) {
                    //如果新闻内容不为空并且用户需要不带class样式的content，那么去掉获取的样式
                    if (news.newsType == 6) {
                        //todo 将来没有旧数据时候可以删除此if语句
                        if (news.content.startsWith("<div class=\"rich_media_content \"")) {
                            content = WechatHtmlUtils.removeWechatHtmlFormat(news.content)
                        }
                    } else {
                        if (news.isHtmlContent) {
                            content = HtmlUtil.removeWebHtmlFormat(news.content)
                        } else {
                            //todo 添加p标签
                            content = StringUtils.ContentText2Html(news.content) + imgUrls.toString()
                        }

                    }
                } else {
                    //获取带样式新闻--添加微信style，网站和微博不处理，微信如果是新采集的带样式的数据需要加上style的头部
                    if (news.newsType == 6) {
                        //todo 将来没有旧数据时候可以删除此if语句
                        if (news.content.startsWith("<div class=\"rich_media_content \"")) {
                            content = WechatHtmlUtils.weChatNewsFormat(news.content)
                        }
                    } else {
                        if (!news.isHtmlContent) {
                            content = StringUtils.ContentText2Html(news.content) + imgUrls.toString()
                        }
                    }
                }
            }
            String source = "";
            if (it.pushType == 1){
                source = news.siteName;
            }else if (it.pushType == 4){
                source = news.source;
            }
            list << [
                    newsId         : news.id ?: news._id,
                    title          : news.title,
                    source         : source,
                    author         : news.author,
                    keyword        : keyword,
                    url            : news.url,
                    content        : content,
                    contentAbstract: news.contentAbstract,
                    publishTime    : DateUitl.convertEsDate(news.publishTime ?: news.createTime).getTime(),
                    createTime     : DateUitl.convertEsDate(news.createTime).getTime()
            ]
        }
        return apiResult([list: list])
    }

    /*
    *
    * 确认已处理完的新闻, newsIds是一个逗号分隔的字符串列表
    *
    * 把状态从未推送修改成已推送
    *
    * /openapi/capture/newsPush/723463874qrqwer,82u3wuriuqerweur,i3eruweoiruweoriuw?_method=delete&orgId=test_org
    *
    * 返回: {"status": 200}
    */

    @RequestMapping(value = "newsPush", method = DELETE)
    Map deleteNewsPush(
            @OrgId String orgId,
            @Params MapWraper data
    ) {
        JSONObject params = data.getMap()
        def newsIds = params.newsIds ?: params.data
        captureSrv.updateNewsPush2Pushed(orgId, newsIds)
        return apiResult()
    }
}

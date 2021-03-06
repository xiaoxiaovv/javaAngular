package com.istar.mediabroken.service.capture

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.entity.News
import com.istar.mediabroken.entity.account.Account
import com.istar.mediabroken.entity.capture.NewsOperation
import com.istar.mediabroken.entity.favoriteGroup.FavoriteGroup
import com.istar.mediabroken.repo.account.AccountRepo
import com.istar.mediabroken.repo.account.TeamRepo
import com.istar.mediabroken.repo.capture.InstantSiteRepo
import com.istar.mediabroken.repo.capture.NewsRepo
import com.istar.mediabroken.repo.capture.SiteRepo
import com.istar.mediabroken.repo.compile.MaterialRepo
import com.istar.mediabroken.repo.favoriteGroup.FavoriteGroupRepo
import com.istar.mediabroken.utils.*
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.http.HttpStatus
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.text.SimpleDateFormat

import static com.istar.mediabroken.api.ApiResult.apiResult

/**
 * Author: Luda
 * Time: 2017/7/28
 */
@Service
@Slf4j
class NewsService {
    @Autowired
    NewsRepo newsRepo
    @Autowired
    SiteRepo siteRepo
    @Autowired
    AccountRepo accountRepo;
    @Autowired
    FavoriteGroupRepo favoriteGroupRepo
    @Autowired
    MaterialRepo materialRepo
    @Autowired
    TeamRepo teamRepo
    @Autowired
    InstantSiteRepo instantSiteRepo

    /**
     * 获取newsId的新闻
     * @param newsId
     * @param style 1获取带样式新闻（带有div css等样式），0获取不带样式新闻（新闻段落以<p>标签包裹）
     * @param original true获取es库中未经处理过的原始新闻，false获取经过style不同处理后的新闻
     * @return
     */
    Map getNewsById(String newsId, int style, boolean original) {
        def news = newsRepo.getNewsById(newsId)
        if (news) {
            if (original) {
                return apiResult(status: HttpStatus.SC_OK, msg: news)
            }
            if (style == 1) {//获取带样式的新闻内容
                if (news.get("htmlContent")) {
                    if (news.newsType == 6) {
                        news.content = WechatHtmlUtils.weChatNewsFormat(news.htmlContent)
                    } else {
                        news.content = news.htmlContent
                    }
                    //news.content = news.htmlContent
                    news.isHtmlContent = true
                } else {
                    news.content = StringUtils.ContentText2Html(news.content)
                }
            }
            if (style == 0) {//获取不带样式的新闻内容，在点击新闻编辑进入编辑器时使用
                StringBuilder imgUrls = new StringBuilder()
                String[] imgs = news.imgUrls
                if (imgs) {
                    for (int i = 0; i < imgs.size(); i++) {
                        imgUrls.append("<p><img style='max-width:100%;' src='" + imgs[i] + "'/></p>")
                    }
                }
                if (news.get("htmlContent")) {
                    news.content = news.htmlContent
                    news.imgUrls = []
                }
                if (news.newsType == 6 && news.get("htmlContent")) {
//                    if (news.content.startsWith("<div class=\"rich_media_content \"")) {
                    news.content = WechatHtmlUtils.removeWechatHtmlFormat(news.content)
//                    }
                } else {
                    if (news.get("htmlContent")) {//原文带样式的去掉样式
                        news.content = HtmlUtil.removeWebHtmlFormat(news.content)
                    } else {//原文不带样式
                        news.content = StringUtils.ContentText2Html(news.content) + imgUrls
                        news.imgUrls = []
                    }
                }
                if (news.content) {
                    news.content = StringUtils.removeSpaceCode(news.content);//去掉多余空格特殊字符、首行缩进的空格
                }
            }
            def keywordsStr = news.keywords
            def keywords = []
            if (null != keywordsStr && !(keywordsStr.equals(""))) {
                try {
                    JSONArray keywordArray = JSONArray.parse(keywordsStr)
                    keywordArray.each {
                        keywords << it.word
                    }
                } catch (Exception e) {
                    log.error("新闻关键词获取失败newsId:{}--newsUrl:{}--keywords:{}--错误信息:{}", news.id, news.url, news.keywords, e.getMessage())
                }
            }
            news.keywords = keywords
            //时间
            news.publishTime = DateUitl.convertEsDate(news.publishTime as String).getTime()
            news.captureTime = DateUitl.convertEsDate(news.captureTime as String).getTime()
            if (news.firstPublishTime) {
                news.firstPublishTime = DateUitl.convertEsDate(news.firstPublishTime as String).getTime()
            } else {
                news.firstPublishTime = DateUitl.convertEsDate(news.publishTime as String).getTime()
            }
            news.classification = []//classification字段意义改变，返回无意义
            return apiResult(status: HttpStatus.SC_OK, msg: news)
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "没有找到新闻信息或新闻信息已经过期")
        }
    }

/*    String newsUrlFormant(String news, String newsUrl) {
        Parser parser = Parser.createParser(news, "utf-8")
        //NodeFilter filter = new RegexFilter("^(?!pic)")
        //NodeList newsList = parser.extractAllNodesThatMatch(filter)
        NodeList newsList = parser.parse(null);
        NodeList list = imgUrlFormantByRecurse(newsList, newsUrl, null)
        return list.toHtml()
    }

    NodeList imgUrlFormantByRecurse(NodeList list, String websiteUrl, String hrefUrl) {
        if (list == null) {
            return null
        }
        Node node = null
        SimpleNodeIterator iterator = list.elements()
        def urlHead = websiteUrl.substring(0, websiteUrl.indexOf("://") + 3)
        def urlBody = websiteUrl.substring(websiteUrl.indexOf("://") + 3)
        def urlArray = urlBody.split("/")
        while (iterator.hasMoreNodes()) {
            node = iterator.nextNode()
            if (node == null)
                break
            imgUrlFormant(node, urlHead, urlArray, websiteUrl, hrefUrl)

        }

        return list
    }

    private void imgUrlFormant(Node node, String urlHead, String[] urlArray, String websiteUrl, String hrefUrl) {
        if (node instanceof Tag) {
            def imgUrl = new StringBuffer(urlHead)
            Tag tag = (Tag) node
            hrefUrl = modifyImgSrc(websiteUrl, hrefUrl, tag)
            removeSourceAttribute(websiteUrl, tag)
            hiddenImage(websiteUrl, tag)
            if (tag.getText().startsWith("img")) {
                def srcUrl = tag.getAttribute("src")
                def imgStyle = tag.getAttribute("style")
                def dataSrcUrl = tag.getAttribute("data-src")
                def dataOriginal = tag.getAttribute("data-original")
                //处理彭博新闻相关图片
                def dataNative = tag.getAttribute("data-native-src")
                //处理新浪博客相关图片
                def realSrc = tag.getAttribute("real_src")
                if (hrefUrl) {
                    srcUrl = hrefUrl
                }
                if (dataOriginal) {
                    imgUrl.delete(0, imgUrl.length())
                    imgUrl.append(dataOriginal)
                } else if (dataNative) {
                    imgUrl.delete(0, imgUrl.length())
                    imgUrl.append(dataNative)
                } else if (realSrc) {
                    imgUrl.delete(0, imgUrl.length())
                    imgUrl.append(realSrc)
                } else if (dataSrcUrl) {
                    imgUrl.delete(0, imgUrl.length())
                    imgUrl.append(dataSrcUrl)
                } else if (srcUrl) {
                    if (srcUrl.startsWith("/") && !srcUrl.startsWith("//")) {
                        imgUrl.append(urlArray[0] + srcUrl)
                    } else if (!srcUrl.contains("/")) {
                        for (int i = 0; i < urlArray.length - 1; i++) {
                            imgUrl.append(urlArray[i] + "/")
                        }
                        imgUrl.append(srcUrl)
                    } else if (srcUrl.startsWith("./")) {
                        for (int i = 0; i < (urlArray.length - 1); i++) {
                            imgUrl.append(urlArray[i] + "/")
                        }
                        srcUrl = srcUrl.substring(srcUrl.indexOf("./") + 2)
                        imgUrl.append(srcUrl)
                    } else if (srcUrl.startsWith("../")) {
                        def srcUrlArry = srcUrl.split("\\../")
                        for (int i = 0; i < (urlArray.length - srcUrlArry.length); i++) {
                            imgUrl.append(urlArray[i] + "/")
                        }
                        while (srcUrl.startsWith("../")) {
                            srcUrl = srcUrl.substring(srcUrl.indexOf("../") + 3)
                        }
                        imgUrl.append(srcUrl)
                    } else if (srcUrl.startsWith("data")) {
                        imgUrl.append(urlArray[0] + "/" + srcUrl)
                    } else if (websiteUrl.contains("reuters.com") && srcUrl.contains("&amp;")) {
                        srcUrl = srcUrl.replace("w=20", "w=1280")
                        imgUrl.delete(0, imgUrl.length())
                        imgUrl.append(srcUrl)
                    } else if (srcUrl.contains("&amp;")) {
                        imgUrl.delete(0, imgUrl.length())
                        srcUrl = srcUrl.substring(0, srcUrl.indexOf("&amp;"))
                        imgUrl.append(srcUrl)
                    } else if (websiteUrl.contains("http://36kr.com") && srcUrl.startsWith("\\\"")) {
                        imgUrl.delete(0, imgUrl.length())
                        srcUrl = srcUrl.replace("\\\"", "")
                        imgUrl.append(srcUrl)
                    } else if (websiteUrl.contains("ly.scdaily.cn") && srcUrl.startsWith("attachments")) {
                        String url = urlArray[0].replace("https:", "http:")
                        imgUrl.append(url + "/" + srcUrl)
                    } else {
                        imgUrl.delete(0, imgUrl.length())
                        imgUrl.append(srcUrl)
                    }
                }
                tag.setAttribute("src", imgUrl.toString())
                hiddenQRcodeImage(websiteUrl, imgStyle, srcUrl, tag)
            }

            if (node.getChildren() != null)
                imgUrlFormantByRecurse(node.getChildren(), websiteUrl, hrefUrl)


        }
    }

    private String modifyImgSrc(String websiteUrl, String hrefUrl, Tag tag) {
        def srcUrl = tag.getAttribute("src")
        if ((websiteUrl.contains("http://sports.cnr.cn/") || websiteUrl.contains("http://pic.cnr.cn/")) && !hrefUrl) {
            hrefUrl = tag.getAttribute("href")
        }
        hrefUrl
    }

    private void removeSourceAttribute(String websiteUrl, Tag tag) {
        if (websiteUrl.contains("https://www.slobodnadalmacija.hr") && tag.getText().startsWith("source")) {
            tag.removeAttribute("srcset")
        }
    }

    private void hiddenQRcodeImage(String websiteUrl, String imgStyle, String srcUrl, Tag tag) {
        if (srcUrl == "http://file.iqilu.com/custom/new/v2016/images/qr-detail-morning.jpg") {
            setImgHidden(tag)
        } else if (srcUrl == "http://gg.gansudaily.com.cn/images/2015/20151229sz01.jpg") {
            setImgHidden(tag)
        } else if (websiteUrl.contains("http://www.xj.xinhuanet.com") && imgStyle == "WIDTH: 482px; HEIGHT: 304px") {
            setImgHidden(tag)
        } else if (websiteUrl.contains("http://www.xj.xinhuanet.com") && imgStyle == "HEIGHT: 304px; WIDTH: 482px") {
            setImgHidden(tag)
        }
    }

    private void hiddenImage(String websiteUrl, Tag tag) {
        if (websiteUrl.contains("www.beijing.gov.cn")) {
            def tagId = tag.getAttribute("id")
            if (tagId && tagId.contains("pic")) {
                setImgHidden(tag)
            }
        } else if (websiteUrl.contains("http://www.xinhuanet.com/politics") && tag.getText().startsWith("iframe")) {
            setImgHidden(tag)
        }
    }

    private setImgHidden(Tag tag) {
        tag.setAttribute("style", "display:none;")
    }*/

    Map getNewsListByIds(List newsIds) {
        def newsListMap = newsRepo.getNewsListByIds(newsIds, 0)
        if (!newsListMap) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "获取新闻信息失败")
        }
        def newsReturn = []
        newsListMap.newsList.each { news ->
            if (news) {
                news.content = StringUtils.Text2Html(news.content)
                def keywordsStr = news.keywords
                def keywords = []
                if (null != keywordsStr && !(keywordsStr.equals(""))) {
                    try {
                        JSONArray keywordArray = JSONArray.parse(keywordsStr)
                        keywordArray.each {
                            keywords << it.word
                        }
                    } catch (Exception e) {
                        log.error("新闻关键词获取失败newsId:{}--newsUrl:{}--keywords:{}--错误信息:{}", news.id, news.url, news.keywords, e.getMessage())
                    }
                }
                news.keywords = keywords
                //时间
                news.publishTime = DateUitl.convertEsDate(news.publishTime as String).getTime()
                news.captureTime = DateUitl.convertEsDate(news.captureTime as String).getTime()
                newsReturn << news
            }
        }
        return apiResult(status: HttpStatus.SC_OK, newsList: newsReturn)
    }

    List getSitesKeyCloud(long userId) {
        def result = []
        def endDate = DateUitl.getBeginDayOfYesterday()
        def startDate = DateUitl.addDay(endDate, -7)
        def sdf = new SimpleDateFormat("yyyy-MM-dd")
        def endDay = sdf.format(endDate)
        def startDay = sdf.format(startDate)
        def sites = siteRepo.getUserSites(userId)
        if (!sites) {
            return result
        }
        def resultMap = [:]
        List siteKeyWords = newsRepo.getSitesKeyCloud(sites, startDay, endDay)
        def siteWords = [:]
        siteKeyWords.each { keywords ->
            def wordCloudList = keywords.wordCloud
            wordCloudList.each {
                def word = it.word
                def count = it.count
                if (siteWords.containsKey(word)) {
                    def currCount = siteWords.get(word)
                    currCount += count
                    siteWords.put(word, currCount)
                } else {
                    siteWords.put(word, count)
                }
            }
        }
        siteWords = siteWords.sort { a, b ->
            (int) b.value - (int) a.value
        }//降序

        List channelKeyList = siteWords.keySet() as List
        def size = channelKeyList.size() < 10 ? channelKeyList.size() : 10
        for (int i = 0; i < size; i++) {
            def obj = channelKeyList.get(i)
            if (resultMap.containsKey(obj)) {
                continue
            } else {
                resultMap.put(obj, siteWords.get(obj))
            }
        }
        resultMap.each {
            result << ['word': it.key, 'count': it.value]
        }
        return result
    }

    Map addNewsFavoriteByIds(Long userId, String agentId, String orgId, List newsIds, String groupId) {
        //判断用户是否已经全部收藏选中的新闻，返回未经收藏的newsIds
        def noneFavoritedNewsIds = newsRepo.getNoneFavoritedNewsIds(userId, newsIds)
        if (noneFavoritedNewsIds.size() == 0) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请不要重复收藏")
        }
        FavoriteGroup group = favoriteGroupRepo.getGroupById(groupId)
        if (!group) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "分组不存在！")
        }
        //获取新闻列表
        def newsListMap = newsRepo.getNewsListByIds(noneFavoritedNewsIds, 1)
        if (!newsListMap) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "获取新闻信息失败")
        }

        List newsList = newsListMap.newsList

        if (newsList.size() == 0) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "没有找到相关的新闻信息")
        }
        def now = new Date()
        List<NewsOperation> newsOperationList = []
        newsList.each {
            def newsOperation = new NewsOperation()
            newsOperation._id = UUID.randomUUID().toString()
            newsOperation.newsId = it.id
            newsOperation.agentId = agentId
            newsOperation.orgId = orgId
            newsOperation.userId = userId
            newsOperation.updateTime = now
            newsOperation.createTime = now
            newsOperation.news = it
            newsOperation.operationType = 3
            newsOperation.groupId = groupId
            newsOperationList << newsOperation
        }
        newsRepo.addNewsOperationList(newsOperationList)

        return apiResult(status: HttpStatus.SC_OK, msg: "成功收藏" + newsList.size() + "条新闻，失败" + newsListMap.noExistNewsIds.size() + "条")

    }

    Map getUserNewsFavorites(Long userId, int pageSize, int pageNo) {
        List result = newsRepo.getUserNewsFavorites(userId, pageSize, pageNo)
        result?.each {
            def list = []
            JSONObject.parse(it.news.keywords)?.each {
                list.add(it.word)
            }
            it.news.keywords = list
            it.news.content = ""
//            it.news.content = StringUtils.Text2Html(it.news.content)
        }
        return apiResult(status: HttpStatus.SC_OK, msg: result)
    }

    Map getUserNewsFavorite(Long userId, String id) {
        def result = newsRepo.getUserNewsFavorite(userId, id)
        if (null == result) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "没有找到相关的收藏信息")
        }
        return apiResult(status: HttpStatus.SC_OK, msg: result)
    }

    Map getUserNewsOperation(Long userId, String id) {
        def result = newsRepo.getUserNewsOperation(userId, id)
        if (null == result) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "没有找到相关的收藏信息")
        } else {
            def list = []
            def keywords = result.news.keywords
            if (result.pushType == 1) {
                try {
                    JSONObject.parse(result.news.keywords)?.each {
                        list.add(it.word)
                    }
                } catch (Exception e) {
                    log.error("关键词转换错误" + result.news.keywords, e)
                }
            } else if (result.pushType == 4) {
                try {
                    def keywordsList = keywords.split(" ")
                    keywordsList.each {
                        list.add(it)
                    }
                } catch (Exception e) {
                    log.error("关键词转换错误" + result.news.keywords, e)
                }
            }
            result.news.keywords = list
            StringBuilder imgUrls = new StringBuilder()
            String[] imgs = result.news.imgUrls
            if (imgs) {
                for (int i = 0; i < imgs.size(); i++) {
                    imgUrls.append("<p><img style='max-width:100%;' src='" + imgs[i] + "'/></p>")
                }
            }
            //todo 如果新闻newsType是6，微信，去除所有的新闻格式
            if (result.news.newsType == 6) {
                if (result.news.content.startsWith("<div class=\"rich_media_content \"")) {
                    result.news.content = WechatHtmlUtils.removeWechatHtmlFormat(result.news.content)
                }
            } else {
                if (!result.news.isHtmlContent) {//原文不带样式
                    result.news.content = StringUtils.ContentText2Html(result.news.content) + imgUrls
                    result.news.imgUrls = []
                } else {//原文带样式的去掉样式
                    result.news.content = HtmlUtil.removeWebHtmlFormat(result.news.content)
                }
            }
            if (result.news.content) {
                result.news.content = StringUtils.removeSpaceCode(result.news.content);//去掉多余空格特殊字符、首行缩进的空格
            }
        }
        return apiResult(status: HttpStatus.SC_OK, msg: result)
    }

    Map removeUserNewsOperation(long userId, String id) {
        boolean result = newsRepo.removeUserNewsOperation(userId, id)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: "删除成功")
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "删除失败")
        }
    }

    Map removeUserNewsOperationList(long userId, List ids) {
        boolean result = newsRepo.removeUserNewsOperationList(userId, ids)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: "删除成功")
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "删除失败")
        }
    }

    Map addNewsPushByIds(Long userId, String agentId, String orgId, String teamId, List newsIds) {
        //判断用户的是否已经存在推送的信息
        def existNewsFavorite = newsRepo.getExistNewsPush(userId, newsIds)
        if (existNewsFavorite.size() > 0) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请不要重复推送！")
        }
        //获取新闻列表
        def newsListMap = newsRepo.getNewsListByIds(newsIds, 1)
        if (!newsListMap) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "获取新闻信息失败")
        }

        List newsList = newsListMap.newsList

        if (newsList.size() == 0) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "没有找到相关的新闻信息")
        }
        def now = new Date()
        def newsOperationList = []
        Account account = accountRepo.getUserById(userId)
        def team = teamRepo.getTeam(teamId)
        newsList.each {
            def newsOperation = new NewsOperation()
            newsOperation._id = UUID.randomUUID().toString()
            newsOperation.newsId = it.id
            newsOperation.agentId = agentId
            newsOperation.orgId = orgId
            newsOperation.teamId = teamId
            newsOperation.userId = userId
            newsOperation.publisher = account?.realName ?: account.userName
            newsOperation.teamName = team ? team.teamName : ""
            newsOperation.updateTime = now
            newsOperation.createTime = now
            if (it.isHtmlContent) {
                //todo 如果采集到了有htmlContent的数据，理论上是不应该包含\n\t\r的数据的，把这类数据替换为''
                it.content = StringUtils.illegalText2Html(it.content)
            } else {
                //不带样式htmlContent的数据，存储前需要去做一些样式的处理
                it.content = StringUtils.ContentText2Html(it.content)
            }
            newsOperation.news = it
            newsOperation.pushType = 1
            newsOperation.status = 1
            newsOperation.operationType = 1
            newsOperation.isAutoPush = false
            newsOperationList << newsOperation
        }
        newsRepo.addNewsOperationList(newsOperationList)

        return apiResult(status: HttpStatus.SC_OK, msg: "成功推送" + newsOperationList.size() + "条新闻，失败" + newsListMap.noExistNewsIds.size() + "条")
    }

    List getNewsPushHistory(long userId, String orgId, String teamId, long pushUser, String pushChannel, String autoPushType, Date startDate, Date endDate, int pageNo, int pageSize) {
//        pushUser记录了userId
        def list = newsRepo.getNewsPushHistory(userId, orgId, teamId, pushUser, pushChannel, autoPushType, startDate, endDate, pageNo, pageSize)
        def result = []
        list.each {
            def news = [:]
            if (!it.news.publishTime) {
                it.news.publishTime = it.news.createTime
            }
            if (it.news.publishTime && String.valueOf(it.news.publishTime).contains("T")) {
                it.news.publishTime = DateUitl.convertEsDate(it.news.publishTime)?.getTime()
            }
            news._id = it._id ?: ""
            news.userId = it.userId ?: ""
            news.realName = accountRepo.getUserById(it.userId)?.realName
            news.userName = accountRepo.getUserById(it.userId)?.userName
            news.title = it.news.title ?: ""
            news.contentAbstract = it.news.contentAbstract ?: ""
            news.siteName = it.news.siteName ?: ""
            news.reprintCount = it.news.reprintCount ?: ""
            news.publishTime = it.news.publishTime ?: 0L
            news.createTime = it.createTime ?: 0L
            news.status = it.status ?: 1

            result << news
        }
        return result
    }

    Map getNewsPushHistoryCount(long userId, String orgId, String teamId) {
        long teamPushCount = 0
        long userPushCount = 0
        def startDate = DateUitl.getBeginDayOfParm(new Date())
        def endDate = new Date()
        userPushCount = newsRepo.getUserNewsPushHistoryCount(userId, startDate, endDate)
        if ((!"0".equals(teamId)) && (!"0".equals(orgId))) {
            teamPushCount = newsRepo.getTeamNewsPushHistoryCount(userId, orgId, teamId, startDate, endDate)
            return [teamPushCount: teamPushCount, userPushCount: userPushCount]
        }
        return [userPushCount: userPushCount]
    }

    long getNewsPushHistoryCount(long userId, String orgId, String teamId, long pushUser, String pushChannel, String autoPushType, Date startDate, Date endDate) {
        long count = 0
        count = newsRepo.getNewsPushHistoryCount(userId, orgId, teamId, pushUser, pushChannel, autoPushType, startDate, endDate)
        return count
    }

    List favouriteCount() {
        List<Account> validUsers = accountRepo.getValidUsers();
        List mapList = new ArrayList()
        for (int i = 0; i < validUsers.size(); i++) {
            Map map = new HashMap()
            Account account = validUsers.get(i)
            String realName = account.getRealName();
            String company = account.getCompany();
            long count = newsRepo.favouriteCount(account.getId())
            map.put("userId", account.getId())
            map.put("userName", account.getUserName())
            map.put("realName", realName);
            map.put("company", company);
            map.put("count", count)
            mapList.add(map)
        }
        return mapList;
    }

    void excelOutFavouriteCount(List favouriteList) {
        String outfileName = UUID.randomUUID();
        ExportExcel ex = new ExportExcel();
        String sheetName = "我的收藏数量";//下载文件的默认名字,sheet页名字
        String fileName = outfileName
        String headers = "userId,userName,realName,company,count";
//表头
        String selname = "userId,userName,realName,company,count,o"
//标题对应key值

        HSSFWorkbook wb = ex.exportExcel(sheetName, headers, favouriteList, selname);
//返回一个workbook即excel实例。datetime是sheet页的名称
        String excelFolder = "D:\\" + sheetName;
        def result = [
                status: HttpStatus.SC_OK,
                msg   : '',
        ];
        //如果文件夹不存在则创建，如果已经存在则删除
        File outPath = new File(excelFolder)
        if (!outPath.exists()) {
            FileUtils.forceMkdir(outPath)
        } else {
            FileUtils.forceDelete(outPath)
            FileUtils.forceMkdir(outPath)
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            wb.write(os);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        byte[] content = os.toByteArray();
        OutputStream fos = null;
        File file = new File(excelFolder + "/" + "excel.xls")

        try {
            fos = new FileOutputStream(file);
            fos.write(content);
            os.close();
            fos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        println("默认生成目录在 D:\\" + sheetName);
    }


    def modifyNewsGroupByGroupId(String groupId, String newsOperationId) {
        def result = newsRepo.modifyNewsGroupByGroupId(groupId, newsOperationId)
    }

    List getExistNewsOperation(long userId, List newsIds, int operationType) {
        return newsRepo.getExistNewsOperation(userId, newsIds, operationType)
    }

    Map getNewsByNewsIdAndUser(String newsId, long userId, int style, boolean original) {
        Map newsMap = getNewsById(newsId, style, original)
        if (newsMap.status == 200) {
            def news = newsMap.msg
            if (userId) {
                def newsIds = []
                newsIds.add(newsId)
                List col = newsRepo.getExistNewsOperation(userId, newsIds, 3)
                List push = newsRepo.getExistNewsOperation(userId, newsIds, 1)
                news.put("isCollection", col.contains(newsId))
                news.put("isPush", push.contains(newsId))
                news.put("isMarked", instantSiteRepo.isNewsMarked(userId, newsId))
            }
        }
        return newsMap


    }

    /**
     * 只是删除收藏的
     * @param userId
     * @param newsId
     * @return
     */
    Map removeNewsOperationByNewsIdAndType(long userId, String newsId, int operationType) {
        boolean result = newsRepo.removeNewsOperationByNewsId(userId, newsId, operationType)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: "删除成功")
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "删除失败")
        }
    }

    List<News> getNewsRoundup(List<String> newsIds, Integer roundupSize) {
        List<News> resultList = new ArrayList<>();
        for (String newsId : newsIds) {
            try {
                NewsOperation newsOperation = newsRepo.getUserNewsOperation(0L, newsId)
                if (!NewsOperation) {
                    log.info("新闻摘要获取失败，未查询到相关新闻，newsOperationId:{}", newsId)
                    continue
                }
                Map newsMap = newsOperation.news
                String newsRoundup = getNewsRoundup(newsMap, roundupSize)
                if (newsRoundup) {
                    newsMap.createTime = null
                    newsMap.content = null
                    News news = News.toObject(newsMap)
                    news.setContentAbstract(newsRoundup)
                    news.setNewsId(newsId)
                    resultList << news
                } else {
                    log.info("新闻摘要获取失败，未获取到新闻相关摘要，newsOperationId:{}", newsId)
                }
            } catch (Exception e) {
                log.info("新闻摘要获取失败，newsOperationId:{},Exception:{} ", newsId, e)
                e.printStackTrace()
            }
        }
        resultList
    }

    String getNewsRoundup(Map news, Integer roundupSize) throws Exception {
        Map requestParm = new HashMap()
        requestParm.put("text", StringUtils.html2text(news.get("content")))
        requestParm.put("n", roundupSize)
        //todo 删除
        def begin = System.currentTimeMillis()
        HttpResponse response = Unirest.post("http://124.205.18.190/cb/api/zhxg/abstract")
                .fields(requestParm)
                .asJson()
        log.info("获取文章摘要接口调用耗时{}", System.currentTimeMillis() - begin);
        if (response.getStatus() == 200) {
            org.json.JSONObject resultJson = response.body.object
            org.json.JSONArray jsonArray = resultJson.get("content")
            StringBuffer content = new StringBuffer("")
            jsonArray.each { sentence ->
                content = content.append(sentence)
            }
            content.toString()
        }
    }

    Map getNewsViewpoint(List<String> newsIds) {
        Map resultMap = [:]
        List<News> newsList = new ArrayList<>();
        int viewpointCount = 0
        for (String newsId : newsIds) {
            try {
                NewsOperation newsOperation = newsRepo.getUserNewsOperation(0L, newsId)
                if (!NewsOperation) {
                    viewpointCount++
                    log.info("新闻摘要获取失败，未查询到相关新闻，newsOperationId:{}", newsId)
                    continue
                }
                Map newsMap = newsOperation.news
                List<Map> newsViewpoint = getNewsViewpoint(newsMap)
                if (newsViewpoint.size() > 0) {
                    newsMap.createTime = null
                    newsMap.content = null
                    News news = News.toObject(newsMap)
                    news.setViewpoint(newsViewpoint)
                    newsList.add(news)
                } else {
                    viewpointCount++
                    log.info("新闻摘要获取失败，未获取到新闻相关观点，newsOperationId:{}", newsId)
                }
            } catch (Exception e) {
                viewpointCount++
                log.info("新闻观点失败，newsOperationId:{},Exception:{} ", newsId, e)
                e.printStackTrace()
            }
        }
        if (viewpointCount == newsIds.size()) {
            resultMap.put("noViewpoint", true)
        }
        resultMap.put("list", newsList)
        resultMap
    }

    List<Map> getNewsViewpoint(Map news) throws Exception {
        Map requestParm = new HashMap()
        List<Map> resultList = new ArrayList<>();
        requestParm.put("text", StringUtils.html2text(news.get("content")))
        //todo 删除
        def begin = System.currentTimeMillis()
        HttpResponse response = Unirest.post("http://124.205.18.190/cb/api/zhxg/viewpoint")
                .fields(requestParm)
                .asJson()
        log.info("获取文章观点接口调用耗时{}", System.currentTimeMillis() - begin);
        if (response.getStatus() == 200) {
            org.json.JSONObject resultJson = response.body.object
            org.json.JSONArray jsonArray = resultJson.get("content")
            jsonArray.each { viewpoint ->
                Map viewpointMap = new HashMap();
                viewpointMap.put("name", viewpoint.getAt("name"))
                viewpointMap.put("importance", viewpoint.getAt("importance"))
                viewpointMap.put("viewpoints", viewpoint.getAt("viewpoints"))
                resultList.add(viewpointMap)
            }
        }
        return resultList
    }
}

package com.istar.mediabroken.service

import com.alibaba.fastjson.JSONArray
import com.istar.mediabroken.Const
import com.istar.mediabroken.api3rd.TopicApi3rd
import com.istar.mediabroken.entity.*
import com.istar.mediabroken.entity.capture.NewsOperation
import com.istar.mediabroken.repo.account.AccountRepo
import com.istar.mediabroken.repo.CaptureRepo
import com.istar.mediabroken.repo.ICompileRepo
import com.istar.mediabroken.utils.*
import com.istar.mediabroken.utils.WordHtml.RichHtmlHandler
import com.istar.mediabroken.utils.WordHtml.WordGeneratorWithFreemarker
import com.istar.mediabroken.utils.wordseg.WordSegUtil
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sun.misc.BASE64Decoder

import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat

import static com.istar.mediabroken.api.ApiResult.apiResult
import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR

@Service
@Slf4j
class ICompileService {
    @Autowired
    AccountRepo accountRepo

    @Autowired
    TopicApi3rd topicApi

    @Autowired
    ICompileRepo iCompileRepo

    @Autowired
    CaptureRepo captureRepo

    @Autowired
    CaptureService captureSrv

    @Value('${image.upload.path}')
    String UPLOAD_PATH

    @Value('${env}')
    String env

    List<News> getRelatedNews(ICompileSummary summary, int limit) {
        return topicApi.getNewsList(summary, 1, limit)
    }

    ICompileSummary getSummary(long userId, String summaryId) {
        def summary = iCompileRepo.getSummary(userId, summaryId)
        summary.session.sid = accountRepo.getYqmsSession(summary.session.userId)
        return summary
    }

    ICompileSummary getSummary(long userId) {
        def summary = iCompileRepo.getSummary(userId)


        // ?????????,??????,??????,??????????????????,???????????????????????????,???title??????
        if (!(summary.time || summary.place || summary.person || summary.event)) {
            summary.event = summary.title
            iCompileRepo.modifySummary(summary)
        }
        return summary
    }

    Map getSprendTrend(ICompileSummary summary) {
        return topicApi.getSprendTrend(summary)
    }

    List<News> getLatestWei(ICompileSummary summary, int limit) {
        def list = topicApi.getWeiboList(summary)
        def result = []
        for (int i = 0; i < list.size() && i < limit; i++) {
            result << list[list.size() - i - 1]
        }
        return result
    }

    Map getTopN(ICompileSummary summary) {
        return topicApi.getTopN(summary)
    }

    Map getEventEvolution(ICompileSummary summary) {
        return topicApi.getEventEvolution(summary)
    }

    Map getIntro(ICompileSummary summary) {
        def rep = [:]
        rep.title = summary.title

        rep.startTime = summary.startTime
        rep.endTime = summary.endTime

        def eventEvolution = topicApi.getEventEvolution(summary)
        println eventEvolution.firstPublish
        if (eventEvolution.firstPublish) {
            eventEvolution.firstPublish.sort { News a, News b ->
                return a.createTime.getTime() - b.createTime.getTime()
            }
            println eventEvolution.firstPublish
            rep.firstPublishSite = eventEvolution.firstPublish[0].site
            rep.firstPublishTime = eventEvolution.firstPublish[0].createTime
        } else {
            rep.firstPublishSite = ""
            rep.firstPublishTime = summary.endTime
        }

        def spreadTrend = topicApi.getSprendTrend(summary)
        rep.totalNews = spreadTrend.totalNews

        def maxCount = -1
        def maxDate = null
        spreadTrend.list.each {
            if (it.news > maxCount) {
                maxCount = it.news
                maxDate = it.time
            }
        }
        rep.maxCount = maxCount
        rep.maxDate = maxDate

        rep.topSites = []
        def topN = topicApi.getTopN(summary)
        for (int i = 0; i < topN.topSites.size() && i < 5; i++) {
            def it = topN.topSites[i]
            rep.topSites << it.site
        }

        return rep
    }

    def todayNewsPush(long userId, String orgId) {
        Date createTime = new DateTime().minusHours(48).toDate()
        List pushList = captureRepo.getNewsPushList(userId, orgId, createTime, 100)

        // ????????????,??????????????????
//        for  ( int  i  =   0 ; i  <  pushList.size()  -   1 ; i ++ )  {
//            for  ( int  j  =  pushList.size()  -   1 ; j  >  i; j -- )  {
//                if  (pushList.get(j).title.equals(pushList.get(i).title))  {
//                    pushList.remove(j);
//                }
//            }
//        }

        // ???????????????????????????????????????
        def pool = new HashSet()
        Iterator<NewsPush> it = pushList.iterator();
        while(it.hasNext()){
            def newsPush = it.next();
            if(pool.contains(newsPush.title)){
                it.remove();
            } else {
                pool.add(newsPush.title)
            }
        }

        return pushList
    }

    AbstractSetting getAbstractSetting(long userId) {
        iCompileRepo.getAbstractSetting(userId)
    }

    def setAbstractSetting(AbstractSetting setting){
        iCompileRepo.setAbstractSetting(setting)
    }
    Map uploadImgFromPc(Long userId, HttpServletRequest request, String UPLOAD_PATH, String type){
        def res = UploadUtil.uploadImg(request, UPLOAD_PATH, type)
        return apiResult(res)
    }
    def getAbstractImgsById(long userId,String abstract_id){
        //???????????????imgs?????????
        def imgs=iCompileRepo.getAbstractImgsById(userId,abstract_id)
        return imgs;
    }

    Map createNewsAbstract(long userId, String orgId, String newsOperationIds) {
        List<String> newsOperationIdList = newsOperationIds.split(",")
        if(!newsOperationIdList)
            return apiResult(SC_INTERNAL_SERVER_ERROR, '????????????????????????')

        //????????????????????????????????????????????????
        AbstractSetting setting = iCompileRepo.getAbstractSetting(userId)
        if(setting){
            int newsCount = setting.newsCount
            if(newsCount <= 0)
                return apiResult(SC_INTERNAL_SERVER_ERROR, '???????????????????????????????????????')

            if(newsOperationIdList.size() > newsCount)
                return apiResult(SC_INTERNAL_SERVER_ERROR, '???????????????????????????????????????????????????')
        }
        List<News> newsList = captureSrv.getNewsListByOperationIds(newsOperationIdList)
        if(!newsList){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '?????????????????????????????????')
        }
        //????????????????????????
        NewsAbstract newsAbstract = new NewsAbstract()
        newsAbstract.userId = userId
        newsAbstract.orgId = orgId
        newsAbstract.author = this.dealAbstractAuthor(userId, setting)
        newsAbstract.abstractId = newsAbstract.createId()
        newsAbstract.title = this.dealAbstractTitle(newsList, setting)
        //content
        String content = ''
        String contentAbstract = ''
        def newsDetail = []
        def imgs = []
        newsList?.each {
            content += "<h2><a href='${it.url}' target='_blank'>${it.title}</a></h2>\n"
            content += "<p>${it.contentAbstract}</p>\n"
            contentAbstract += "${it.title}\n"
            newsDetail << [
                    newsId  : it.newsId,
                    title   : it.title,
                    contentAbstract : it.contentAbstract
            ]
            def currImg = StringUtils.extractImgUrl(it.content);
            if (currImg){
                imgs += currImg
            }
        }
        newsAbstract.content = content
        newsAbstract.contentAbstract = contentAbstract
        newsAbstract.newsDetail = newsDetail
        newsAbstract.imgs = imgs && imgs.size() > 1 ? imgs.unique() : imgs;
        newsAbstract.picUrl = this.dealAbstractPic(newsAbstract.imgs, setting)
        Date now = new Date()
        newsAbstract.createTime = now
        newsAbstract.updateTime = now
        newsAbstract.type = 2
        iCompileRepo.addNewsAbstract(newsAbstract)
        return apiResult([id : newsAbstract.abstractId]);
    }

    Map modifyNewsAbstract (LoginUser user,Map abstractMap){
        def newsAbstractTemp = new NewsAbstract()
        if(abstractMap.containsKey("abstractId") && (abstractMap.abstractId)){
            newsAbstractTemp = iCompileRepo.getNewsAbstract(user.userId, abstractMap.abstractId);
            if(!newsAbstractTemp){
                return apiResult(SC_INTERNAL_SERVER_ERROR, '????????????????????????????????????????????????')
            }
        }else {
            //?????????????????????
            newsAbstractTemp.userId = user.userId
            newsAbstractTemp.orgId = user.orgId
            newsAbstractTemp.abstractId = newsAbstractTemp.createId()
            newsAbstractTemp.createTime = new Date()
        }
        if(abstractMap.containsKey("title") && !(abstractMap.title.equals(""))){
            newsAbstractTemp.title = abstractMap.title
        } else {
            return apiResult(SC_INTERNAL_SERVER_ERROR, '?????????????????????')
        }
        if(abstractMap.containsKey("author")){ newsAbstractTemp.author = abstractMap.author }
        if(abstractMap.containsKey("picUrl")){ newsAbstractTemp.picUrl = abstractMap.picUrl }
        if(abstractMap.containsKey("content")){ newsAbstractTemp.content = abstractMap.content }

        if(abstractMap.containsKey("contentAbstract")){ newsAbstractTemp.contentAbstract = abstractMap.contentAbstract }
        if(abstractMap.containsKey("keyWords") && !(abstractMap.keyWords.equals(""))){
            newsAbstractTemp.keyWords = abstractMap.keyWords
        }else {
            return apiResult(SC_INTERNAL_SERVER_ERROR, '??????????????????')
        }
        if(abstractMap.containsKey("classification") && !(abstractMap.classification.equals(""))){
            newsAbstractTemp.classification = abstractMap.classification
        } else {
            return apiResult(SC_INTERNAL_SERVER_ERROR, '?????????????????????')
        }

        if(abstractMap.containsKey("source")){ newsAbstractTemp.source = abstractMap.source }
        if(abstractMap.containsKey("originalAuthor")){ newsAbstractTemp.originalAuthor = abstractMap.originalAuthor }
        if(abstractMap.containsKey("firstPublishSite")){ newsAbstractTemp.firstPublishSite = abstractMap.firstPublishSite }
        if(abstractMap.containsKey("firstPublishTime") && (abstractMap.firstPublishTime)){
            def sdf = new SimpleDateFormat('yyyy/MM/dd HH:mm')
            def publishTime = null
            try{
                publishTime = sdf.parse(abstractMap.firstPublishTime)
            }catch(Exception e){
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????'yyyy/MM/dd HH:mm'??????????????????")
            }
            newsAbstractTemp.firstPublishTime = publishTime
        }
        if(abstractMap.containsKey("originalUrl")){ newsAbstractTemp.originalUrl = abstractMap.originalUrl }
        if(abstractMap.containsKey("type")){
            newsAbstractTemp.type = abstractMap.type
        }else {
            newsAbstractTemp.type = 3
        }
        newsAbstractTemp.updateTime = new Date()
        iCompileRepo.modifyNewsAbstract(newsAbstractTemp)
        return apiResult([abstractId: newsAbstractTemp.abstractId])

    }

    Map removeNewsAbstract (long userId,String abstractId){
        def newsAbstractTemp = iCompileRepo.getNewsAbstract(userId,abstractId);
        if(!newsAbstractTemp){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '????????????????????????????????????????????????')
        }
        iCompileRepo.removeNewsAbstract(userId,abstractId)
        return apiResult();
    }

    private String dealAbstractAuthor(long userId, AbstractSetting setting) {
        String author = ''
        if(setting) {
            author = setting.author
        }
        if(author){
            return author
        }
        def user = accountRepo.getUserById(userId)
        if(user) {
            author  = user.nickName ?: user.userName
        }
        return author
    }

    private String dealAbstractTitle(List<News> newsList, AbstractSetting setting) {

        if(!setting) {
            setting = new AbstractSetting()
        }

        String title = ''
        int maxTitleNum = 4
        newsList.eachWithIndex { it, i ->
            if(title.concat(';').concat(it.title).length() <= setting.titleLength && i < maxTitleNum){
                title += (it.title + ';')
            } else {
                return
            }
        }
        if(!title){
            title = newsList.get(0).title.substring(0, setting.titleLength);
        }
        return title
    }

    private String dealAbstractPic(List picList, AbstractSetting setting) {

        String pic = ''
        if(setting && !setting.showThumbnail){
            return pic
        }
        //????????????????????????????????????
        try {
            InputStream is = null
            BufferedImage img = null
            for (int i = 0; i < picList.size(); i++) {
                is = new URL(picList.get(i)).openStream()
                img = ImageIO.read(is)
                if(img.width > 300 && img.height > 200){
                    pic = picList.get(i)
                    break
                }
            }
        }catch (Exception e) {
            pic = picList?.get(0)
        }
        //????????????????????????????????????????????????????????????????????????
//        pic = 'http://img.hb.aicdn.com/bf05e750f448d119d295b14eda770f8fc7ed41cd27067-OGSYpG_fw658'
        return pic
    }

    NewsAbstract getNewsAbstract(long userId, String abstractId) {
        return iCompileRepo.getNewsAbstract(userId, abstractId)
    }

    NewsAbstract getNewsAbstractById(String abstractId) {
        return iCompileRepo.getNewsAbstractById(abstractId)
    }

    /**
     * ????????????????????????
     * @param abstractId
     * @return
     */
    Map getNewsAbstractById4Material(String abstractId) {
        NewsAbstract newsAbstract = iCompileRepo.getNewsAbstractById(abstractId)
        if(null == newsAbstract){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR,msg: "???????????????????????????"])
        }
        newsAbstract.imgs = newsAbstract.imgs && newsAbstract.imgs.size() > 1? newsAbstract.imgs.unique():newsAbstract.imgs
        return apiResult([detail: newsAbstract])
    }

    NewsAbstract getTodayNewsAbstract(long userId) {
        return iCompileRepo.getTodayNewsAbstract(userId)
    }

    Paging<NewsAbstract> getPagingAbstractList(long userId, String orgId,int pageNo,int limit ) {
        long total = iCompileRepo.getAbstractTotal(userId, orgId)
        def paging = new Paging<NewsAbstract>(pageNo, limit, total)
        iCompileRepo.getPagingAbstractList(userId, orgId, paging)
        return paging
    }

    def createSummaryPush(long userId, String orgId, String summaryId) {
        ICompileSummary summary = getSummary(userId, summaryId)
        Date now = new Date()
        return captureRepo.addSummaryPush([
                summaryId   : summaryId,
                data        : summary?.data,
                title       : summary?.title,
                source      : PushTypeEnum.SUMMARY_PUSH.value,
                pushType    : PushTypeEnum.SUMMARY_PUSH.index,
                orgId       : orgId,
                userId      : userId,
                status      : Const.PUSH_STATUS_NOT_PUSH,
                createTime  : now,
                updateTime  : now
        ])
    }

    Paging<SummaryPush> getPagingSummaryPushList(long userId, String orgId, int status, int pageNo, int limit) {
        long total = captureRepo.getSummaryPushTotal(userId, orgId, status)
        def paging = new Paging<SummaryPush>(pageNo, limit, total)
        captureRepo.getSummaryPushList(userId, orgId, status, paging)
        return paging
    }

    def createAbstractPush(long userId, String orgId, String abstractId) {
        NewsAbstract newsAbstract = getNewsAbstract(userId, abstractId)
        Date now = new Date()
        return captureRepo.addAbstractPush([
                abstractId  : abstractId,
                title       : newsAbstract?.title,
                source      : PushTypeEnum.ABSTRACT_PUSH.value,
                picUrl      : newsAbstract?.picUrl,
                newsDetail  : newsAbstract?.newsDetail,
                pushType    : PushTypeEnum.ABSTRACT_PUSH.index,
                orgId       : orgId,
                userId      : userId,
                status      : Const.PUSH_STATUS_NOT_PUSH,
                content     : newsAbstract?.content,
                newsAbstract: newsAbstract.toMap(),
                createTime  : now,
                updateTime  : now
        ])
    }

    Paging<AbstractPush> getPagingAbstractPushList(long userId, String orgId, int status, int pageNo, int limit) {
        long total = captureRepo.getAbstractPushTotal(userId, orgId, status)
        def paging = new Paging<AbstractPush>(pageNo, limit, total)
        captureRepo.getAbstractPushList(userId, orgId, status, paging)
        return paging
    }

    ICompileSummary querySummary(ICompileSummary summary) {
        def oldSummary = iCompileRepo.getSummary(summary.userId)

        if (oldSummary) {
            def currentTime = new Date()

            summary.summaryId = oldSummary.summaryId
            summary.yqmsUserId = oldSummary.yqmsUserId
            summary.yqmsTopicId = oldSummary.yqmsTopicId
            summary.updateTime = currentTime
            def session = accountRepo.getYqmsSession2(summary.yqmsUserId)

            topicApi.removeTopic(session, summary.yqmsTopicId)
            def topicId = topicApi.addTopic(session,
                    summary.title,
                    summary.keywords,
                    summary.ambiguous,
                    summary.startTime,
                    summary.endTime
            )?.topicId

            summary.yqmsTopicId = topicId
            iCompileRepo.modifySummary(summary)
        } else {
            // ?????????,?????????
            def currentTime = new Date()
            summary.summaryId = UUID.randomUUID().toString()
            summary.createTime = currentTime
            summary.updateTime = currentTime

            def yqmsUserId = iCompileRepo.getLeastUsedUserIdForTopic()
            summary.yqmsUserId = yqmsUserId
            def session = accountRepo.getYqmsSession2(summary.yqmsUserId)
            def topicId = topicApi.addTopic(session,
                    summary.title,
                    summary.keywords,
                    summary.ambiguous,
                    summary.startTime,
                    summary.endTime
            )?.topicId

            summary.yqmsTopicId = topicId
            iCompileRepo.addSummary(summary)
        }

        return summary
    }

    /**
     * ????????????????????????
     * @param userId
     * @return
     */
    List<ICompileSummary> getSummariesByUserId(long userId) {
        def summaries = iCompileRepo.getSummariesByUserId(userId)
        return summaries
    }

    /**
     * ????????????????????????
     * @param userId
     * @return
     */
    Paging<NewsAbstract> getNewsAbstracts(long userId, int pageNo, int limit,String queryKeyWords,int orderType) {
        def newsAbstractsTotal = iCompileRepo.getNewsAbstractsTotal(userId,queryKeyWords);

        def paging = new Paging<NewsAbstract>(pageNo, limit, newsAbstractsTotal)
        iCompileRepo.getNewsAbstracts(userId, paging,queryKeyWords,orderType)
        return paging
    }
    /**
     * ????????????????????????
     * @param userId
     * @return
     */
    Paging<NewsOperation> getNewsOperations(long userId, int pageNo, int limit, String queryKeyWords, int orderType, int operationSource, int timeType, String timeStart, String timeEnd) {
        def newsOperationsTotal = iCompileRepo.getNewsOperationsTotal(userId,queryKeyWords,operationSource,timeType,timeStart,timeEnd);

        def paging = new Paging<NewsOperation>(pageNo, limit, newsOperationsTotal)
        iCompileRepo.getNewsOperations(userId, paging,queryKeyWords,orderType,operationSource,timeType,timeStart,timeEnd)

        // ?????????????????????
        def pool = new HashSet()
        Iterator<NewsOperation> it = paging.list.iterator()
        while(it.hasNext()){
            def newsOperation = it.next();
            if(pool.contains(newsOperation.news.title)){
                it.remove();
                log.debug('??????????????????{}', newsOperation.news.title)
            } else {
                pool.add(newsOperation.news.title)
            }
        }

        return paging
    }

    /**
     * ????????????????????????
     * @param summary
     * @return
     */
    Map addSummary(ICompileSummary summary) {
        if(!(summary.isEnoughInfo())){
            return apiResult(SC_INTERNAL_SERVER_ERROR, "???????????????3????????????")
        }
        //?????????????????????sumnary?????????????????????5?????????????????????
        int userSummaryCount = iCompileRepo.getUserSummaryCount(summary.userId)
        if(userSummaryCount >= 5){
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????5???????????????")
        }

        def currentTime = new Date()
        summary.summaryId = UUID.randomUUID().toString()
        summary.createTime = currentTime
        summary.updateTime = currentTime

        def yqmsUserId = iCompileRepo.getLeastUsedUserIdForTopic()
        summary.yqmsUserId = yqmsUserId
        def session = accountRepo.getYqmsSession2(summary.yqmsUserId)
        def topicId = topicApi.addTopic(session,
                summary.title,
                summary.keywords,
                summary.ambiguous,
                summary.startTime,
                summary.endTime
        )?.topicId

        summary.yqmsTopicId = topicId

        def defaultTemplate = getSummaryDefaultTemplate(summary.userId)
        summary.template = defaultTemplate
        iCompileRepo.addSummary(summary)

        return apiResult()
    }

    /**
     * ????????????????????????
     * @param summary
     * @return
     */
    Map removeSummaryById(long userId,String summaryId) {
        def summary = iCompileRepo.getSummaryById(summaryId)
        if(userId != summary.userId){
            return apiResult(SC_BAD_REQUEST, "?????????????????????????????????")
        }
        if(summary){
            def session = accountRepo.getYqmsSession2(summary.yqmsUserId)
            topicApi.removeTopic(session, summary.yqmsTopicId)
            iCompileRepo.removeSummaryById(summaryId)
        }
        return apiResult()
    }

    /**
     * ??????????????????
     * @param id
     * @return
     */
    ICompileSummary getSummaryById(id){
        return iCompileRepo.getSummaryById(id)
    }

    /**
     * ??????????????????
     * @param summary
     * @return
     */
    Map modifySummary(ICompileSummary summary) {
        def oldSummary = iCompileRepo.getSummaryById(summary.summaryId)
        if(summary.userId != oldSummary.userId){
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if(!oldSummary){
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        if(summary.equals(oldSummary)){
            return apiResult()
        }
        if(!(summary.isEnoughInfo())){
            return apiResult(SC_INTERNAL_SERVER_ERROR, "???????????????3????????????")
        }
        def currentTime = new Date()
        summary.summaryId = oldSummary.summaryId
        summary.yqmsUserId = oldSummary.yqmsUserId
        summary.yqmsTopicId = oldSummary.yqmsTopicId
        summary.updateTime = currentTime
        def session = accountRepo.getYqmsSession2(summary.yqmsUserId)
        topicApi.removeTopic(session, summary.yqmsTopicId)
        def topicId = topicApi.addTopic(session,
                summary.title,
                summary.keywords,
                summary.ambiguous,
                summary.startTime,
                summary.endTime
        )?.topicId
        summary.yqmsTopicId = topicId
        iCompileRepo.modifySummary(summary)
        return apiResult()
    }

    String getKeywords(String title) {
        return WordSegUtil.seg(title)
    }

    def getOpenSummaryPushList(String orgId){
        captureRepo.getOpenSummaryPushList(orgId)
    }

    def getOpenAbstractPushList(String orgId){
        captureRepo.getOpenAbstractPushList(orgId)
    }

    def updateSummaryPush2Pushed(String orgId, String summaryIds){
        captureRepo.modifySummaryPushStatus(orgId, summaryIds.tokenize(','), Const.PUSH_STATUS_PUSHED)
    }

    def updateAbstractPush2Pushed(String orgId, String abstractIds){
        captureRepo.modifyAbstractPushStatus(orgId, abstractIds.tokenize(','), Const.PUSH_STATUS_PUSHED)
    }


    List<News> getDiscussions(ICompileSummary summary, int limit) {
            return topicApi.getDiscussionList(summary, 1, limit)
    }

    List<News> getHotTopic(ICompileSummary summary) {
        return topicApi.getHotTopic(summary)
    }

    Map getWeiboAuthorRelations(ICompileSummary summary) {
        return topicApi.getWeiboAuthorRelations(summary)
    }
    Map getNewsAbstractWord(long userId,String abstractId){

        NewsAbstract newsAbstract = getNewsAbstract(userId, abstractId)
        if(!newsAbstract){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '??????????????????????????????')
        }
        Map<String, Object> data = new HashMap<String, Object>();
        String type = newsAbstract.type?newsAbstract.type.toString():"";
        data.put("type",type?:"");
        data.put("abstractId",newsAbstract.abstractId?:"")
        data.put("title",newsAbstract.title?:"")
        data.put("author",newsAbstract.author?:"")
        data.put("picUrl",newsAbstract.picUrl?:"")
        data.put("updateTime",newsAbstract.updateTime.toLocaleString()?:"")
        data.put("fileName",newsAbstract.abstractId?:"")
        data.fileType = 'downLoadNews'

        List<Map<String, Object>> newsList = new ArrayList<Map<String, Object>>();
        String contentStr=newsAbstract.content?:"";
        //???????????????????????????????????????????????????????????????
        String outfilePath = "/${UPLOAD_PATH}/download/${data.fileType as String}/${data.abstractId as String}";
        File outPath = new File(outfilePath)
        if(!outPath.exists()){
            FileUtils.forceMkdir(outPath)
        }else {
            FileUtils.forceDelete(outPath)
            FileUtils.forceMkdir(outPath)
        }
        //????????????
        String saveFile = null;
        if(!(null == data.get("picUrl")) && !data.get("picUrl").equals("")){
            if(data.get("picUrl").startsWith("http")){
                saveFile = HttpClientUtil.downLoadFromUrl(data.get("picUrl"),"0",outfilePath)
            }else {
                saveFile = DownloadUtils.downLoadFromPath(UPLOAD_PATH,data.get("picUrl"),"0",outfilePath)
            }
        }

        def imgList = StringUtils.extractImgUrl(contentStr)
        imgList?.eachWithIndex {it, i ->
            String wechatImg = "";
            if(it.startsWith("http")){
                wechatImg = HttpClientUtil.downLoadFromUrl(it,"img"+((i+1) as String),outfilePath)
            }else {
                wechatImg = DownloadUtils.downLoadFromPath(UPLOAD_PATH,it,"img"+((i+1) as String),outfilePath)
            }
            if(wechatImg){
                contentStr = contentStr.replaceAll(it as String, wechatImg)
            }
        }

        StringBuilder sb = new StringBuilder();

        String author = newsAbstract.author?:"";
        String updateTime=newsAbstract.updateTime.toLocaleString()?:""
        String title=newsAbstract.title?:""
        sb.append("<div>");
        sb.append("<h2>"+title+"</h2>");
        sb.append("<p>"+author+" "+updateTime+"</p>");
        if(!(null == data.get("picUrl")) && !data.get("picUrl").equals("")){
            sb.append("<img style='max-width:100%;' src='"+saveFile+"'/>");
        }
        sb.append("</div>");
        sb.append(contentStr);

        RichHtmlHandler handler = new RichHtmlHandler(sb.toString());

        handler.setDocSrcLocationPrex("file:///C:/8595226D");
        handler.setDocSrcParent("file3405.files");
        handler.setNextPartId("01D214BC.6A592540");
        handler.setShapeidPrex("_x56fe__x7247__x0020");
        handler.setSpidPrex("_x0000_i");
        handler.setTypeid("#_x0000_t75");

        handler.handledHtml(false);

        String bodyBlock = handler.getHandledDocBodyBlock();
        System.out.println("bodyBlock:\n"+bodyBlock);

        String handledBase64Block = "";
        if (handler.getDocBase64BlockResults() != null
                && handler.getDocBase64BlockResults().size() > 0) {
            for (String item : handler.getDocBase64BlockResults()) {
                handledBase64Block += item + "\n";
            }
        }
        data.put("imagesBase64String", handledBase64Block);

        String xmlimaHref = "";
        if (handler.getXmlImgRefs() != null
                && handler.getXmlImgRefs().size() > 0) {
            for (String item : handler.getXmlImgRefs()) {
                xmlimaHref += item + "\n";
            }
        }
        data.put("imagesXmlHrefString", xmlimaHref);
        data.put("content", bodyBlock);
        Map result = WordGeneratorWithFreemarker.createDoc(data,UPLOAD_PATH,env);
        if(result.status != HttpStatus.SC_OK) {
            return apiResult(result)
        }

        if(result.status != HttpStatus.SC_OK) {
            return apiResult(result)
        }
        //zip file
        File file = new File(UPLOAD_PATH.concat(result.msg))
        def fileName = StringUtils.removeSpecialCode(data.get("abstractId"))
        if(fileName.length() > 30){
            fileName = fileName.substring(0,30)
        }
        def zipPath = file.getParent() + "/" + fileName + ".zip"
        ZipUitl.zip(file.getParent(),zipPath )
        result.msg = zipPath
        iCompileRepo.addAbstractDowdload(newsAbstract)
        return result
    }

    Map getSummaryWord(String keyWordCloudImg,String relatedNews){
        Map<String, Object> data = new HashMap<String, Object>();
        String outfileName = UUID.randomUUID();
        data.put("fileName",outfileName)
        data.put("fileType","summary")
        data.put("picUrl","https://")
        JSONArray jsonArray = JSONArray.parseArray(relatedNews);
        //???????????????????????????????????????????????????????????????
        List<Map<String, Object>> newsDetail = new ArrayList<Map<String, Object>>();
        jsonArray.each {
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("title",isNullOrNot(it.title));
            news.put("contentAbstract",isNullOrNot(it.contentAbstract));
            news.put("site",isNullOrNot(it.site));
            news.put("time",it.time==null ?"":strToDate(it.time));
            newsDetail.add(news);
        }
        data.put("newsDetail",newsDetail)
        String outfilePath = "/${UPLOAD_PATH}/download/${data.fileType as String}/${data.fileName as String}";
        File outPath = new File(outfilePath)
        if(!outPath.exists()){
            FileUtils.forceMkdir(outPath)
        }else {
            FileUtils.forceDelete(outPath)
            FileUtils.forceMkdir(outPath)
        }
        String[] url = keyWordCloudImg.split(",");
        String getData = url[1];
        try {
            // Base64??????
            byte[] b = new BASE64Decoder().decodeBuffer(getData);
            File file = new File(outfilePath,"keyWordCloudImg.png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(b);
            if (fos != null) {
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("keyWordCloudImg",getData)
        Map result = WordUtils.createWord(UPLOAD_PATH, data, env)
        if(result.status != HttpStatus.SC_OK) {
            return apiResult(result)
        }
        //zip file
        File file = new File(UPLOAD_PATH.concat(result.msg))
        def fileName = outfileName
        if(fileName.length() > 30){
            fileName = fileName.substring(0,30)
        }
        def zipPath = file.getParent() + "/" + fileName + ".zip"
        ZipUitl.zip(file.getParent(),zipPath )
        result.msg = zipPath
        return result
    }
    public static String isNullOrNot(String str){
        String strRe="";
        strRe=(str==null?"":StringUtils.html2text(str))
        return strRe
    }
    /*????????????-????????????-????????????*/
    Map getImageSummaryWord(String intro, String title,String topNews, String list, String newsRank, String psiTrend, String bsiTrend, String bloggerRank, String distribution, String trendWeek, String firstShow, String writenTop, String structure, String miiTop,String powerTop, String region, String opinion, String trendImg, String mapImg, String formImg) {
        String outfileName = UUID.randomUUID();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("fileName",outfileName)
        data.put("fileType","downLoadImageSummary")
        data.put("picUrl","https://")

        data.put("headercontent",title==null?"????????????":title);
        data.put("spreadscope",isNullOrNot(intro))//????????????
        data.put("spreadtrend",isNullOrNot(trendWeek))//????????????
        data.put("channelcontent",isNullOrNot(firstShow))//?????????????????????
        data.put("articlecontent",isNullOrNot(writenTop))//????????????
        data.put("sherecontent",isNullOrNot(structure))//??????????????????
        data.put("mediasharepower",isNullOrNot(powerTop))//?????????????????????
        data.put("mediaimpactpowercontent",isNullOrNot(miiTop))//?????????????????????
        data.put("suggestleadercontent",isNullOrNot(opinion))//????????????
        data.put("areacontent",isNullOrNot(region))//????????????


//         ????????????????????? channelList firstShow list
        JSONArray lists = JSONArray.parseArray(list);
        List<Map<String, Object>> channelList = new ArrayList<Map<String, Object>>();
        lists.each {
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("mediatype",isNullOrNot(it.newsTypeName));
            String time=it.time;
            news.put("publishtime",time==null?"":strToDate(time));
            news.put("title",isNullOrNot(it.title));
            news.put("source",isNullOrNot(it.source));
            news.put("link",isNullOrNot(it.url));

            channelList.add(news);
        }

//        ???????????? articleList writenTop newsRank
        JSONArray newsRanks = JSONArray.parseArray(newsRank);
        List<Map<String, Object>> articleList = new ArrayList<Map<String, Object>>();
        newsRanks.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("index",(i+1));
            news.put("title",isNullOrNot(it.title));
            news.put("source",isNullOrNot(it.source));
            String time=it.time;
            news.put("publishtime",time=null?"":strToDate(time));
            news.put("siteshare",isNullOrNot(String.valueOf(it.reprintCountByPersion)));
            news.put("peopleshare",isNullOrNot(String.valueOf(it.reprintCountBySite)));
            news.put("comment",isNullOrNot(String.valueOf(it.commentCount)));
            news.put("dz",isNullOrNot(String.valueOf(it.likesCount)));

            articleList.add(news);
        }

//        ????????????????????? newsList powerTop psiTrend
        JSONArray psiTrends = JSONArray.parseArray(psiTrend);
        List<Map<String, Object>> newsList = new ArrayList<Map<String, Object>>();
        psiTrends.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("index",(i+1));
            news.put("medianame",isNullOrNot(it.siteName));
            news.put("power",isNullOrNot(String.valueOf(it.psi)));

            newsList.add(news);
        }

//        ????????????????????? mediapowerList
        JSONArray bsiTrends = JSONArray.parseArray(bsiTrend);
        List<Map<String, Object>> mediapowerList = new ArrayList<Map<String, Object>>();
        bsiTrends.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("index",(i+1));
            news.put("medianame",isNullOrNot(it.siteName));
            news.put("power",isNullOrNot(String.valueOf(it.psi)));

            mediapowerList.add(news);
        }

//        ???????????? leaderList opinion
        JSONArray bloggerRanks = JSONArray.parseArray(bloggerRank);
        List<Map<String, Object>> leaderList = new ArrayList<Map<String, Object>>();
        bloggerRanks.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("index",(i+1));
            news.put("medianame",isNullOrNot(it.name));
            news.put("power",isNullOrNot(String.valueOf(it.personsAffected)));

            leaderList.add(news);
        }
//        ????????????????????? channelList
//        ???????????? articleList
//        ????????????????????? newsList
//        ????????????????????? mediapowerList
//        ???????????? leaderList
        data.put("channelList",channelList)
        data.put("articleList",articleList)
        data.put("newsList",newsList)
        data.put("mediapowerList",mediapowerList)
        data.put("leaderList",leaderList)


        //???????????????????????????????????????????????????????????????
        String outfilePath = "/${UPLOAD_PATH}/download/${data.fileType as String}/${data.fileName as String}";
        File outPath = new File(outfilePath)
        if(!outPath.exists()){
            FileUtils.forceMkdir(outPath)
        }else {
            FileUtils.forceDelete(outPath)
            FileUtils.forceMkdir(outPath)
        }

       // String trendImg, String mapImg, String formImg

        String[] url = trendImg.split(",");
        String trendImgData = url[1];
        try {
            // Base64??????
            byte[] b = new BASE64Decoder().decodeBuffer(trendImgData);
            File file = new File(outfilePath,"trendImgData.png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(b);
            if (fos != null) {
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("trendImgData",trendImgData)

        String[] url2 = mapImg.split(",");
        String mapImgData = url2[1];
        try {
            // Base64??????
            byte[] b = new BASE64Decoder().decodeBuffer(mapImgData);
            File file = new File(outfilePath,"mapImgData.png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(b);
            if (fos != null) {
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("mapImgData",mapImgData)

        String[] url3 = formImg.split(",");
        String formImgData = url3[1];
        try {
            // Base64??????
            byte[] b = new BASE64Decoder().decodeBuffer(formImgData);
            File file = new File(outfilePath,"formImgData.png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(b);
            if (fos != null) {
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("formImgData",formImgData)




        Map result = WordUtils.createWord(UPLOAD_PATH, data, env)
        if(result.status != HttpStatus.SC_OK) {
            return apiResult(result)
        }
        //zip file
        File file = new File(UPLOAD_PATH.concat(result.msg))
        def fileName = outfileName
        if(fileName.length() > 30){
            fileName = fileName.substring(0,30)
        }
        def zipPath = file.getParent() + "/" + fileName + ".zip"
        ZipUitl.zip(file.getParent(),zipPath )
        result.msg = zipPath
        result.outfileName = outfileName
        return result
    }
    List<News> getFirstPublishMedias(ICompileSummary summary) {
        return topicApi.getFirstPublishMedias(summary)
    }

    Map getWeiboAnalysis(ICompileSummary summary) {
        return topicApi.getWeiboAnalysis(summary)
    }

    Paging<AbstractDownload> getPagingAbstractDownload(long userId, String orgId, int pageNo, int limit ) {
        long total = iCompileRepo.getAbstractDownloadTotal(userId as String, orgId)
        def paging = new Paging<AbstractDownload>(pageNo, limit, total)
        iCompileRepo.getPagingAbstractDownloadList(userId as String, orgId, paging)
        return paging
    }

    AbstractPush getAbstractPush(String id) {
        return captureRepo.getAbstractPush(id)
    }

    AbstractDownload getAbstractDownload(String id) {
        return iCompileRepo.getAbstractDownload(id)
    }

    void createNewsAbstractShare(long userId, String orgId, String abstractId, def shareChannelList, def shareContent) {
        NewsAbstract newsAbstract = getNewsAbstract(userId, abstractId)
        Date now = new Date()
        iCompileRepo.addAbstractShare([
                abstractId  : abstractId,
                title       : shareContent.title,
                newsAbstract: newsAbstract.toMap(),
                shareChannel: shareChannelList,//???????????????????????????????????????????????????
                shareContent: shareContent,
                userId      : userId,
                orgId       : orgId,
                createTime  : now,
                updateTime  : now
        ])
    }

    Paging<AbstractShare> getPagingAbstractShare(Long userId, String orgId, int pageNo, int limit) {
        long total = iCompileRepo.getAbstractShareTotal(userId, orgId)
        def paging = new Paging<AbstractShare>(pageNo, limit, total)
        iCompileRepo.getAbstractShareList(userId, orgId, paging)
        return paging
    }
/*
     * ??????????????????????????? 2017 06 01 14 21 39
     */
    public static String strToDate(String s){
        //1496321853000
        //20170601142139
        String res;
        if (s.length() ==14){
            res = s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8) + " " + s.substring(8, 10) + ":" + s.substring(10, 12) + ":" + s.substring(12, 14);
        }else if (s.length()==13){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long lcc_time = Long.valueOf(s);
            res = simpleDateFormat.format(new Date(lcc_time));
        }else{
            res="";
        }
        return res;
    }
    AbstractShare getAbstractShare(String id) {
        return iCompileRepo.getAbstractShare(id)
    }

/*????????????-????????????-??????????????????*/
    Map getTextSummaryWord(String intro,String title,String keyArticle,String newsRank,String relatedNews,String hotTopic,String latestWeibo,String discussions,String relatedNewsTrack,String hotTopicIntro,String latestWeiboView,String hotDiscussions){
//        ???????????????????????????????????????????????????????????????????????????????????????

        Map<String, Object> data = new HashMap<String, Object>();
        String outfileName = UUID.randomUUID();
        data.put("fileName",outfileName)
        data.put("fileType","downLoadTextSummary")

        //???????????? keyArticle newsRank
        JSONArray keyArticles = JSONArray.parseArray(newsRank);
        List<Map<String, Object>> keyArticleList = new ArrayList<Map<String, Object>>();
//        StringUtils.html2text(news.content),
        keyArticles.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("title",it.title==null?"":(i+1)+"???"+ StringUtils.html2text(it.title));
            news.put("contentAbstract",it.contentAbstract==null?"":StringUtils.html2text(it.contentAbstract));
            news.put("source",it.source==null?"":StringUtils.html2text(it.source));
            String time = it.time;
            news.put("time",time==null?"":strToDate(time));

            keyArticleList.add(news)
        }


        //????????????   list   relatedNews
        JSONArray relatedNewss = JSONArray.parseArray(relatedNews);
        List<Map<String, Object>> relatedNewsList = new ArrayList<Map<String, Object>>();
        relatedNewss.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("title",it.title==null?"":(i+1)+"???"+StringUtils.html2text(it.title));
            news.put("contentAbstract",it.contentAbstract==null?"":StringUtils.html2text(it.contentAbstract));
            news.put("source",it.source==null?"":StringUtils.html2text(it.source));
            String time = it.time;
            news.put("time",time==null?"":strToDate(time));

            relatedNewsList.add(news)
        }

        //???????????? hotList hotTopic
        JSONArray hotTopics = JSONArray.parseArray(hotTopic);
        List<Map<String, Object>> hotTopicList = new ArrayList<Map<String, Object>>();
        hotTopics.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("title",it.title==null?"":(i+1)+"???"+ StringUtils.html2text(it.title));
            news.put("contentAbstract",it.contentAbstract==null?"":StringUtils.html2text(it.contentAbstract));
            news.put("source",it.source==null?"":StringUtils.html2text(it.source));
            String time = it.time;
            news.put("time",time==null?"":strToDate(time));

            hotTopicList.add(news)
        }


        //???????????? lists  latestWeibo
        JSONArray latestWeibos = JSONArray.parseArray(latestWeibo);
        List<Map<String, Object>> latestWeiboList = new ArrayList<Map<String, Object>>();
        latestWeibos.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("title",it.title==null?"":(i+1)+"???"+ StringUtils.html2text(it.title));
            news.put("contentAbstract",it.contentAbstract==null?"":StringUtils.html2text(it.contentAbstract));
            news.put("source",it.source==null?"":StringUtils.html2text(it.source));
            String time = it.time;
            news.put("time",time==null?"":strToDate(time));

            latestWeiboList.add(news)
        }

        //???????????? talkList discussions
        JSONArray discussionss = JSONArray.parseArray(discussions);
        List<Map<String, Object>> discussionsList = new ArrayList<Map<String, Object>>();
        discussionss.eachWithIndex {it, i ->
            Map<String, Object> news = new HashMap<String, Object>();
            news.put("title",it.title==null?"":(i+1)+"???"+ StringUtils.html2text(it.title));
            news.put("contentAbstract",it.contentAbstract==null?"": StringUtils.html2text(it.contentAbstract));
            news.put("source",it.source==null?"": StringUtils.html2text(it.source));
            String time = it.time;
            news.put("time",time==null?"":strToDate(time));

            discussionsList.add(news)
        }
        data.put("keyAbstract",isNullOrNot(keyArticle))//????????????
        data.put("relatedAbstract",isNullOrNot(relatedNewsTrack))//????????????
        data.put("hotAbstract",isNullOrNot(hotTopicIntro))//????????????
        data.put("latestAbstract",isNullOrNot(latestWeiboView))//????????????
        data.put("discussionsAbstract",isNullOrNot(hotDiscussions))//????????????
        data.put("intro",(intro==null||intro=="null"||"".equals(intro))?"":intro)
        data.put("title",(title==null||title=="null"||"".equals(title))?"????????????????????????":title)
        data.put("keyArticleList",keyArticleList)
        data.put("relatedNewsList",hotTopicList)
        data.put("hotTopicList",relatedNewsList)
        data.put("latestWeiboList",latestWeiboList)
        data.put("discussionsList",discussionsList)


        String outfilePath = "/${UPLOAD_PATH}/download/${data.fileType as String}/${data.fileName as String}";
        //???????????????????????????????????????????????????????????????
        File outPath = new File(outfilePath)
        if(!outPath.exists()){
            FileUtils.forceMkdir(outPath)
        }else {
            FileUtils.forceDelete(outPath)
            FileUtils.forceMkdir(outPath)
        }
        def result = WordUtils.createWord(UPLOAD_PATH, data, env)
        if(result.status != HttpStatus.SC_OK) {
            return apiResult(result)
        }
        File file = new File(UPLOAD_PATH.concat(result.msg))
        def fileName = StringUtils.removeSpecialCode(data.get("title"))
        if(fileName.length() > 30){
            fileName = fileName.substring(0,30)
        }
        def zipPath = file.getParent() + "/" + fileName + ".zip"
        ZipUitl.zip(file.getParent(),zipPath )
        result.msg = zipPath
        result.outfileName = outfileName
        return result
    }

    List getSourceTypeDistribution(ICompileSummary summary) {
        return topicApi.getSourceTypeDistribution(summary)
    }

    Map getSummaryDistribution(ICompileSummary summary) {
        return topicApi.getSummaryDistribution(summary)
    }

    Map modifySummaryTemplate(long userId,String summaryId,int template){
        iCompileRepo.modifySummaryTemplate(userId, summaryId, template)
        return apiResult()
    }
    /**
     * ?????????????????????
     * @param userId
     * @return
     */
    int getSummaryDefaultTemplate(long userId){
        def compileSummary = accountRepo.getCompileSummaryProfile(userId)
        if(compileSummary.size() == 0){
            return 1
        }else {
            return compileSummary.defaultTemplate ? compileSummary.defaultTemplate : 1
        }
    }

    Map modifySummaryDefaultTemplate(long userId, int defaultTemplate){
        //?????????????????????????????????????????????????????????????????????
        AccountProfile accountProfile = accountRepo.getAccountProfileByUser(userId)
        Date now = new Date();
        if(accountProfile == null){
            accountProfile = new AccountProfile()
            accountProfile.userId = userId
            accountProfile.compileSummary = [defaultTemplate : defaultTemplate]
            accountProfile.updateTime = now
            accountProfile.createTime = now
            accountRepo.addAccountProfile(accountProfile)
        }else {
            def compileSummary = accountProfile.compileSummary;
            if(compileSummary == null){
                compileSummary = [defaultTemplate : defaultTemplate]
            }else {
                compileSummary.defaultTemplate = defaultTemplate
            }
            accountProfile.compileSummary = compileSummary
            accountProfile.updateTime = now
            accountRepo.saveAccountProfile(accountProfile)
        }
        return apiResult()
    }

    String downLoadTextSummaryPathByToken(String token) {
        def filePath = new File(UPLOAD_PATH, 'download').path
        def path = filePath.toString() + File.separator + "downLoadTextSummary" + File.separator + token
        List<String> fileList = ZipUitl.getFileNames(path)
        for (int i = 0; i < fileList.size() ; i++) {
            if(fileList.get(i).endsWith(".zip")){
                path = path + File.separator + fileList.get(i)
                return path
            }
        }
        return ""
    }
    String downLoadImageSummaryPathByToken(String token) {
        def filePath = new File(UPLOAD_PATH, 'download').path
        def path = filePath.toString() + File.separator + "downLoadImageSummary" + File.separator + token
        List<String> fileList = ZipUitl.getFileNames(path)
        for (int i = 0; i < fileList.size() ; i++) {
            if(fileList.get(i).endsWith(".zip")){
                path = path + File.separator + fileList.get(i)
                return path
            }
        }
        return ""
    }
}

package com.istar.mediabroken.api.capture

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.api.CheckPrivilege
import com.istar.mediabroken.api.CurrentUser
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.entity.News
import com.istar.mediabroken.entity.account.Privilege
import com.istar.mediabroken.service.capture.NewsService
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import java.text.SimpleDateFormat

import static com.istar.mediabroken.api.ApiResult.apiResult
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR
import static org.springframework.web.bind.annotation.RequestMethod.*

/**
 * Author: Luda
 * Time: 2017/7/28
 */
@RestController("NewsApiController")
@Slf4j
@RequestMapping(value = "/api/capture/")
class NewsApiController {
    @Autowired
    NewsService newsService

    /**
    * 用于外部系统或允许不经登录查看新闻详情的情况，
    * 获取新闻详情接口，默认是带样式的1，如果获取不带样式的新闻内容style=0
    * @param newsId
    * @param style 1获取带样式新闻（带有div css等样式），0获取不带样式新闻（新闻段落以<p>标签包裹）
    * @param original true获取es库中未经处理过的原始新闻，false获取经过style不同处理后的新闻
    * @return
     */
    @RequestMapping(value = "news/{newsId}", method = GET)
    public Map getNews(
            @PathVariable("newsId") String newsId,
            @RequestParam(value = "style", defaultValue = "1", required = false) int style,
            @RequestParam(value = "original", defaultValue = "false", required = false) boolean original
    ) {
        def result = newsService.getNewsById(newsId, style, original)
        return result
    }

    /**
     * 收藏新闻
     * @param user
     * @param newsIds
     * @return
     */
    @RequestMapping(value = "news/favorite", method = POST)
    public Map addNewsFavorite(
            @CurrentUser LoginUser user,
            @RequestBody String data
    ) {
        String[] newsIdList = []
        String groupId
        try {
            JSONObject jsonObj = JSONObject.parse(data)
            newsIdList = jsonObj.get("newsIds")
            groupId = jsonObj.get("groupId")
            if(newsIdList.size() > 300){
                return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "收藏的新闻数量不能大于300")
            }
        }catch (Exception e){
            e.printStackTrace()
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: e.getMessage())
        }
        if(newsIdList.size() == 0 || newsIdList.equals([null])){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请输入新闻的id")
        }
        def result = newsService.addNewsFavoriteByIds(user.userId, user.agentId, user.orgId, (newsIdList as List).unique(), groupId)
        return result
    }
    /**
    * 收藏新闻列表
    * @param user
    * @param pageSize
    * @param pageNo
    * @return
     */
    @RequestMapping(value = "news/favorites", method = GET)
    public Map getNewsFavorites(
            @CurrentUser LoginUser user,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo
    ) {
        def result = newsService.getUserNewsFavorites(user.userId, pageSize, pageNo)
        return result
    }
    /**
    * 获取收藏新闻详情--已废弃接口
    * @param user
    * @param id
    * @return
     */
    @RequestMapping(value = "news/favorite/{id}", method = GET)
    public Map getNewsFavorite(
            @CurrentUser LoginUser user,
            @PathVariable("id") String id //新闻的id，以逗号隔开
    ) {
        def result = newsService.getUserNewsFavorite(user.userId, id)
        return result
    }

    /**
    * 获取格式化后详情，编辑器左侧预览和拖拽入编辑器调用此接口
    * @param user
    * @param id
    * @return
     */
    @RequestMapping(value = "news/operation/{id}", method = GET)
    public Map getNewsOperation(
            @CurrentUser LoginUser user,
            @PathVariable("id") String id //word的id，以逗号隔开
    ) {
        def result = newsService.getUserNewsOperation(user.userId, id)
        return result
    }

    /**
     * 获取新闻摘要
     **/
    @RequestMapping(value = "news/operation/roundup",method = POST)
    public Map getNewsRoundup(
            @CurrentUser LoginUser user,
            @RequestParam(value = "newsIds", required = false, defaultValue = "") String newsIds,
            @RequestParam(value = "roundupSize", required = false, defaultValue = "") String roundupSize
    ){
        if ("".equals(newsIds)){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请选择新闻")
        }else {
            String[] newsId = newsIds.split(",")
            //todo 删除
            def begin = System.currentTimeMillis()
            List<News> resultList = newsService.getNewsRoundup(newsId.toList(),roundupSize as Integer)
            log.info("=======获取文章摘要方法完成耗时{}",System.currentTimeMillis()-begin)
            Map<String,Objects> resultMap = new HashMap<>()
            resultMap.put("list",resultList)
            return  apiResult(resultMap)
        }
    }

    /**
     * 获取新闻观点
     **/
    @RequestMapping(value = "news/operation/viewpoint",method = POST)
    public Map getNewsViewpoint(
            @CurrentUser LoginUser user,
            @RequestParam(value = "newsIds", required = false, defaultValue = "") String newsIds
    ){
        if ("".equals(newsIds)){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请选择新闻")
        }else {
            String[] newsId = newsIds.split(",")
            //todo 删除
            def begin = System.currentTimeMillis()
            Map resultMap = newsService.getNewsViewpoint(newsId.toList())
            log.info("=======获取文章观点方法完成耗时{}",System.currentTimeMillis()-begin)
            if (resultMap.get("noViewpoint")){
                return  apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "抱歉，所选文章没有提取到观点，请选择其他文章。")
            }
            return  apiResult(resultMap)
        }
    }

    /**
    * 删除收藏新闻
    * @param user
    * @param id
    * @return
     */
    @RequestMapping(value = "news/operation/{id}", method = DELETE)
    public Map removeNewsOperation(
            @CurrentUser LoginUser user,
            @PathVariable("id") String id //新闻的id，以逗号隔开
    ) {
        def result = newsService.removeUserNewsOperation(user.userId, id)
        return result
    }

    /**
    * 批量删除收藏新闻
    * @param user
    * @param id
    * @return
     */
    @RequestMapping(value = "news/operation/remove", method = POST)
    public Map removeNewsOperationList(
            @CurrentUser LoginUser user,
            @RequestBody String data
    ) {
        JSONObject dataJson = JSONObject.parseObject(data);
        String operationIds = dataJson.get("operationIds").toString();
        List split = operationIds.split(",")
        def result = newsService.removeUserNewsOperationList(user.userId, split)
        return result
    }

    /**
     * 新闻推送
     * @param user
     * @param newsIds
     * @return
     */
    @CheckPrivilege(privileges = [Privilege.PUSHNEWS_MANAGE])
    @RequestMapping(value = "news/push", method = POST)
    public Map addNewsPush(
            @CurrentUser LoginUser user,
            @RequestBody String newsIds
    ) {
        String[] newsIdList = []
        try {
            JSONObject newsIdsJSON = JSONObject.parse(newsIds)
            newsIdList = newsIdsJSON.get("newsIds")
            if(newsIdList.size() > 300){
                return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "推送的新闻数量不能大于300")
            }
        }catch (Exception e){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: e.getMessage())
        }
        if(newsIdList.size() == 0){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请输入新闻的id")
        }
        if ((!user.orgId) || "0".equals(user.orgId)){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "推送失败，您的账号未开通平台对接功能")
        }
        def result = newsService.addNewsPushByIds(user.userId, user.agentId, user.orgId, user.teamId, (newsIdList as List).unique())
        return result
    }

    /**
    * 查询推送历史
    * @param user
    * @param pushUser 推送人的userId
    * @param pushChannel 渠道 新闻 1 微信 2  微博 3 我的文稿 4
    * @param autoPushType 是否自动推送 isAutoPush 0 手动推送 1 自动推送
    * @param startTime
    * @param endTime
    * @param pageNo
    * @param pageSize
    * @return
     */
    @CheckPrivilege(privileges = [Privilege.PUSHNEWS_MANAGE])
    @RequestMapping(value = "news/pushHistory", method = GET)
    public Map getNewsPushHistory(
            @CurrentUser LoginUser user,
            @RequestParam(value = "pushUser", required = false, defaultValue = "") String pushUser,
            @RequestParam(value = "pushChannel", required = false, defaultValue = "") String pushChannel,
            @RequestParam(value = "autoPushType", required = false, defaultValue = "") String autoPushType,
            @RequestParam(value = "startTime", required = false, defaultValue = "") String startTime,
            @RequestParam(value = "endTime", required = false, defaultValue = "") String endTime,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize
    ) {
        //判断时间格式是否合法
        def sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def startDate = null
        def endDate = null
        try {
            if (startTime) {
                startDate = sdf.parse(startTime)
            }
            if (endTime) {
                endDate = sdf.parse(endTime)
            }
        } catch (Exception e) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "时间格式不正确")
        }
        Long pushUserId = 0l
        if (pushUser){
            pushUserId = Long.valueOf(pushUser)
        }
        def list = newsService.getNewsPushHistory(user.userId, user.orgId, user.teamId, pushUserId, pushChannel, autoPushType, startDate, endDate, pageNo, pageSize)
        def count = newsService.getNewsPushHistoryCount(user.userId, user.orgId, user.teamId, pushUserId, pushChannel, autoPushType, startDate, endDate)
        return apiResult([list : list, count: count])
    }

    /**
    * 查询今日推送的数据量
    * @param user
    * @return
     */
    @CheckPrivilege(privileges = [Privilege.PUSHNEWS_MANAGE])
    @RequestMapping(value = "news/pushHistoryCount", method = GET)
    public Map getNewsPushHistoryCount(
            @CurrentUser LoginUser user
    ) {
        def map = newsService.getNewsPushHistoryCount(user.userId, user.orgId, user.teamId)
        return apiResult(map)
    }

    @RequestMapping(value = "/news/modify/group" , method = RequestMethod.POST)
    def modifyNewsGroupNameByGroupId(
            @CurrentUser LoginUser user,
            @RequestParam(value = "groupId", required = false) String groupId,
            @RequestParam(value = "newsOperationId", required = false) String newsOperationId
    ){
        try {
            newsService.modifyNewsGroupByGroupId(groupId, newsOperationId)
            return apiResult(HttpStatus.SC_OK, '移动分组成功！')
        } catch (Exception e) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, '移动分组失败！')
        }
    }

    /**
    * bjj项目中用于获取带样式或不带样式的新闻详情，在接口中对获取到的新闻详情进行了处理
    * @param newsId
    * @param user
    * @param style 1获取带样式新闻（带有div css等样式），0获取不带样式新闻（新闻段落以<p>标签包裹）
    * @param original true获取es库中未经处理过的原始新闻，false获取经过style不同处理后的新闻
    * @return
     */
    @RequestMapping(value = "newsDetail/{newsId}", method = GET)
    public Map getNewsByUser(
            @PathVariable("newsId") String newsId,
            @CurrentUser LoginUser user,
            @RequestParam(value = "style", defaultValue = "1", required = false) int style,
            @RequestParam(value = "original", defaultValue = "false", required = false) boolean original
    ) {
        def result = newsService.getNewsByNewsIdAndUser(newsId, user.userId, style, original)
        return result
    }

    @RequestMapping(value = "news/collect/{newsId}", method = DELETE)
    public Map removeNewsOperationByNewsId(
            @CurrentUser LoginUser user,
            @PathVariable("newsId") String newsId //新闻的id，以逗号隔开
    ) {
        def result = newsService.removeNewsOperationByNewsIdAndType(user.userId, newsId, 3)
        return result
    }



    /**
    * 单条收藏新闻
    * @param user
    * @param data
    * @return
     */
    @RequestMapping(value = "news/favorite/{newsId}", method = POST)
    public Map newsFavorite(
            @CurrentUser LoginUser user,
            @PathVariable("newsId") String newsId,
            @RequestParam(value = "groupId", required = false) String groupId
    ) {

        List newsIdList = []
        newsIdList.add(newsId)
        if(newsIdList.size() == 0 || newsIdList.equals([null])){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "请输入新闻的id")
        }
        def result = newsService.addNewsFavoriteByIds(user.userId, user.agentId, user.orgId, newsIdList, groupId)
        return result
    }
}

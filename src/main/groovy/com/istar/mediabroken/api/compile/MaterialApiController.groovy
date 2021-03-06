package com.istar.mediabroken.api.compile

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.api.CheckExpiry
import com.istar.mediabroken.api.CheckPrivilege
import com.istar.mediabroken.api.CurrentUser
import com.istar.mediabroken.api.CurrentUserId
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.entity.account.Privilege
import com.istar.mediabroken.service.ShareChannelService
import com.istar.mediabroken.service.capture.NewsService
import com.istar.mediabroken.service.compile.MaterialService
import com.istar.mediabroken.utils.DateUitl
import com.istar.mediabroken.utils.HtmlUtil
import com.istar.mediabroken.utils.UploadUtil
import com.istar.mediabroken.utils.Word2Html
import groovy.util.logging.Slf4j
import net.sf.json.JSONArray
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

import static com.istar.mediabroken.api.ApiResult.apiResult
import static org.springframework.web.bind.annotation.RequestMethod.*

/**
 * Author: Luda
 * Time: 2017/8/1
 */
@RestController
@Slf4j
@RequestMapping(value = "/api/compile/")
class MaterialApiController {

    @Autowired
    MaterialService materialService
    @Autowired
    ShareChannelService shareChannelSrv
    @Autowired
    NewsService newsService

    @Value('${image.upload.path}')
    public String uploadPath

    @RequestMapping(value = "material", method = POST)
    public Map addUserMaterial(
            HttpServletRequest request,
            @CurrentUser LoginUser user,
            @RequestBody String data
    ) {
        JSONObject inputData = new JSONObject()
        try {
            inputData = JSONObject.parse(data)
        }catch (Exception e){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "???????????????????????????" + e.getMessage())
        }
        def inputTitle = inputData?.title?.trim()
        def inputAuthor = inputData?.author?.trim()
        def inputContent = inputData?.content?.trim()
        def inputContentAbstract = inputData?.contentAbstract.trim()
        String tagIds = inputData?.tagIds.trim()

        JSONArray customTags = JSONArray.fromObject(inputData.customTags.toString())
        List<Map<String, Object>> tagList = new ArrayList<>()
        if(customTags) {
            for(JSONObject json: customTags) {
                Map<String, Object> map = new HashMap<>()
                map.put("tagName", json.get("tagName"))
                map.put("tagType", json.get("tagType"))
                map.put("tagValue", json.get("tagValue"))
                tagList.add(map)
            }
        }

        if (inputTitle.equals("")) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "?????????????????????")
        }
        if (inputContent.equals("")|| inputContent.equals("<p><br></p>")) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "?????????????????????")
        }
        if (inputTitle.length() > 64) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????64")
        }
        if (inputAuthor.length() > 8) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????8")
        }
        if (inputContentAbstract.length() > 120) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????120")
        }
        if (!(inputData?.type in [1, 2, 3])) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "type???????????????")
        }

        List idsList =[]
        if (StringUtils.isNoneBlank(tagIds) && tagIds.length() > 0 ){
            idsList=tagIds.split(",")
        }else {
            idsList =[]
        }
        def result = materialService.addUserMaterial(request, user.userId, user.orgId, inputTitle, inputAuthor, inputContent,
                inputContentAbstract, inputData?.keywords?.trim(), inputData?.classification?.trim(),
                inputData?.source?.trim(), inputData?.picUrl?.trim(), inputData?.type, idsList, tagList)
         return result
    }

    /**
     * ????????????????????????
     * @param id
     * @return
     */
    @RequestMapping(value = "material/{id}", method = GET)
    public Map getUserMaterial(
            @CurrentUser LoginUser user,
            @PathVariable("id") String id
    ) {
        def result = materialService.getUserMaterial(id)
        return result;
    }

    /**
     * ??????????????????
     * @param user
     * @param id
     * @param title
     * @param author
     * @param content
     * @param contentAbstract
     * @param keywords
     * @param classification
     * @param source
     * @param picUrl
     * @param type
     * @return
     */
    @RequestMapping(value = "material/{id}", method = PUT)
    public Map modifyUserMaterial(
            HttpServletRequest request,
            @CurrentUser LoginUser user,
            @PathVariable("id") String id,
            @RequestBody String data
    ) {
        String title=""
        String author=""
        String content=""
        String contentAbstract=""
        String keywords=""
        String classification=""
        String source=""
        String picUrl=""
        int type=0
        String tagIds =""
        List<Map<String, Object>> tagList = new ArrayList<>()
        try{
            JSONObject newsAbstractJson = JSONObject.parse(data)
            title = newsAbstractJson.get("title")
            author = newsAbstractJson.get("author")
            content = newsAbstractJson.get("content")
            contentAbstract = newsAbstractJson.get("contentAbstract")
            keywords = newsAbstractJson.get("keywords")
            classification = newsAbstractJson.get("classification")
            source = newsAbstractJson.get("source")
            picUrl = newsAbstractJson.get("picUrl")
            type = newsAbstractJson.get("type")
            tagIds = newsAbstractJson.get("tagIds")
            //?????????????????????
            JSONArray customTags = JSONArray.fromObject(newsAbstractJson.customTags.toString())
            if(customTags) {
                for(JSONObject json: customTags) {
                    Map<String, Object> map = new HashMap<>()
                    map.put("tagName", json.get("tagName"))
                    map.put("tagType", json.get("tagType"))
                    map.put("tagValue", json.get("tagValue"))
                    tagList.add(map)
                }
            }
        }catch (Exception e){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????"])
        }
        def inputTitle = title.trim()
        def inputAuthor = author.trim()
        def inputContent = content.trim()
        def inputContentAbstract = contentAbstract.trim()
        if ("".equals(inputTitle)) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "?????????????????????")
        }
        if ("".equals(inputContent) || inputContent.equals("<p><br></p>")) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "?????????????????????")
        }
        if (inputTitle.length() > 64) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????64")
        }
        if (inputAuthor.length() > 8) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????8")
        }
        if (inputContentAbstract.length() > 120) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????120")
        }
        if (!(type in [1, 2, 3])) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "type???????????????")
        }
        List idsList = []
        if (StringUtils.isNoneBlank(tagIds) && tagIds.length() > 0) {
            idsList = tagIds.split(",")
        }else {
            idsList = []
        }
        def result = materialService.modifyUserMaterial(request, user.userId, id, user.orgId, inputTitle, inputAuthor, inputContent,
                inputContentAbstract, keywords.trim(), classification.trim(), source.trim(), picUrl.trim(), type, idsList, tagList)
        return result
    }
    /**
     * ??????????????????
     * @param userId
     * @return
     */
    @RequestMapping(value = "materials", method = GET)
    public Map getUserMaterials(
            @CurrentUserId Long userId,
            @RequestParam(value="pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value="pageSize", required = false, defaultValue = "10" ) int pageSize,
            @RequestParam(value = "queryKeyWords",required = false , defaultValue = "") String queryKeyWords,
            @RequestParam(value = "isPublished",required = false , defaultValue = "0") int isPublished,//0 1 2 4
            @RequestParam(value = "isReleased",required = false , defaultValue = "0") int isReleased //0 1 2
            ) {
        def result = materialService.getUserMaterials(userId, queryKeyWords, isPublished,isReleased,pageSize, pageNo)
        return result
    }

    /**
    *  ??????????????????
    * @param userId
    * @param queryKeyWords
    * @param isPublished
    * @return
     */
    @RequestMapping(value = "materials/count", method = GET)
    public Map getUserMaterialCount(
            @CurrentUserId Long userId,
            @RequestParam(value = "queryKeyWords",required = false , defaultValue = "") String queryKeyWords,
            @RequestParam(value = "isPublished",required = false , defaultValue = "0") int isPublished,//0 1 2
            @RequestParam(value = "isReleased",required = false , defaultValue = "0") int isReleased//0 1 2
            ) {
        def result = materialService.getUserMaterialCount(userId, queryKeyWords, isPublished, isReleased)
        return result;
    }

    /**
     *  ????????????
     * @param userId
     * @param id
     * @return
     */
    @RequestMapping(value = "material/{id}", method = DELETE)
    public Map removeUserMaterial(
            @CurrentUserId Long userId,
            @PathVariable("id") String id
    ) {
        materialService.removeUserMaterial(userId, id)
        return apiResult()
    }

    /**
     *  ??????????????????
     * @param userId
     * @param id
     * @return
     */
    @RequestMapping(value = "material/remove", method = POST)
    public Map removeUserMaterialList(
            @CurrentUserId Long userId,
            @RequestBody String data
    ) {
        JSONObject dataJson = JSONObject.parseObject(data);
        String operationIds = dataJson.get("materialIds").toString()
        List split = operationIds.split(",")
        materialService.removeUserMaterialList(userId, split)
        return apiResult()
    }

    /**
    *??????????????????
    * @param user
    * @param data
    * @return
     */
    @CheckPrivilege(privileges = [Privilege.PUSHNEWS_MANAGE])
    @RequestMapping(value = "material/articlePush", method = POST)
    public Map addArticlePush(
            @CurrentUser LoginUser user,
            @RequestParam(value = "materialId",required = false , defaultValue = "") String materialId
    ) {
        if ((!user.orgId) || "0".equals(user.orgId)){
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "??????????????????????????????????????????????????????")
        }
        def resultMaterial = materialService.addArticlePush(user.userId, user.agentId, user.orgId, user.teamId, materialId)
        return apiResult(resultMaterial);
    }

    /**
    *??????ID??????????????????????????????
    * @param userId
    * @param id
    * @return
     */
    @RequestMapping(value = "material/article/{id}", method = GET)
    public Map getUserArticlePush(
            @PathVariable("id") String id
    ) {
        def result = materialService.getUserArticleOperationById(id)
        return result;
    }

    /**
    *??????word???????????????content
    * @param userId
    * @param id
    * @param request
    * @return
     */
    @RequestMapping(value = "material/import", method = POST, produces = ["text/plain;charset=UTF-8"])
    @CheckExpiry
    public String importArticle(
            @CurrentUser LoginUser user,
            HttpServletRequest request
    ) {
        def res = UploadUtil.uploadWord(request, uploadPath, '/doc')
        if(!(res.status == HttpStatus.SC_OK)){
            return JSONObject.toJSONString(res)
        }
        String title="?????????"
        if (res){
            String fileNames = res.fileName
            title=fileNames.substring(0,fileNames.size()-4)
        }
        String content = ""
        def result = [status: HttpStatus.SC_OK, msg: '']
        try{
            content = Word2Html.getWordAndStyle(uploadPath + File.separator + res.msg)
        }catch (Exception e){
            result = [status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '??????????????????????????????']
            return result
        }

        result = materialService.importArticle(user.userId, user.agentId, user.orgId, user.teamId, title,content)

        return JSONObject.toJSONString(result)
    }

    /**
    * ??????????????????
    * @param userId
    * @param newsOperationId
    * @return
     */
    @RequestMapping(value = "material/newsOperation/{id}", method = GET)
    public Map getUserNewsOperationById(
            @PathVariable String id
            ) {
        def result = materialService.getUserNewsOperationById(id)
        return result;
    }

    @RequestMapping(value = "material/removeDuplicateParagraph", method = POST)
    public Map removeDuplicateParagraph(
            HttpServletRequest request,
            @CurrentUser LoginUser user,
            @RequestBody String content
    ) {
        List result = HtmlUtil.removeDuplicateParagraph(content)
        return apiResult(status: HttpStatus.SC_OK, content: result)
    }

    @RequestMapping(value = "material/images", method = POST)
    public Map extractImages(
            HttpServletRequest request,
            @RequestBody String data
    ) {
        try{
            JSONObject dataJson = JSONObject.parse(data)
            String content = dataJson.get("content")

            content = shareChannelSrv.dealBase64Image2Local(request, content)
            def list = materialService.extractCoverList(request, content)

            return apiResult([
                    content     : content,
                    imageList   : list
            ])
        } catch (Exception e){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "??????????????????"])
        }

    }

    /**
    *   h5 ??????(?????????) ?????????????????????
     * @param user
     * @param materialId
     * @return
     */
    @RequestMapping(value = "material/release", method = POST)
    public Map MaterialRelease(
            @CurrentUser LoginUser user,
            @RequestParam(value = "materialId",required = false , defaultValue = "") String materialId
    ) {

        if (!materialId){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????"])
        }
        def release = materialService.MaterialRelease(user, materialId)
        return release
    }

    /**
     * ????????????????????????????????????????????????????????????
     * @param user
     * @param id
     * @return
     */
    @RequestMapping(value = "material/preview/{id}", method = PUT)
    public Map MaterialProcess(
            @CurrentUser LoginUser user,
            @PathVariable String id
    ) {
        if (!id){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: []])
        }
        def process = materialService.MaterialPreview(user, id)
        if (process){
            return apiResult([status: HttpStatus.SC_OK, msg: process])
        }else if (!process){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: []])
        }
    }

    /**
    * ????????????????????????(??????????????????)
    * @param user
    * @param id
    * @return
     */
    @RequestMapping(value = "material/detail/{id}", method = GET)
    public Map getMaterialDetail(
            @PathVariable String id
    ) {
        if (!id){
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: []])
        }
        def time = new Date()
        def result = materialService.getArticleOperationByTime(id, DateUitl.addMins(time, -30))
        if (!result) {
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: []])
        }
        return apiResult([status: HttpStatus.SC_OK, msg: result])
    }

}

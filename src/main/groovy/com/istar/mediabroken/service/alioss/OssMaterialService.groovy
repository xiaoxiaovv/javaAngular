package com.istar.mediabroken.service.alioss

import com.istar.mediabroken.repo.alioss.OssRepo
import com.istar.mediabroken.utils.HttpHelper
import groovy.util.logging.Slf4j
import net.sf.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @description: 视频相关
 * @author: hexushuai
 * @date: 2019/1/18 9:10
 */
@Service
@Slf4j
class OssMaterialService {

    @Autowired
    private OssRepo ossRepo
    @Autowired
    private OssApiUrlService ossApiUrlService

    /**
     * 上传请求构造，返回前端上传
     */
    def upload(String orgId, long userId, String type) {
        Map<String, String> respMap = ossRepo.getOssRequestMap(orgId, userId, type)
        JSONObject ja1 = JSONObject.fromObject(respMap)
        return ja1
    }

    /**
     * 请求转码
     */
    void submitTransCode(String url, materialId, String orgId, long userId) {
        JSONObject result = HttpHelper.doGet(url, null)
        JSONObject resultList = result.get("JobResultList")
        JSONObject jobList = resultList.get("JobResult")
        JSONObject job = jobList.get("Job")
        log.info("submit to aliyun oss json response:{}", job)
        if (resultList) {
            //获取 job_id state
            Map map = new HashMap()
            map.put("jobId", job.get("JobId"))
            map.put("pipelineId", job.get("PipelineId"))
            map.put("state", job.get("State"))
            map.put("orgId", orgId)
            map.put("userId", userId)
            map.put("materialId", materialId)
            ossRepo.addJob(map)
        }
    }

    /**
     * 上传视频素材,插入素材表
     */
    public String addMaterial(String orgId, long userId, String object) {
        Map ossMap = ossRepo.getAliyunOssParameter(orgId)
        ossMap.put("object", object)
        ossMap.put("userId", userId)
        println("add material video parameter:{}" + ossMap)
        return ossRepo.addMaterial(ossMap)
    }

    /**
     * 上传图片
     */
    public void addImg(String orgId, long userId, String object) {
        Map ossMap = ossRepo.getAliyunOssParameter(orgId)
        ossMap.put("object", object)
        ossMap.put("userId", userId)
        log.info("add material img parameter:{}", ossMap)
        ossRepo.addImg(ossMap)
    }

    /**
     * 更新转码状态
     */
    public void updateTransCode() {
        while(true) {
            Map map = ossRepo.getTranscodeList()
            if(!map) {
                log.info("no oss job need to update")
                return
            }
            List jobList = map.get("jobList")
            if(jobList.size() == 0) {
                return
            }
            String url = ossApiUrlService.getStateTransCodeUrl(map.get("orgId"), jobList)
            JSONObject str = HttpHelper.doGet(url, null)
            JSONObject json = JSONObject.fromObject(str)
            JSONObject job = json.get("JobList")
            log.info("get oss transCode job json :{}", job)
            List<JSONObject> list = job.get("Job")
            ossRepo.updateMaterialState(list)
        }
    }
}

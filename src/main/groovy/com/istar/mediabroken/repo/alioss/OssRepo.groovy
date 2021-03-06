package com.istar.mediabroken.repo.alioss

import com.aliyun.oss.OSSClient
import com.aliyun.oss.common.utils.BinaryUtil
import com.aliyun.oss.model.MatchMode
import com.aliyun.oss.model.PolicyConditions
import com.istar.mediabroken.entity.compile.ImgLibrary
import com.istar.mediabroken.utils.MongoHolder
import groovy.util.logging.Slf4j
import net.sf.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import static com.istar.mediabroken.utils.MongoHelper.toObj

/**
 * @author: hexushuai
 * @date: 2019/1/18 9:56
 */
@Repository
@Slf4j
class OssRepo {

    @Autowired
    MongoHolder mongo
    @Value('${aliyun.oss.callback}')
    String callbackUrl

    def getOssRequestMap(String orgId, long userId, String type) {
        def collection = mongo.getCollection("organization")
        def obj = collection.findOne(toObj([_id: orgId]))
        Map map = obj.get("aliyunoss")
        Map<String, String> respMap = new LinkedHashMap<String, String>()
        if(map) {
            String accessId = map.get("accessId")
            String endpoint = map.get("endpoint")
            String bucketType
            if(type.equals("video")) {
                bucketType = map.get("bucketVideo")
            } else {
                bucketType = map.get("bucketImg")
            }
            println "请求到的文件格式为:" + type + ", bucketType=" + bucketType
            String host = "http://" + bucketType + "." + endpoint
            OSSClient client = new OSSClient(endpoint, accessId, map.get("accessKey"))
            String dir = orgId + "/" + userId.toString() + "/"
            long expireTime = 30
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000
            Date expiration = new Date(expireEndTime)
            PolicyConditions policy = new PolicyConditions()
            try {
                policy.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000)
                policy.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir)
                String postPolicy = client.generatePostPolicy(expiration, policy)
                byte[] binaryData = postPolicy.getBytes("utf-8")
                String encodedPolicy = BinaryUtil.toBase64String(binaryData)
                String postSignature = client.calculatePostSignature(postPolicy)
                respMap.put("accessid", accessId)
                respMap.put("policy", encodedPolicy)
                respMap.put("signature", postSignature)
                respMap.put("dir", dir)
                respMap.put("host", host)
                respMap.put("expire", String.valueOf(expireEndTime / 1000))
                JSONObject jasonCallback = new JSONObject()
                jasonCallback.put("callbackUrl", callbackUrl)
                jasonCallback.put("callbackBody", "filename=\${object}&size=\${size}&mimeType=\${mimeType}&height=\${imageInfo.height}&width=\${imageInfo.width}&x:var1=" + orgId + "&x:var2=" + userId)
                jasonCallback.put("callbackBodyType", "application/json")
                String base64CallbackBody = BinaryUtil.toBase64String(jasonCallback.toString().getBytes())
                respMap.put("callback", base64CallbackBody)
            } catch (Exception e) {
                log.debug("get aliyun oss request error")
            }
        }
        return respMap
    }

    def getAliyunOssParameter(String orgId) {
        def collection = mongo.getCollection("organization")
        def obj = collection.findOne(toObj([_id: orgId]))
        Map map = obj.get("aliyunoss")
        return map
    }

    void addJob(Map map) {
        def collection = mongo.getCollection("aliOssJob")
        collection.insert(toObj([
                _id: UUID.randomUUID().toString(),
                orgId: map.get("orgId"),
                userId: map.get("userId"),
                materialId: map.get("materialId"),
                jobId: map.get("jobId"),
                pipelineId: map.get("pipelineId"),
                state: map.get("state"),
                createTime: new Date(),
                updateTime: new Date()
        ]))
    }

    String addMaterial(Map ossMap) {
        def material = mongo.getCollection("materialLibrary")
        String object = ossMap.get("object").toString()
        String transferName = object.substring(0, object.lastIndexOf(".")) + ".mp4"
        String cutFrameName = object.substring(0, object.lastIndexOf(".")) + ".jpg"
        String videoSource = "http://" + ossMap.get("bucketVideo") + "." + ossMap.get("endpoint") + "/" + ossMap.get("object")
        String videoPublish = "http://" + ossMap.get("bucketTranscode") + "." + ossMap.get("endpoint") + "/" + ossMap.get("publishDir") + "/" + transferName
        String videoPreview = "http://" + ossMap.get("bucketTranscode") + "." + ossMap.get("endpoint") + "/" + ossMap.get("previewDir") + "/" + transferName
        String videoCutFrameImg = "http://" + ossMap.get("bucketTranscode") + "." + ossMap.get("endpoint") + "/" + ossMap.get("cutFrameDir") + "/" + cutFrameName
        String uuid = UUID.randomUUID().toString()
        def video = new ImgLibrary(
                _id: uuid,
                userId: Long.parseLong(ossMap.get("userId").toString()),
                videoSourceUrl: videoSource,
                videoPublishUrl: videoPublish,
                videoPreviewUrl: videoPreview,
                cutFrameImgUrl: videoCutFrameImg,
                videoState: 2,
                type: "video",
                updateTime: new Date(),
                createTime: new Date()
        )
        material.insert(toObj(video.toMap()))
        return uuid
    }

    void addImg(Map ossMap) {
        def material = mongo.getCollection("materialLibrary")
        String imgUrl = "http://" + ossMap.get("bucketImg") + "." + ossMap.get("endpoint") + "/" + ossMap.get("object")
        def img = new ImgLibrary(
                _id: UUID.randomUUID().toString(),
                userId: Long.parseLong(ossMap.get("userId").toString()),
                imgGroupId: "0",
                mina: false,
                imgUrl: imgUrl,
                type: "img",
                updateTime: new Date(),
                createTime: new Date()
        )
        material.insert(toObj(img.toMap()))
    }

    def getTranscodeList() {
        def collection = mongo.getCollection("aliOssJob")
        List resultList = new ArrayList<>()
        Map map = new HashMap()
        List stateList = new ArrayList()
        stateList.add("Submitted")
        stateList.add("Transcoding")
        def list = collection.find(toObj([state: ['$in': stateList]])).limit(10)
        if(!list) {
            return null
        }
        String userId = ""
        String orgId = ""
        list.each { it ->
            userId = it.userId
            orgId = it.orgId
            resultList.add(it.jobId)
        }
        map.put("userId", userId)
        map.put("orgId", orgId)
        map.put("jobList", resultList)
        log.info("need to update oss job size:{}", resultList.size())
        println "需要转码的任务数量:" + resultList.size()
        return map
    }

    void updateMaterialState(List<JSONObject> list) {
        def collection = mongo.getCollection("aliOssJob")
        def material = mongo.getCollection("materialLibrary")
        for(JSONObject json: list) {
            String materialId
            def cursor = collection.find(toObj([
                    jobId: json.get("JobId")])).limit(1)
            while (cursor.hasNext()) {
                def obj = cursor.next()
                materialId = obj.materialId
                cursor.close()
            }
            collection.update(toObj([
                    jobId: json.get("JobId"),
            ]), toObj(['$set':[
                    state: json.get("State"),
                    updateTime: new Date()]
            ]), false, false)
            //更新素材库
            int materialState = 0
            if(json.get("State").toString().equals("Transcoding")) {
                materialState = 2
            } else if(json.get("State").toString().equals("TranscodeSuccess")) {
                //考虑到转码两次，有第一次是转码中 第二次是转码成功的情况，这种仍算转码中
                int count = material.find(toObj([
                        materialId: materialId, state: "Transcoding"])).count()
                if(count == 0) {
                    materialState = 3
                } else {
                    materialState = 2
                }

            } else if(json.get("State").toString().equals("TranscodeFail")) {
                materialState = 4
            } else if(json.get("State").toString().equals("Submitted")) {
                materialState = 1
            }
            material.update(toObj([
                    _id: materialId,
            ]), toObj(['$set':[
                        videoState: materialState,
                        updateTime: new Date()]
            ]), false, false)
        }

    }
}

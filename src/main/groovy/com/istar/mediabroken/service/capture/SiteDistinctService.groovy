package com.istar.mediabroken.service.capture

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.entity.capture.Site
import com.istar.mediabroken.repo.capture.SiteDistinctRepo
import com.istar.mediabroken.utils.DateUitl
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * @author zxj
 * @create 2019/1/3
 */
@Service
@Slf4j
class SiteDistinctService {
    @Autowired
    SiteDistinctRepo siteDistinctRepo;
    @Autowired
    SiteService service

    @Value('${xgsj.token}')
    String token
    @Value('${xgsj.subject.weibo.id}')
    String weiboId
    @Value('${report.request.url}')
    String requestUrl
    @Value('${push.kafka.result}')
    String pushKafka

    @Value('${xgsj.subject.url.update}')
    String subjectUrlUpdate

    def traceNewsForWeiboTask() {
        //查询出来所有 es ：无数据， 主题 ：无数据
        long start = System.currentTimeMillis();
        log.info("---处理微博数据开始----：{}", start);
        log.info("获取站点表数据+++：{}", start);
        def siteList = this.weiboSiteNameListNotSubject(null, false, null)

        //添加主题
        long start1 = System.currentTimeMillis();
        log.info("更新星光数据平台主题+++：{}", start1);
        log.info("更新星光数据平台主题添加微博个数：{}", siteList.size());
        if (siteList.size() > 0) {
            this.addXGSJSubjectForweibo(siteList)
        }
        //请求 推送数据到kafka
        long start2 = System.currentTimeMillis();
        log.info("请求星光平台数据+++：{}", start2);
        def list = this.weiboSiteNameListNotSubject(null, null, false)
        log.info("请求星光平台数据微博个数：{}", siteList.size());
        if (list.size() > 0) {
            this.traceWeiboData(list)
        }
        log.info("---处理微博数据完成----：{}", start2 - start);
    }

    List weiboSiteNameListNotSubject(Boolean esHave, Boolean subjectHave, Boolean trace) {
        def list = siteDistinctRepo.getSiteDisList(esHave, subjectHave, trace, Site.SITE_TYPE_WEIBO)
        return list
    }

    def addXGSJSubjectForweibo(List siteList) {

        List nameList = [];
        List ids = [];
        for (int i = 0; i < siteList.size(); i++) {
            Map map = siteList.get(i);
            nameList.add(map.get("siteName"))
            ids.add(map.get("_id"))
        }
        //添加主题
        if (nameList.size() > 0) {
            List subject = service.getXGSJweiboSubject()
            subject.addAll(nameList);
            String join = StringUtils.join(subject, ",")
            this.updateXGSJSubjectweibo(join)
            siteDistinctRepo.updateSiteDisListToSubjectHave(true, ids)
        }
    }

    def traceWeiboData(List sites) {
        List nameList = [];
        List ids = [];
        for (int i = 0; i < sites.size(); i++) {
            Map map = sites.get(i);
            nameList.add(map.get("siteName"))
            ids.add(map.get("_id"))
        }

        HttpResponse response = null;
        JSONObject object = null;
        try {
            response = Unirest.post(requestUrl)
                    .field("result_url", pushKafka)//推送到那
                    .field("max_count", nameList.size() * 350 * 7)
                    .field("url_dedup", true)// true为url去重
                    .field("user_logic", "*")
                    .field("filter_source", StringUtils.join(nameList, ","))
                    .field("filter_domain", "weibo.com")
                    .field("filter_media", "04")
                    .field("start_ctime", DateUitl.addDay(new Date(), -7).getTime())//2018-01-01 00:00:00
                    .field("end_ctime", new Date().getTime())
                    .field("plugins", "keywords5,sim_hash")
                    .asJson();
            object = JSONObject.parseObject(response.getBody().toString());
            if (Objects.equals(object.get("result_code"), "1")) {
                //记录jobId
                String job_id = (String) object.get("job_id");
                log.info("请求星光数据的jobId：{}", job_id);
                siteDistinctRepo.updateSiteDisListToTrace(true, ids, job_id)

            }

        } catch (UnirestException e) {
            log.info("请求星光数据接口异常：{}", e);
            e.printStackTrace();
        }
    }
    /**
     *
     * @param siteNames 逗号分割的字符串
     * @return
     */
    def updateXGSJSubjectweibo(String siteNames) {
        HttpResponse response = null;
        JSONObject object = null;
        try {

            response = Unirest.post(subjectUrlUpdate)
                    .queryString("token", token)
                    .field("id", weiboId)
                    .field("filter_source", siteNames)
                    .asJson();
            object = JSONObject.parseObject(response.getBody().toString());
        } catch (Exception e) {
            log.info("智慧星光更新主题错误{}", e.toString())
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        String url = "https://xgsj.istarshine.com/v1/subject/update?token=43857c37-8390-4d01-b46d-1c071fdcb153";
        HttpResponse response = null;
        JSONObject object = null;
        try {

            response = Unirest.post(url)
                    .field("id", "771197934454784")
                    .field("filter_source", "原州水利,原州人社,")
                    .asJson();
            object = JSONObject.parseObject(response.getBody().toString());
        } catch (Exception e) {
            log.info("智慧星光更新主题错误{}", e.toString())
            e.printStackTrace();
        }

    }
}

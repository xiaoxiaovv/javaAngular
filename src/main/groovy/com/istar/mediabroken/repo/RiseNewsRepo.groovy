package com.istar.mediabroken.repo

import com.istar.mediabroken.entity.capture.Site
import com.istar.mediabroken.utils.EsHolder
import com.istar.mediabroken.utils.MongoHolder
import com.istar.mediabroken.utils.StringUtils
import com.istar.mediabroken.utils.UrlUtils
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.common.text.Text
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.search.highlight.HighlightField
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
@Slf4j
class RiseNewsRepo {
    @Autowired
    MongoHolder mongo
    @Autowired
    EsHolder elasticsearch
    @Value('${es.index.riseRateMonitor}')
    String index

    List getRiseNewsFromEs(List<Site> siteList, int hot, Date startTime, Date endTime,
                           int orientation, boolean hasPic, int order, String keyWords, int pageSize, int offset) {
        String[] includeList = ["id", "author", "poster", "siteName", "contentAbstract", "title", "cover", "imgUrls", "reprintCount", "captureTime", "url", "firstPublishTime", "firstPublishSiteName", "simhash", "publishTime", "riseRate", "createTime"]
        String[] excludeList = []

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder()

        BoolQueryBuilder siteQueryBuilder = new BoolQueryBuilder()
        siteList.each { site ->
            switch (site.siteType) {
                case Site.SITE_TYPE_WEBSITE:
                    siteQueryBuilder.should(QueryBuilders.boolQuery()
                            .must(QueryBuilders.prefixQuery("domainReverseUrl", (null == site.domainReverse || site.domainReverse == "") ? UrlUtils.getReverseDomainFromUrl(site.websiteDomain) : site.domainReverse))
                            .must(QueryBuilders.termQuery("newsType", 1)))
                    break

                case Site.SITE_TYPE_WECHAT:
                    siteQueryBuilder.should(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("siteName", site.websiteName))
                            .must(QueryBuilders.termQuery("newsType", 6)))
                    break
            }
        }
        if (siteList.size() > 0) {
            boolQueryBuilder.must(siteQueryBuilder)
        }

        if (null != startTime || null != endTime) {
            RangeQueryBuilder timeRange
            if (null != startTime && null == endTime) {
                timeRange = QueryBuilders
                        .rangeQuery("createTime")
                        .gte(startTime.getTime());
            }
            if (null == startTime && null != endTime) {
                timeRange = QueryBuilders
                        .rangeQuery("createTime")
                        .lte(endTime.getTime());
            }
            if (null != startTime && null != endTime) {
                timeRange = QueryBuilders
                        .rangeQuery("createTime")
                        .gte(startTime.getTime()).lte(endTime.getTime());
            }
            boolQueryBuilder.must(timeRange)
        }

        if (!(null == keyWords || keyWords.equals(""))) {
            List wordList = keyWords.split(" ")
            BoolQueryBuilder titleSubBoolQueryBuilder = new BoolQueryBuilder()
            BoolQueryBuilder contentSubBoolQueryBuilder = new BoolQueryBuilder()
            wordList.each {
                if (it) {
                    titleSubBoolQueryBuilder.must(QueryBuilders.matchPhraseQuery("content", it))
                    contentSubBoolQueryBuilder.must(QueryBuilders.matchPhraseQuery("title", it))
                }
            }
            boolQueryBuilder.must(new BoolQueryBuilder().should(titleSubBoolQueryBuilder).should(contentSubBoolQueryBuilder))
        }

        //0?????? 1 ??? 2 ??? 3 ???
        if (hot != 0) {
            switch (hot) {
                case 1:
                    boolQueryBuilder.must(QueryBuilders
                            .rangeQuery("reprintCount")
                            .lt(10))
                    break
                case 2:
                    boolQueryBuilder.must(QueryBuilders
                            .rangeQuery("reprintCount")
                            .from(10).to(100))
                    break
                case 3:
                    boolQueryBuilder.must(QueryBuilders
                            .rangeQuery("reprintCount")
                            .gt(100))
                    break
            }
        }

        //orientation
        //0????????? 1????????? 2????????? 3????????? 4????????????
        if (orientation != 0) {
            switch (orientation) {
                case 1:
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("orientation").from(1).to(8))
                    break
                case 2:
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("orientation").lt(0))
                    break
                case 3:
                    boolQueryBuilder.must(QueryBuilders.termQuery("orientation", 0))
                    break
            }
        }

        //hasPic
        if (hasPic) {
            boolQueryBuilder.must(QueryBuilders.termQuery("hasImg", true))
        }

        SearchRequestBuilder prepareSearch = elasticsearch.client.prepareSearch(index)
                .setTypes("news")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setFetchSource(includeList, excludeList)
                .setQuery(boolQueryBuilder)
                .setFrom(offset)
                .setSize(pageSize)
                .addSort("createTime", SortOrder.DESC)
                .addSort("riseRate", SortOrder.DESC)
                .addSort("firstPublishTime", SortOrder.DESC)
        if (keyWords) {
            prepareSearch.addHighlightedField("content")
                    .addHighlightedField("title")
                    .setHighlighterPreTags("<b class='high-light'>")
                    .setHighlighterPostTags("</b>")
        }
        SearchResponse response = prepareSearch.execute().actionGet()
        def result = []
        response?.getHits()?.getHits()?.each { hit ->
            def searchResult = hit.source
            if (keyWords) {
                // ????????????????????????
                Map<String, HighlightField> highlightFields = hit.highlightFields();
                // ???????????????????????????????????????
                HighlightField titleField = highlightFields.get("title");
                if (titleField != null) {
                    // ???????????????????????????
                    Text[] titleTexts = titleField.fragments();
                    // ???title????????????????????????????????????
                    String title = "";
                    for (Text text : titleTexts) {
                        title += text;
                    }
                    searchResult.title = title
                }
                // ???????????????????????????????????????
                HighlightField contentField = highlightFields.get("content");
                if (contentField != null) {
                    // ???????????????????????????
                    Text[] contentTexts = contentField.fragments();
                    // ???title????????????????????????????????????
                    String content = "";
                    for (Text text : contentTexts) {
                        content += text;
                    }
                    Map highlightMap = StringUtils.extractHighlight(content)
                    highlightMap.each { key, value ->
                        if (keyWords.indexOf(value) == -1) {
                            content = content.replace(key as CharSequence, value as CharSequence)
                        }
                    }
                    // ???????????????????????????????????????????????????????????????
                    searchResult.contentAbstract = content
                }
            }
            result << searchResult
        }
        return result
    }
}

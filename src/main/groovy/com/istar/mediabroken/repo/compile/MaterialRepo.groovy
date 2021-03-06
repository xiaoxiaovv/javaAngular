package com.istar.mediabroken.repo.compile

import com.istar.mediabroken.entity.capture.NewsOperation
import com.istar.mediabroken.entity.compile.ArticlePush
import com.istar.mediabroken.entity.compile.Material
import com.istar.mediabroken.utils.MongoHolder
import com.mongodb.QueryBuilder
import com.mongodb.WriteResult
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

import static com.istar.mediabroken.utils.MongoHelper.toObj

/**
 * Author: Luda
 * Time: 2017/8/1
 */
@Repository
@Slf4j
class MaterialRepo {
    @Autowired
    MongoHolder mongo
    // 1文章推送 2 文章同步 3 H5发布 4预览
    public static final int ARTICLE_TYPE_PUSH = 1
    public static final int ARTICLE_TYPE_SYNC = 2
    public static final int ARTICLE_TYPE_RELEASE = 3
    public static final int ARTICLE_TYPE_PREVIEW = 4

    String addHistory(String newsAbstractId, def resMap) {

        def collection = mongo.getCollection("newsAbstract")

        def obj = collection.findAndModify(
                toObj(["_id" : newsAbstractId]), //query
                toObj(["channelResult": 1]),      //fields
                null,
                false,
                toObj([$push: ["channelResult": resMap]]),    //update
                true,
                true
        )

        return obj?.value.toString();
    }


    String addMaterial(Material material) {
        def collection = mongo.getCollection("newsAbstract")
        collection.insert(toObj(material.toMap()))
        return material.id
    }

    boolean modifyMaterial(Material material) {
        def collection = mongo.getCollection("newsAbstract")
        WriteResult result = collection.update(
                toObj([
                        userId: material.userId,
                        _id   : material.id]),
                toObj(['$set': [
                        title          : material.title,
                        author         : material.author,
                        content        : material.content,
                        contentAbstract: material.contentAbstract,
                        keywords       : material.keywords,
                        classification : material.classification,
                        source         : material.source,
                        picUrl         : material.picUrl,
                        type           : material.type,
                        userId         : material.userId,
                        orgId          : material.orgId,
                        tags           : material.tags,
                        customTags     : material.customTags,
                        updateTime     : new Date()
                ]]))
        return result.updateOfExisting

    }

    boolean modifyMaterialStatus(long userId, String id, int published) {
        def collection = mongo.getCollection("newsAbstract")
        def result = collection.update(
                toObj([
                        userId: userId,
                        _id   : id]),
                toObj(['$set': [
                        isPublished: published,
                        updateTime : new Date()
                ]]))
        return true
    }

    Material getUserMaterial(long userId, String id) {
        def collection = mongo.getCollection("newsAbstract")
        def query = ["userId": userId, "_id": id]
        def material = collection.findOne(toObj(query))
        if (material) {
            return new Material(material)
        }
        return null
    }

    List getUserMaterialList(long userId, List ids) {
        def list = new ArrayList<>()
        def collection = mongo.getCollection("newsAbstract")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("userId").is(userId)
        queryBuilder.put("_id").is(ids)
        def material = collection.find(queryBuilder.get())
        if (material) {
            list << new Material(material)
        }
        return list
    }

    Material getUserMaterial(String id) {
        def collection = mongo.getCollection("newsAbstract")
        def query = ["_id": id]
        def material = collection.findOne(toObj(query))
        if (material) {
            return new Material(material)
        }
        return null
    }

    Material getUserMaterialByTime(String id, Date endTime) {
        def collection = mongo.getCollection("newsAbstract")
        def query = [
                "_id"     : id,
                updateTime: [$gte: endTime]
        ]
        def material = collection.findOne(toObj(query))
        if (material) {
            return new Material(material)
        }
        return null
    }

    List getUserMaterials(long userId, String queryKeyWords, int isPublished, int isReleased, int pageSize, int pageNo) {
        def collection = mongo.getCollection("newsAbstract")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("userId").is(userId)
        if (isPublished == 2)//false
        {
            queryBuilder.put("isPublished").in([false, 2])
        } else if (isPublished == 1)//true
        {
            queryBuilder.put("isPublished").in([true, 1])
        } else if (isPublished == 4) {
            queryBuilder.put("isPublished").is(4)
        }

        if (isReleased == 1)//true
        {
            queryBuilder.put("isReleased").is(true)
        } else if (isReleased == 2)//true
        {
            queryBuilder.put("isReleased").notEquals(true)
        }

        if (queryKeyWords) {
            queryBuilder.put("title").regex(~/.*(?i)${queryKeyWords}.*/)
        }
        def cursor = collection.find(queryBuilder.get()).sort(toObj([updateTime: -1])).hint("userId_1").skip(pageSize * (pageNo - 1)).limit(pageSize)
        def result = []
        while (cursor.hasNext()) {
            def material = cursor.next()
            result << new Material(material)
        }
        cursor.close()
        return result
    }


    int getUserMaterialCount(long userId, String queryKeyWords, int isPublished, int isReleased) {
        def collection = mongo.getCollection("newsAbstract")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("userId").is(userId)
        if (isPublished == 2)//false  //未发
        {
            queryBuilder.put("isPublished").in([false, 2])
        } else if (isPublished == 1)//true 已
        {
            queryBuilder.put("isPublished").in([true, 1])
        } else if (isPublished == 4) {
            queryBuilder.put("isPublished").is(4)
        }

        if (isReleased == 1)//true
        {
            queryBuilder.put("isReleased").is(true)
        } else if (isReleased == 2)//true
        {
            queryBuilder.put("isReleased").notEquals(true)
        }
        if (queryKeyWords) {
            queryBuilder.put("title").regex(~/.*(?i)${queryKeyWords}.*/)
        }
        def result = collection.find(queryBuilder.get()).count()
        return result
    }

    void removeMaterial(long userId, String materialId) {
        def collection = mongo.getCollection("newsAbstract")
        collection.remove(toObj([userId: userId, _id: materialId]))
    }

    def removeMaterialList(long userId, List materialIds) {
        def collection = mongo.getCollection("newsAbstract")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("userId").is(userId)
        queryBuilder.put("_id").in(materialIds)
        def remove = collection.remove(queryBuilder.get())
    }

    String addArticlePush(ArticlePush articlePush) {
        def collection = mongo.getCollection("articleOperation")
        Map articleOperation = articlePush.toMap();
        articleOperation.operationType = 1;// 1文章推送 2 文章同步
        collection.insert(toObj(articleOperation))
        return articlePush.id
    }

    ArticlePush getUserArticlePush(long userId, String id) {
        def collection = mongo.getCollection("articleOperation")
        def query = ["userId": userId, "_id": id]
        def articlePush = collection.findOne(toObj(query))
        if (articlePush) {
            return new ArticlePush(articlePush)
        }
        return null
    }

    def getArticleOperationById(String operationId) {
        def collection = mongo.getCollection("articleOperation")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("_id").is(operationId)
        def articleOperation = collection.findOne(queryBuilder.get())
        if (articleOperation) {
            return articleOperation
        }
        return null
    }

    String addArticle(def article) {
        def collection = mongo.getCollection("articleOperation")
        article._id = UUID.randomUUID().toString()
        Map articleOperation = article
        collection.insert(toObj(articleOperation))
        return article._id
    }

    String importArticle(NewsOperation newsOperation) {
        def collection = mongo.getCollection("newsOperation")
        collection.insert(toObj(newsOperation.toMap()))
        return newsOperation._id
    }

    NewsOperation getUserNewsOperationById(String newsOperationId) {
        def collection = mongo.getCollection("newsOperation")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("_id").is(newsOperationId)
        def newsOperation = collection.findOne(queryBuilder.get())
        if (newsOperation) {
            return new NewsOperation(newsOperation)
        }
        return null
    }

    List queryUserMaterials(long userId) {
        def collection = mongo.getCollection("newsAbstract")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("userId").is(userId)
        def cursor = collection.find(queryBuilder.get(), toObj(["_id": 1, "title": 1, "contentAbstract": 1, "createTime": 1, "updateTime": 1])).sort(toObj([updateTime: -1]))
        def result = []
        while (cursor.hasNext()) {
            def material = cursor.next()
            result << new Material(material)
        }
        cursor.close()
        return result
    }
    //查articleOperation
    List<ArticlePush> getArticleOperationInDate(long userId, int operationType, Date startDate, Date endDate) {
        def collection = mongo.getCollection("articleOperation")
        def query = toObj(['userId': userId, 'operationType': operationType, 'createTime': [$gt: startDate, $lt: endDate]])
        def cursor = collection.find(query);
        def result = []
        while (cursor.hasNext()) {
            def articleOperation = cursor.next()
            result << new ArticlePush(toObj(articleOperation))
        }
        cursor.close()
        return result
    }

    def addArticleOperation(def article) {
        def collection = mongo.getCollection("articleOperation")
        article._id = UUID.randomUUID().toString()
        Map articleOperation = article
        collection.insert(toObj(articleOperation))
        return article
    }

    def modifyMaterialReleased(long userId, def material, String articleOperationId) {
        def collection = mongo.getCollection("newsAbstract")
        def result = collection.update(
                toObj([
                        userId: userId,
                        _id   : material.id]),
                toObj(['$set': [
                        isReleased: true,
                        articleId : articleOperationId
                ]]))
        return result.updateOfExisting
    }

    def modifyMaterialPush(long userId, def material, String articleOperationId) {
        def collection = mongo.getCollection("newsAbstract")
        def result = collection.update(
                toObj([
                        userId: userId,
                        _id   : material.id]),
                toObj(['$set': [
                        articleId : articleOperationId
                ]]))
        return result.updateOfExisting
    }

    def modifyArticleOperationMaterial(long userId, Map material, int operationType, String articleOperationId) {
        def collection = mongo.getCollection("articleOperation")
        def result = collection.update(
                toObj([
                        userId       : userId,
                        operationType: operationType,
                        _id          : articleOperationId]),
                toObj(['$set': [
                        material  : material,
                        updateTime: new Date(),
                ]]), false, true)
        return result.updateOfExisting
    }

    def updateArticleOperationMaterial(long userId, Map material, int operationType, String articleOperationId) {
        def collection = mongo.getCollection("articleOperation")
        def result = collection.update(
                toObj([
                        userId       : userId,
                        operationType: operationType,
                        _id          : articleOperationId]),
                toObj(['$set': [
                        material: material,
                ]]), false, true)
        return result.updateOfExisting
    }

    def getArticleOperationByIdAndType(long userId, int operationType, String materialId) {
        def collection = mongo.getCollection("articleOperation")
        def query = [
                "userId"       : userId,
                "operationType": operationType,
                "material._id" : materialId
        ]
        def articleOperation = collection.findOne(toObj(query))
        if (articleOperation) {
            return new ArticlePush(articleOperation)
        }
        return null
    }

    def getArticleOperationByTime(String id, Date endTime) {
        def collection = mongo.getCollection("articleOperation")
        def query = [
                "_id"     : id,
                updateTime: [$gte: endTime]
        ]
        def articleOperation = collection.findOne(toObj(query))
        if (articleOperation) {
            return new ArticlePush(articleOperation)
        }
        return null
    }

    def removeArticleOperation(long userId, String id) {
        def collection = mongo.getCollection("articleOperation")
        def query = [
                "userId": userId,
                "_id"   : id

        ]
        def articleOperation = collection.remove(toObj(query))
        return articleOperation.updateOfExisting
    }

    def removeArticleOperationList(long userId, List ids) {
        def collection = mongo.getCollection("articleOperation")
        QueryBuilder queryBuilder = QueryBuilder.start()
        queryBuilder.put("userId").is(userId)
        queryBuilder.put("_id").in(ids)
        def remove = collection.remove(queryBuilder.get())
        return remove.updateOfExisting
    }
}

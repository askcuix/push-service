package io.askcuix.push.persist;

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import io.askcuix.push.common.Constant;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * 订阅topic。
 * <p>
 * 索引：
 * <p>
 * db.subscribe_topic.createIndex( { "topic": 1 } )
 * db.subscribe_topic.createIndex( { "topic": 1, "fid": 1 }, { unique: true } )
 * <p>
 * Created by Chris on 15/12/7.
 */
@Repository
public class SubscribeDao {
    private static final Logger logger = LoggerFactory.getLogger(SubscribeDao.class);
    private final static Logger monitorLogger = LoggerFactory.getLogger(Constant.LOG_MONITOR);

    private static final String COLLECTION_TOPIC_SUBSCRIBE = "subscribe_topic";

    private static final String FIELD_TOPIC = "topic";
    private static final String FIELD_FID = "fid";

    private static final long MONITOR_THRESHOLDS = 1000L;

    @Autowired
    private MongoTemplate mongoTemplate;

    public boolean subscribe(String topic, String uid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            Document dbObj = new Document();
            dbObj.put(FIELD_TOPIC, topic);
            dbObj.put(FIELD_FID, uid);
            dbObj.put(MongoTemplate.FIELD_CREATE_TIME, System.currentTimeMillis());

            mongoTemplate.getCollection(COLLECTION_TOPIC_SUBSCRIBE).insertOne(dbObj);
        } catch (DuplicateKeyException e) {
            logger.warn("Subscribe relation exist. topic: " + topic + ", id: " + uid, e);
            return false;
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] subscribe - topic: {}, uid: {}, cost time: {}ms", topic, uid, costTime);
            }
        }

        return true;
    }

    public boolean unsubscribe(String topic, String uid) throws Exception {
        long start = System.currentTimeMillis();

        try {

            mongoTemplate.getCollection(COLLECTION_TOPIC_SUBSCRIBE).deleteOne(and(eq(FIELD_TOPIC, topic), eq(FIELD_FID, uid)));
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] unsubscribe - topic: {}, uid: {}, cost time: {}ms", topic, uid, costTime);
            }
        }

        return true;
    }

    public List<String> getUsersByTopic(String topic, String lastUid, int limit) {
        long start = System.currentTimeMillis();
        List<String> uidList = new ArrayList<String>();

        try {
            List<Bson> filters = Lists.newArrayList();
            filters.add(eq(FIELD_TOPIC, topic));

            if (!StringUtils.isBlank(lastUid) && !"0".equals(lastUid)) {
                filters.add(lt(FIELD_FID, lastUid));
            }

            FindIterable<Document> findIterable = mongoTemplate.getCollection(COLLECTION_TOPIC_SUBSCRIBE).find(and(filters));
            if (limit > 0) {
                findIterable.limit(limit);
            }

            BasicDBObject sorter = new BasicDBObject(FIELD_FID, -1);
            findIterable.sort(sorter);

            MongoCursor<Document> cursor = findIterable.iterator();

            while (cursor.hasNext()) {
                Document obj = cursor.next();
                uidList.add((String) obj.get(FIELD_FID));
            }
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] getUsersByTopic - topic: {}, lastUid: {}, limit: {}, cost time: {}ms", topic, lastUid, limit, costTime);
            }
        }

        return uidList;
    }

}

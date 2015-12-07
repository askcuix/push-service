package io.askcuix.push.persist;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import io.askcuix.push.common.Constant;
import io.askcuix.push.entity.UserPushInfo;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * 用户push信息.
 * <p>
 * 索引：
 * <p>
 * db.user_push_info.createIndex( { "_id": 1, "os": 1, "notifySysType": 1 } )
 * db.user_push_info.createIndex( { "_id": 1, "os": 1, "pushSysType": 1 } )
 * db.user_push_info.createIndex( { "pushSysType": 1, "pushId": 1 } )
 * <p>
 * Created by Chris on 15/12/3.
 */
@Repository
public class UserPushInfoDao {
    private final static Logger monitorLogger = LoggerFactory.getLogger(Constant.LOG_MONITOR);

    private static final String COLLETION_PUSH_INFO_NAME = "user_push_info";

    private static final String FIELD_OS = "os";
    private static final String FIELD_NOTIFY_SYS_TYPE = "notifySysType";
    private static final String FIELD_NOTIFY_ID = "notifyId";
    private static final String FIELD_PUSH_SYS_TYPE = "pushSysType";
    private static final String FIELD_PUSH_ID = "pushId";

    private static final long MONITOR_THRESHOLDS = 1000L;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveUserInfo(UserPushInfo userInfo) throws Exception {
        if (userInfo == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            Document dbObj = new Document();
            dbObj.put(MongoTemplate.FIELD_OBJ_ID, userInfo.getUid());
            dbObj.put(FIELD_OS, userInfo.getOs());

            dbObj.put(FIELD_NOTIFY_SYS_TYPE, userInfo.getNotifySysType());
            if (StringUtils.isNotBlank(userInfo.getNotifyId())) {
                dbObj.put(FIELD_NOTIFY_ID, userInfo.getNotifyId());
            } else {
                dbObj.put(FIELD_NOTIFY_ID, "");
            }

            dbObj.put(FIELD_PUSH_SYS_TYPE, userInfo.getPushSysType());
            if (StringUtils.isNotBlank(userInfo.getPushId())) {
                dbObj.put(FIELD_PUSH_ID, userInfo.getPushId());
            } else {
                dbObj.put(FIELD_PUSH_ID, "");
            }
            dbObj.put(MongoTemplate.FIELD_CREATE_TIME, userInfo.getCreateTime());
            dbObj.put(MongoTemplate.FIELD_UPDATE_TIME, userInfo.getUpdateTime());

            BasicDBObject filter = new BasicDBObject(MongoTemplate.FIELD_OBJ_ID, userInfo.getUid());

            mongoTemplate.getCollection(COLLETION_PUSH_INFO_NAME).replaceOne(filter, dbObj, new UpdateOptions().upsert(true));
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] saveUserInfo - userInfo: {}, cost time: {}ms", userInfo, costTime);
            }
        }
    }

    public UserPushInfo findByUid(String uid) throws Exception {
        if (StringUtils.isBlank(uid)) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            UserPushInfo pushInfo = null;

            MongoCursor<Document> cursor = mongoTemplate.getCollection(COLLETION_PUSH_INFO_NAME).find(new BasicDBObject(MongoTemplate.FIELD_OBJ_ID, uid)).iterator();

            while (cursor.hasNext()) {
                Document dbObj = cursor.next();
                pushInfo = convertFromDBObject(dbObj);

                break;
            }

            return pushInfo;
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] findByUid - uid: {}, cost time: {}ms", uid, costTime);
            }
        }
    }

    public List<UserPushInfo> findByUids(List<String> uidList) throws Exception {
        if (uidList == null || uidList.isEmpty()) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            return queryUsers(in(MongoTemplate.FIELD_OBJ_ID, uidList), -1);
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] findByUids - uidList size: {}, cost time: {}ms", uidList.size(), costTime);
            }
        }
    }

    public List<UserPushInfo> findUsers(List<String> uidList, int os, boolean isNotify, int pushSysType) throws Exception {
        if (uidList == null || uidList.isEmpty()) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            Bson uidCon = in(MongoTemplate.FIELD_OBJ_ID, uidList);
            Bson osCon = eq(FIELD_OS, os);

            String sysType = null;
            if (isNotify) {
                sysType = FIELD_NOTIFY_SYS_TYPE;
            } else {
                sysType = FIELD_PUSH_SYS_TYPE;
            }
            Bson sysCon = eq(sysType, pushSysType);

            return queryUsers(and(uidCon, osCon, sysCon), -1);
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] findUsers - uidList size: {}, os: {}, isNotify: {}, pushSysType: {} cost time: {}ms", uidList.size(), os, isNotify, pushSysType, costTime);
            }
        }
    }

    public List<UserPushInfo> findUsers(String lastUid, int os, boolean isNotify, int pushSysType, int limit) throws Exception {
        long start = System.currentTimeMillis();

        try {
            List<Bson> filters = Lists.newArrayList(eq(FIELD_OS, os));

            if (!StringUtils.isBlank(lastUid) && !"0".equals(lastUid)) {
                filters.add(lt(MongoTemplate.FIELD_OBJ_ID, lastUid));
            }

            String sysType = null;
            if (isNotify) {
                sysType = FIELD_NOTIFY_SYS_TYPE;
            } else {
                sysType = FIELD_PUSH_SYS_TYPE;
            }
            filters.add(eq(sysType, pushSysType));

            return queryUsers(and(filters), limit);
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] findUsers - lastUid: {}, os: {}, isNotify: {}, pushSysType: {}, limit: {}, cost time: {}ms", lastUid, os, isNotify, pushSysType, limit, costTime);
            }
        }
    }

    private List<UserPushInfo> queryUsers(Bson query, int limit) {
        FindIterable<Document> findIterable = mongoTemplate.getCollection(COLLETION_PUSH_INFO_NAME).find(query);
        if (limit > 0) {
            findIterable.limit(limit);
        }

        BasicDBObject sorter = new BasicDBObject(MongoTemplate.FIELD_OBJ_ID, -1);
        findIterable.sort(sorter);

        MongoCursor<Document> cursor = findIterable.iterator();

        List<UserPushInfo> userInfoList = Lists.newArrayList();

        while (cursor.hasNext()) {
            Document obj = cursor.next();
            userInfoList.add(convertFromDBObject(obj));
        }

        return userInfoList;
    }

    public void removeUserInfo(String uid) throws Exception {
        if (StringUtils.isBlank(uid)) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            mongoTemplate.getCollection(COLLETION_PUSH_INFO_NAME).deleteOne(eq(MongoTemplate.FIELD_OBJ_ID, uid));
        } finally {
            long costTime = System.currentTimeMillis() - start;

            if (costTime > MONITOR_THRESHOLDS) {
                monitorLogger.info("[MongoDB] removeUserInfo - uid: {}, cost time: {}ms", uid, costTime);
            }
        }
    }

    private UserPushInfo convertFromDBObject(Document dbObj) {
        if (dbObj == null) {
            return null;
        }

        UserPushInfo userInfo = new UserPushInfo();
        userInfo.setUid(dbObj.get(mongoTemplate.FIELD_OBJ_ID).toString());
        userInfo.setOs(Integer.parseInt(dbObj.get(FIELD_OS).toString()));

        userInfo.setNotifySysType(Integer.parseInt(dbObj.get(FIELD_NOTIFY_SYS_TYPE).toString()));
        Object notifyIdObj = dbObj.get(FIELD_NOTIFY_ID);
        if (notifyIdObj != null) {
            userInfo.setNotifyId(notifyIdObj.toString());
        }

        userInfo.setPushSysType(Integer.parseInt(dbObj.get(FIELD_PUSH_SYS_TYPE).toString()));
        Object pushIdObj = dbObj.get(FIELD_PUSH_ID);
        if (pushIdObj != null) {
            userInfo.setPushId(pushIdObj.toString());
        }

        return userInfo;
    }
}

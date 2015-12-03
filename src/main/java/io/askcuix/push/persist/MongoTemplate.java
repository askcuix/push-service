package io.askcuix.push.persist;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Chris on 15/11/30.
 */
public class MongoTemplate {
    private static final Logger logger = LoggerFactory.getLogger(MongoTemplate.class);

    // Operators
    public static final String OP_INC = "$inc";
    public static final String OP_PUSH = "$push";
    public static final String OP_EACH = "$each";
    public static final String OP_SLICE = "$slice";
    public static final String OP_SET = "$set";
    public static final String OP_UNSET = "$unset";
    public static final String OP_MATCH = "$match";
    public static final String OP_GROUP = "$group";
    public static final String OP_SORT = "$sort";
    public static final String OP_SUM = "$sum";
    public static final String OP_LT = "$lt";
    public static final String OP_LTE = "$lte";
    public static final String OP_GTE = "$gte";
    public static final String OP_FIRST = "$first";
    public static final String OP_LAST = "$last";
    public static final String OP_MAX = "$max";
    public static final String OP_MIN = "$min";
    public static final String OP_LIMIT = "$limit";

    // General Field
    public static final String FIELD_OBJ_ID = "_id";
    public static final String FIELD_CREATE_TIME = "createTime";
    public static final String FIELD_UPDATE_TIME = "updateTime";

    private MongoClient mongoClient;
    private String database;

    public MongoTemplate(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDatabase getDB() {
        if (StringUtils.isBlank(database)) {
            logger.error("database not configured!");
            return null;
        }
        return mongoClient.getDatabase(database);
    }

    public MongoDatabase getDB(String dbname) {
        if (StringUtils.isBlank(dbname)) {
            logger.error("dbname is empty!");
            return null;
        }

        return mongoClient.getDatabase(dbname);
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        MongoDatabase db = getDB();

        if (db == null) {
            throw new IllegalStateException("Default database not exists!");
        }

        return db.getCollection(collectionName);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}

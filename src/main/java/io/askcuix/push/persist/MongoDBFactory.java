package io.askcuix.push.persist;

import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chris on 15/11/27.
 */
public class MongoDBFactory implements FactoryBean<MongoClient>, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBFactory.class);

    private List<ServerAddress> mongoServers = new ArrayList<ServerAddress>();

    private static final MongoClientOptions DEFAULT_MONGO_OPTIONS = MongoClientOptions.builder().build();

    private ReadPreference readPreference = DEFAULT_MONGO_OPTIONS.getReadPreference();
    private WriteConcern writeConcern = DEFAULT_MONGO_OPTIONS.getWriteConcern();
    private int maxConnectionsPerHost = DEFAULT_MONGO_OPTIONS.getConnectionsPerHost();
    private int threadsAllowedToBlockForConnectionMultiplier = DEFAULT_MONGO_OPTIONS.getThreadsAllowedToBlockForConnectionMultiplier();
    private int maxWaitTime = DEFAULT_MONGO_OPTIONS.getMaxWaitTime();
    private int maxConnectionIdleTime = DEFAULT_MONGO_OPTIONS.getMaxConnectionIdleTime();
    private int maxConnectionLifeTime = DEFAULT_MONGO_OPTIONS.getMaxConnectionLifeTime();
    private int connectTimeout = DEFAULT_MONGO_OPTIONS.getConnectTimeout();
    private int socketTimeout = DEFAULT_MONGO_OPTIONS.getSocketTimeout();
    private boolean socketKeepAlive = DEFAULT_MONGO_OPTIONS.isSocketKeepAlive();

    private MongoClient mongoClient;

    public MongoDBFactory(String mongoAddress) {
        String[] mongoArray = StringUtils.split(mongoAddress, ",");
        if (mongoArray.length < 1) {
            throw new IllegalArgumentException("No available MongoDB server.");
        }

        for (String server : mongoArray) {
            String[] serverInfo = StringUtils.split(server.trim(), ":");
            if (serverInfo.length != 2) {
                continue;
            }

            String host = serverInfo[0].trim();
            int port = Integer.parseInt(serverInfo[1].trim());

            ServerAddress serverAdd = new ServerAddress(host, port);
            mongoServers.add(serverAdd);
        }

        if (mongoServers.isEmpty()) {
            throw new IllegalArgumentException("No valid MongoDB server.");
        }
    }


    @Override
    public MongoClient getObject() throws Exception {
        MongoClientOptions mongoClientOptions = MongoClientOptions.builder()
                .readPreference(readPreference).writeConcern(writeConcern)
                .connectionsPerHost(maxConnectionsPerHost)
                .threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier)
                .maxWaitTime(maxWaitTime)
                .maxConnectionIdleTime(maxConnectionIdleTime).maxConnectionLifeTime(maxConnectionLifeTime)
                .connectTimeout(connectTimeout)
                .socketTimeout(socketTimeout).socketKeepAlive(socketKeepAlive)
                .build();

        mongoClient = new MongoClient(mongoServers, mongoClientOptions);

        return mongoClient;
    }

    @Override
    public Class<MongoClient> getObjectType() {
        return MongoClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public ReadPreference getReadPreference() {
        return readPreference;
    }

    public void setReadPreference(ReadPreference readPreference) {
        this.readPreference = readPreference;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getThreadsAllowedToBlockForConnectionMultiplier() {
        return threadsAllowedToBlockForConnectionMultiplier;
    }

    public void setThreadsAllowedToBlockForConnectionMultiplier(int threadsAllowedToBlockForConnectionMultiplier) {
        this.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockForConnectionMultiplier;
    }

    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public int getMaxConnectionIdleTime() {
        return maxConnectionIdleTime;
    }

    public void setMaxConnectionIdleTime(int maxConnectionIdleTime) {
        this.maxConnectionIdleTime = maxConnectionIdleTime;
    }

    public int getMaxConnectionLifeTime() {
        return maxConnectionLifeTime;
    }

    public void setMaxConnectionLifeTime(int maxConnectionLifeTime) {
        this.maxConnectionLifeTime = maxConnectionLifeTime;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public boolean isSocketKeepAlive() {
        return socketKeepAlive;
    }

    public void setSocketKeepAlive(boolean socketKeepAlive) {
        this.socketKeepAlive = socketKeepAlive;
    }
}

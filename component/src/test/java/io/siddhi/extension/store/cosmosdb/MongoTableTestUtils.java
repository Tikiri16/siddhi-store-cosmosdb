/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.extension.store.cosmosdb;

import com.cosmosdb.CosmosClient;
import com.cosmosdb.CosmosClientURI;
import com.cosmosdb.CosmosException;
import com.cosmosdb.client.ListIndexesIterable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CosmosTableTestUtils {

    private static final Log log = LogFactory.getLog(CosmosTableTestUtils.class);
    private static final String COSMOS_CLIENT_URI =
            "cosmosdb://{{cosmos.credentials}}{{cosmos.servers}}/{{cosmos.database}}";
    private static String databaseName = "admin";

    private CosmosTableTestUtils() {
    }

    public static String resolveBaseUri(String uri) {
        return uri
                .replace("{{cosmos.credentials}}", getCosmosCredentials())
                .replace("{{cosmos.servers}}", getAddressOfContainers())
                .replace("{{cosmos.database}}", getCosmosDatabaseName());
    }

    public static String resolveBaseUri() {
        return resolveBaseUri(COSMOS_CLIENT_URI);
    }

    private static String getAddressOfContainers() {
        String cosmosServers = System.getProperty("cosmos.servers");
        if (!isEmpty(cosmosServers)) {
            return cosmosServers;
        } else {
            return "172.17.0.2:27017";
        }
    }

    private static String getCosmosCredentials() {
        String cosmosUsername = System.getProperty("cosmos.username");
        String cosmosPassword = System.getProperty("cosmos.password");
        if (!isEmpty(cosmosUsername) && !isEmpty(cosmosPassword)) {
            return cosmosUsername + ":" + cosmosPassword + "@";
        } else {
            if (!isEmpty(cosmosUsername)) {
                return cosmosUsername + "@";
            }
            return "";
        }
    }

    private static String getCosmosDatabaseName() {
        String cosmosDatabaseName = System.getProperty("cosmos.database.name");
        if (!isEmpty(cosmosDatabaseName)) {
            databaseName = cosmosDatabaseName;
        }
        return databaseName;
    }

    private static boolean isEmpty(String field) {
        return (field == null || field.trim().length() == 0);
    }

    public static void dropCollection(String uri, String collectionName) {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri))) {
            cosmosClient.getDatabase(databaseName).getCollection(collectionName).drop();
        } catch (CosmosException e) {
            log.debug("Clearing DB collection failed due to " + e.getMessage(), e);
            throw e;
        }
    }

    public static long getDocumentsCount(String uri, String collectionName) {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri))) {
            return cosmosClient.getDatabase(databaseName).getCollection(collectionName).count();
        } catch (CosmosException e) {
            log.debug("Getting rows in DB table failed due to " + e.getMessage(), e);
            throw e;
        }
    }

    public static boolean doesCollectionExists(String uri, String customCollectionName) {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri))) {
            for (String collectionName : cosmosClient.getDatabase(databaseName).listCollectionNames()) {
                if (customCollectionName.equals(collectionName)) {
                    return true;
                }
            }
            return false;
        } catch (CosmosException e) {
            log.debug("Checking whether collection was created failed due to" + e.getMessage(), e);
            throw e;
        }
    }

    private static List<Document> getIndexList(String uri, String collectionName) {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri))) {
            ListIndexesIterable<Document> existingIndexesIterable =
                    cosmosClient.getDatabase(databaseName).getCollection(collectionName).listIndexes();
            List<Document> existingIndexDocuments = new ArrayList<>();
            existingIndexesIterable.forEach((Consumer<? super Document>) existingIndex -> {
                existingIndex.remove("ns");
                existingIndexDocuments.add(existingIndex);
            });
            return existingIndexDocuments;
        } catch (CosmosException e) {
            log.debug("Getting indexes in DB table failed due to " + e.getMessage(), e);
            throw e;
        }
    }

    public static Document getIndex(String uri, String collectionName, String indexName) {
        try {
            List<Document> existingIndexList = getIndexList(uri, collectionName);
            for (Document existingIndex : existingIndexList) {
                if (existingIndex.get("name").equals(indexName)) {
                    return existingIndex;
                }
            }
            return null;
        } catch (CosmosException e) {
            log.debug("Getting indexes in DB table failed due to " + e.getMessage(), e);
            throw e;
        }
    }

    public static Document getDocument(String uri, String collectionName, String findFilter) {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri))) {
            Document findFilterDocument = Document.parse(findFilter);
            Document firstFind = cosmosClient.getDatabase(databaseName).getCollection(collectionName)
                    .find(findFilterDocument).first();
            firstFind.remove("_id");
            return firstFind;
        } catch (CosmosException e) {
            log.debug("Getting indexes in DB table failed due to " + e.getMessage(), e);
            throw e;
        }
    }

    public static void createCollection(String uri, String collectionName) {
        dropCollection(uri, collectionName);
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri))) {
            cosmosClient.getDatabase(databaseName).createCollection(collectionName);
        } catch (CosmosException e) {
            log.debug("Getting indexes in DB table failed due to " + e.getMessage(), e);
            throw e;
        }
    }
}



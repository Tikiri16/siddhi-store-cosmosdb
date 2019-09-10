/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.siddhi.extension.store.cosmosdb;

import com.cosmosdb.CosmosBulkWriteException;
import com.cosmosdb.CosmosClient;
import com.cosmosdb.CosmosClientOptions;
import com.cosmosdb.CosmosClientURI;
import com.cosmosdb.CosmosException;
import com.cosmosdb.CosmosSocketOpenException;
import com.cosmosdb.client.CosmosCollection;
import com.cosmosdb.client.CosmosCursor;
import com.cosmosdb.client.CosmosDatabase;
import com.cosmosdb.client.model.DeleteManyModel;
import com.cosmosdb.client.model.IndexModel;
import com.cosmosdb.client.model.InsertOneModel;
import com.cosmosdb.client.model.UpdateManyModel;
import com.cosmosdb.client.model.UpdateOptions;
import com.cosmosdb.client.model.WriteModel;
import io.siddhi.annotation.Example;
import io.siddhi.annotation.Extension;
import io.siddhi.annotation.Parameter;
import io.siddhi.annotation.SystemParameter;
import io.siddhi.annotation.util.DataType;
import io.siddhi.core.exception.ConnectionUnavailableException;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.table.record.AbstractRecordTable;
import io.siddhi.core.table.record.ExpressionBuilder;
import io.siddhi.core.table.record.RecordIterator;
import io.siddhi.core.util.collection.operator.CompiledCondition;
import io.siddhi.core.util.collection.operator.CompiledExpression;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.extension.store.cosmosdb.exception.CosmosTableException;
import io.siddhi.extension.store.cosmosdb.util.CosmosTableConstants;
import io.siddhi.extension.store.cosmosdb.util.CosmosTableUtils;
import io.siddhi.query.api.annotation.Annotation;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.query.api.definition.TableDefinition;
import io.siddhi.query.api.util.AnnotationHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.siddhi.core.util.SiddhiConstants.ANNOTATION_INDEX_BY;
import static io.siddhi.core.util.SiddhiConstants.ANNOTATION_PRIMARY_KEY;
import static io.siddhi.core.util.SiddhiConstants.ANNOTATION_STORE;


/**
 * Class representing CosmosDB Event Table implementation.
 */
@Extension(
        name = "cosmosdb",
        namespace = "store",
        description = "Using this extension a CosmosDB Event Table can be configured to persist events " +
                "in a CosmosDB of user's choice.",
        parameters = {
                @Parameter(name = "cosmosdb.uri",
                        description = "The CosmosDB URI for the CosmosDB data store. The uri must be of the format \n" +
                                "cosmosdb://[username:password@]host1[:port1][,hostN[:portN]][/[database][?options]]\n" +
                                "The options specified in the uri will override any connection options specified in " +
                                "the deployment yaml file.",
                        type = {DataType.STRING}),
                @Parameter(name = "collection.name",
                        description = "The name of the collection in the store this Event Table should" +
                                " be persisted as.",
                        optional = true,
                        defaultValue = "Name of the siddhi event table.",
                        type = {DataType.STRING}),
                @Parameter(name = "secure.connection",
                        description = "Describes enabling the SSL for the cosmosdb connection",
                        optional = true,
                        defaultValue = "false",
                        type = {DataType.STRING}),
                @Parameter(name = "trust.store",
                        description = "File path to the trust store.",
                        optional = true,
                        defaultValue = "${carbon.home}/resources/security/client-truststore.jks",
                        type = {DataType.STRING}),
                @Parameter(name = "trust.store.password",
                        description = "Password to access the trust store",
                        optional = true,
                        defaultValue = "wso2carbon",
                        type = {DataType.STRING}),
                @Parameter(name = "key.store",
                        description = "File path to the keystore.",
                        optional = true,
                        defaultValue = "${carbon.home}/resources/security/client-truststore.jks",
                        type = {DataType.STRING}),
                @Parameter(name = "key.store.password",
                        description = "Password to access the keystore",
                        optional = true,
                        defaultValue = "wso2carbon",
                        type = {DataType.STRING})
        },
        systemParameter = {
                @SystemParameter(name = "applicationName",
                        description = "Sets the logical name of the application using this CosmosClient. The " +
                                "application name may be used by the client to identify the application to " +
                                "the server, for use in server logs, slow query logs, and profile collection.",
                        defaultValue = "null",
                        possibleParameters = "the logical name of the application using this CosmosClient. The " +
                                "UTF-8 encoding may not exceed 128 bytes."),
                @SystemParameter(name = "cursorFinalizerEnabled",
                        description = "Sets whether cursor finalizers are enabled.",
                        defaultValue = "true",
                        possibleParameters = {"true", "false"}),
                @SystemParameter(name = "requiredReplicaSetName",
                        description = "The name of the replica set",
                        defaultValue = "null",
                        possibleParameters = "the logical name of the replica set"),
                @SystemParameter(name = "sslEnabled",
                        description = "Sets whether to initiate connection with TSL/SSL enabled. true: Initiate " +
                                "the connection with TLS/SSL. false: Initiate the connection without TLS/SSL.",
                        defaultValue = "false",
                        possibleParameters = {"true", "false"}),
                @SystemParameter(name = "trustStore",
                        description = "File path to the trust store.",
                        defaultValue = "${carbon.home}/resources/security/client-truststore.jks",
                        possibleParameters = "Any valid file path."),
                @SystemParameter(name = "trustStorePassword",
                        description = "Password to access the trust store",
                        defaultValue = "wso2carbon",
                        possibleParameters = "Any valid password."),
                @SystemParameter(name = "keyStore",
                        description = "File path to the keystore.",
                        defaultValue = "${carbon.home}/resources/security/client-truststore.jks",
                        possibleParameters = "Any valid file path."),
                @SystemParameter(name = "keyStorePassword",
                        description = "Password to access the keystore",
                        defaultValue = "wso2carbon",
                        possibleParameters = "Any valid password."),
                @SystemParameter(name = "connectTimeout",
                        description = "The time in milliseconds to attempt a connection before timing out.",
                        defaultValue = "10000",
                        possibleParameters = "Any positive integer"),
                @SystemParameter(name = "connectionsPerHost",
                        description = "The maximum number of connections in the connection pool.",
                        defaultValue = "100",
                        possibleParameters = "Any positive integer"),
                @SystemParameter(name = "minConnectionsPerHost",
                        description = "The minimum number of connections in the connection pool.",
                        defaultValue = "0",
                        possibleParameters = "Any natural number"),
                @SystemParameter(name = "maxConnectionIdleTime",
                        description = "The maximum number of milliseconds that a connection can remain idle in " +
                                "the pool before being removed and closed. A zero value indicates no limit to " +
                                "the idle time.  A pooled connection that has exceeded its idle time will be " +
                                "closed and replaced when necessary by a new connection.",
                        defaultValue = "0",
                        possibleParameters = "Any positive integer"),
                @SystemParameter(name = "maxWaitTime",
                        description = "The maximum wait time in milliseconds that a thread may wait for a connection " +
                                "to become available. A value of 0 means that it will not wait.  A negative value " +
                                "means to wait indefinitely",
                        defaultValue = "120000",
                        possibleParameters = "Any integer"),
                @SystemParameter(name = "threadsAllowedToBlockForConnectionMultiplier",
                        description = "The maximum number of connections allowed per host for this CosmosClient " +
                                "instance. Those connections will be kept in a pool when idle. Once the pool " +
                                "is exhausted, any operation requiring a connection will block waiting for an " +
                                "available connection.",
                        defaultValue = "100",
                        possibleParameters = "Any natural number"),
                @SystemParameter(name = "maxConnectionLifeTime",
                        description = "The maximum life time of a pooled connection.  A zero value indicates " +
                                "no limit to the life time.  A pooled connection that has exceeded its life time " +
                                "will be closed and replaced when necessary by a new connection.",
                        defaultValue = "0",
                        possibleParameters = "Any positive integer"),
                @SystemParameter(name = "socketKeepAlive",
                        description = "Sets whether to keep a connection alive through firewalls",
                        defaultValue = "false",
                        possibleParameters = {"true", "false"}),
                @SystemParameter(name = "socketTimeout",
                        description = "The time in milliseconds to attempt a send or receive on a socket " +
                                "before the attempt times out. Default 0 means never to timeout.",
                        defaultValue = "0",
                        possibleParameters = "Any natural integer"),
                @SystemParameter(name = "writeConcern",
                        description = "The write concern to use.",
                        defaultValue = "acknowledged",
                        possibleParameters = {"acknowledged", "w1", "w2", "w3", "unacknowledged", "fsynced",
                                "journaled", "replica_acknowledged", "normal", "safe", "majority", "fsync_safe",
                                "journal_safe", "replicas_safe"}),
                @SystemParameter(name = "readConcern",
                        description = "The level of isolation for the reads from replica sets.",
                        defaultValue = "default",
                        possibleParameters = {"local", "majority", "linearizable"}),
                @SystemParameter(name = "readPreference",
                        description = "Specifies the replica set read preference for the connection.",
                        defaultValue = "primary",
                        possibleParameters = {"primary", "secondary", "secondarypreferred", "primarypreferred",
                                "nearest"}),
                @SystemParameter(name = "localThreshold",
                        description = "The size (in milliseconds) of the latency window for selecting among " +
                                "multiple suitable CosmosDB instances.",
                        defaultValue = "15",
                        possibleParameters = "Any natural number"),
                @SystemParameter(name = "serverSelectionTimeout",
                        description = "Specifies how long (in milliseconds) to block for server selection " +
                                "before throwing an exception. A value of 0 means that it will timeout immediately " +
                                "if no server is available.  A negative value means to wait indefinitely.",
                        defaultValue = "30000",
                        possibleParameters = "Any integer"),
                @SystemParameter(name = "heartbeatSocketTimeout",
                        description = "The socket timeout for connections used for the cluster heartbeat. A value of " +
                                "0 means that it will timeout immediately if no cluster member is available.  " +
                                "A negative value means to wait indefinitely.",
                        defaultValue = "20000",
                        possibleParameters = "Any integer"),
                @SystemParameter(name = "heartbeatConnectTimeout",
                        description = "The connect timeout for connections used for the cluster heartbeat. A value " +
                                "of 0 means that it will timeout immediately if no cluster member is available.  " +
                                "A negative value means to wait indefinitely.",
                        defaultValue = "20000",
                        possibleParameters = "Any integer"),
                @SystemParameter(name = "heartbeatFrequency",
                        description = "Specify the interval (in milliseconds) between checks, counted from " +
                                "the end of the previous check until the beginning of the next one.",
                        defaultValue = "10000",
                        possibleParameters = "Any positive integer"),
                @SystemParameter(name = "minHeartbeatFrequency",
                        description = "Sets the minimum heartbeat frequency.  In the event that the driver " +
                                "has to frequently re-check a server's availability, it will wait at least this " +
                                "long since the previous check to avoid wasted effort.",
                        defaultValue = "500",
                        possibleParameters = "Any positive integer")
        },
        examples = {
                @Example(
                        syntax = "@Store(type=\"cosmosdb\"," +
                                "cosmosdb.uri=\"cosmosdb://admin:admin@localhost/Foo\")\n" +
                                "@PrimaryKey(\"symbol\")\n" +
                                "@IndexBy(\"volume 1 {background:true,unique:true}\")\n" +
                                "define table FooTable (symbol string, price float, volume long);",
                        description = "This will create a collection called FooTable for the events to be saved " +
                                "with symbol as Primary Key(unique index at cosmosd level) and index for the field " +
                                "volume will be created in ascending order with the index option to create the index " +
                                "in the background.\n\n" +
                                "Note: \n" +
                                "@PrimaryKey: This specifies a list of comma-separated values to be treated as " +
                                "unique fields in the table. Each record in the table must have a unique combination " +
                                "of values for the fields specified here.\n\n" +
                                "@IndexBy: This specifies the fields that must be indexed at the database level. " +
                                "You can specify multiple values as a come-separated list. A single value to be in " +
                                "the format,\n“<FieldName> <SortOrder> <IndexOptions>”\n" +
                                "<SortOrder> - ( 1) for Ascending and (-1) for Descending\n" +
                                "<IndexOptions> - Index Options must be defined inside curly brackets. {} to be " +
                                "used for default options. Options must follow the standard cosmosdb index options " +
                                "format. Reference : " +
                                "https://docs.cosmosdb.com/manual/reference/method/db.collection.createIndex/\n" +
                                "Example : “symbol 1 {“unique”:true}”\n"
                )
        }
)
public class CosmosDBEventTable extends AbstractRecordTable {
    private static final Log log = LogFactory.getLog(CosmosDBEventTable.class);

    private CosmosClientURI cosmosClientURI;
    private CosmosClient cosmosClient;
    private String databaseName;
    private String collectionName;
    private List<String> attributeNames;
    private ArrayList<IndexModel> expectedIndexModels;
    private boolean initialCollectionTest;

    @Override
    protected void init(TableDefinition tableDefinition, ConfigReader configReader) {
        this.attributeNames =
                tableDefinition.getAttributeList().stream().map(Attribute::getName).collect(Collectors.toList());

        Annotation storeAnnotation = AnnotationHelper
                .getAnnotation(ANNOTATION_STORE, tableDefinition.getAnnotations());
        Annotation primaryKeys = AnnotationHelper
                .getAnnotation(ANNOTATION_PRIMARY_KEY, tableDefinition.getAnnotations());
        Annotation indices = AnnotationHelper
                .getAnnotation(ANNOTATION_INDEX_BY, tableDefinition.getAnnotations());

        this.initializeConnectionParameters(storeAnnotation, configReader);

        this.expectedIndexModels = new ArrayList<>();
        IndexModel primaryKey = CosmosTableUtils.extractPrimaryKey(primaryKeys, this.attributeNames);
        if (primaryKey != null) {
            this.expectedIndexModels.add(primaryKey);
        }
        this.expectedIndexModels.addAll(CosmosTableUtils.extractIndexModels(indices, this.attributeNames));

        String customCollectionName = storeAnnotation.getElement(
                CosmosTableConstants.ANNOTATION_ELEMENT_COLLECTION_NAME);
        this.collectionName = CosmosTableUtils.isEmpty(customCollectionName) ?
                tableDefinition.getId() : customCollectionName;
        this.initialCollectionTest = false;
    }

    /**
     * Method for initializing cosmosClientURI and database name.
     *
     * @param storeAnnotation the source annotation which contains the needed parameters.
     * @param configReader    {@link ConfigReader} ConfigurationReader.
     * @throws CosmosTableException when store annotation does not contain cosmosdb.uri or contains an illegal
     *                             argument for cosmosdb.uri
     */
    private void initializeConnectionParameters(Annotation storeAnnotation, ConfigReader configReader) {
        String cosmosClientURI = storeAnnotation.getElement(CosmosTableConstants.ANNOTATION_ELEMENT_URI);
        if (cosmosClientURI != null) {
            CosmosClientOptions.Builder cosmosClientOptionsBuilder =
                    CosmosTableUtils.extractCosmosClientOptionsBuilder(storeAnnotation, configReader);
            try {
                this.cosmosClientURI = new CosmosClientURI(cosmosClientURI, cosmosClientOptionsBuilder);
                this.databaseName = this.cosmosClientURI.getDatabase();
            } catch (IllegalArgumentException e) {
                throw new SiddhiAppCreationException("Annotation '" + storeAnnotation.getName() + "' contains " +
                        "illegal value for 'cosmosdb.uri' as '" + cosmosClientURI + "'. Please check your query and " +
                        "try again.", e);
            }
        } else {
            throw new SiddhiAppCreationException("Annotation '" + storeAnnotation.getName() +
                    "' must contain the element 'cosmosdb.uri'. Please check your query and try again.");
        }
    }

    /**
     * Method for checking if the collection exists or not.
     *
     * @return <code>true</code> if the collection exists
     * <code>false</code> otherwise
     * @throws CosmosTableException if lookup fails.
     */
    private boolean collectionExists() throws ConnectionUnavailableException {
        try {
            for (String collectionName : this.getDatabaseObject().listCollectionNames()) {
                if (this.collectionName.equals(collectionName)) {
                    return true;
                }
            }
            return false;
        } catch (CosmosSocketOpenException e) {
            throw new ConnectionUnavailableException(e);
        } catch (CosmosException e) {
            this.destroy();
            throw new CosmosTableException("Error in retrieving collection names from the database '"
                    + this.databaseName + "' : " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Method for returning a database object.
     *
     * @return a new {@link CosmosDatabase} instance from the Cosmos client.
     */
    private CosmosDatabase getDatabaseObject() {
        if (this.cosmosClient == null) {
            try {
                this.cosmosClient = new CosmosClient(this.cosmosClientURI);
            } catch (CosmosException e) {
                throw new SiddhiAppCreationException("Annotation 'Store' contains illegal value for " +
                        "element 'cosmosdb.uri' as '" + this.cosmosClientURI + "'. Please check " +
                        "your query and try again.", e);
            }
        }
        return this.cosmosClient.getDatabase(this.databaseName);
    }

    /**
     * Method for returning a collection object.
     *
     * @return a new {@link CosmosCollection} instance from the Cosmos client.
     */
    private CosmosCollection<Document> getCollectionObject() {
        return this.getDatabaseObject().getCollection(this.collectionName);
    }

    /**
     * Method for creating indices on the collection.
     */
    private void createIndices(List<IndexModel> indexModels) throws ConnectionUnavailableException {
        if (!indexModels.isEmpty()) {
            try {
                this.getCollectionObject().createIndexes(indexModels);
            } catch (CosmosSocketOpenException e) {
                throw new ConnectionUnavailableException(e);
            } catch (CosmosException e) {
                this.destroy();
                throw new CosmosTableException("Error in creating indices in the database '"
                        + this.collectionName + "' : " + e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Method for doing bulk write operations on the collection.
     *
     * @param parsedRecords a List of WriteModels to be applied
     * @throws CosmosTableException if the write fails
     */
    private void bulkWrite(List<? extends WriteModel<Document>> parsedRecords) throws ConnectionUnavailableException {
        try {
            if (!parsedRecords.isEmpty()) {
                this.getCollectionObject().bulkWrite(parsedRecords);
            }
        } catch (CosmosSocketOpenException e) {
            throw new ConnectionUnavailableException(e);
        } catch (CosmosBulkWriteException e) {
            List<com.cosmosdb.bulk.BulkWriteError> writeErrors = e.getWriteErrors();
            int failedIndex;
            Object failedModel;
            for (com.cosmosdb.bulk.BulkWriteError bulkWriteError : writeErrors) {
                failedIndex = bulkWriteError.getIndex();
                failedModel = parsedRecords.get(failedIndex);
                if (failedModel instanceof UpdateManyModel) {
                    log.error("The update filter '" + ((UpdateManyModel) failedModel).getFilter().toString() +
                            "' failed to update with event '" + ((UpdateManyModel) failedModel).getUpdate().toString() +
                            "' in the CosmosDB Event Table due to " + bulkWriteError.getMessage());
                } else {
                    if (failedModel instanceof InsertOneModel) {
                        log.error("The event '" + ((InsertOneModel) failedModel).getDocument().toString() +
                                "' failed to insert into the Cosmos Event Table due to " + bulkWriteError.getMessage());
                    } else {

                        log.error("The delete filter '" + ((DeleteManyModel) failedModel).getFilter().toString() +
                                "' failed to delete the events from the CosmosDB Event Table due to "
                                + bulkWriteError.getMessage());
                    }
                }
                if (failedIndex + 1 < parsedRecords.size()) {
                    this.bulkWrite(parsedRecords.subList(failedIndex + 1, parsedRecords.size() - 1));
                }
            }
        } catch (CosmosException e) {
            this.destroy();
            throw new CosmosTableException("Error in writing to the collection '"
                    + this.collectionName + "' : " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    protected void add(List<Object[]> records) throws ConnectionUnavailableException {
        List<InsertOneModel<Document>> parsedRecords = records.stream().map(record -> {
            Map<String, Object> insertMap = CosmosTableUtils.mapValuesToAttributes(record, this.attributeNames);
            Document insertDocument = new Document(insertMap);
            if (log.isDebugEnabled()) {
                log.debug("Event formatted as document '" + insertDocument.toJson() + "' is used for building " +
                        "Cosmos Insert Model");
            }
            return new InsertOneModel<>(insertDocument);
        }).collect(Collectors.toList());
        this.bulkWrite(parsedRecords);
    }



    @Override
    protected void connect() throws ConnectionUnavailableException {
        if (!this.initialCollectionTest) {
            if (!this.collectionExists()) {
                try {
                    this.getDatabaseObject().createCollection(this.collectionName);
                    this.createIndices(expectedIndexModels);
                } catch (CosmosSocketOpenException e) {
                    throw new ConnectionUnavailableException(e);
                } catch (CosmosException e) {
                    this.destroy();
                    throw new CosmosTableException("Creating cosmos collection '" + this.collectionName
                            + "' is not successful due to " + e.getLocalizedMessage(), e);
                }
            } else {
                CosmosCursor<Document> existingIndicesIterator;
                try {
                    existingIndicesIterator = this.getCollectionObject().listIndexes().iterator();
                } catch (CosmosSocketOpenException e) {
                    throw new ConnectionUnavailableException(e);
                } catch (CosmosException e) {
                    this.destroy();
                    throw new CosmosTableException("Retrieving indexes from  cosmos collection '" + this.collectionName
                            + "' is not successful due to " + e.getLocalizedMessage(), e);
                }
                CosmosTableUtils.checkExistingIndices(expectedIndexModels, existingIndicesIterator);
            }
            this.initialCollectionTest = true;
        } else {
            try {
                this.getDatabaseObject().listCollectionNames();
            } catch (CosmosSocketOpenException e) {
                throw new ConnectionUnavailableException(e);
            }
        }
    }

    @Override
    protected void disconnect() {
    }

    @Override
    protected void destroy() {
        if (this.cosmosClient != null) {
            this.cosmosClient.close();
        }
    }
}

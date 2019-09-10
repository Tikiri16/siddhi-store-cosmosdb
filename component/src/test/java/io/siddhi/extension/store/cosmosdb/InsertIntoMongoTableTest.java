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

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.extension.store.cosmosdb.exception.CosmosTableException;
import io.siddhi.query.api.exception.DuplicateDefinitionException;
import io.siddhi.query.api.exception.SiddhiAppValidationException;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;

public class InsertIntoCosmosTableTest {

    private static final Logger log = Logger.getLogger(InsertIntoCosmosTableTest.class);

    private static String uri = CosmosTableTestUtils.resolveBaseUri();

    @BeforeClass
    public void init() {
        log.info("== Cosmos Table INSERT tests started ==");
    }

    @AfterClass
    public void shutdown() {
        log.info("== Cosmos Table INSERT tests completed ==");
    }

    @Test
    public void insertIntoCosmosTableTest1() throws InterruptedException {
        log.info("insertIntoCosmosTableTest1 - DASC5-877:Insert events to a CosmosDB table successfully");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 1, "Insertion failed");

    }

    @Test(expectedExceptions = DuplicateDefinitionException.class)
    public void insertIntoCosmosTableTest2() {
        log.info("insertIntoCosmosTableTest2 - " +
                "DASC5-878:Insert events to a CosmosDB table when query has less attributes to select from");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void insertIntoCosmosTableTest3() {
        log.info("insertIntoCosmosTableTest3 - " +
                "DASC5-879:[N] Insert events to a CosmosDB table when query has more attributes to select from");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, length, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }


    @Test
    public void insertIntoCosmosTableTest4() throws InterruptedException {
        log.info("insertIntoCosmosTableTest4 - " +
                "DASC5-880:[N] Insert events to a non existing CosmosDB table");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable144;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 0, "Insertion failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void insertIntoCosmosTableTest5() {
        log.info("insertIntoCosmosTableTest5 - " +
                "DASC5-883:[N] Insert events to a CosmosDB table by selecting from non existing stream");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream123 " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void insertIntoCosmosTableTest6() {
        log.info("insertIntoCosmosTableTest6 - " +
                "DASC5-888:[N] Insert events to a CosmosDB table when the stream has not defined");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void insertIntoCosmosTableTest7() {
        log.info("insertIntoCosmosTableTest7 - " +
                "DASC5-889:[N] Insert events data to CosmosDB table when the table has not defined");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, false, "Definition was created");
    }


    @Test
    public void insertIntoCosmosTableTest8() throws InterruptedException {
        log.info("insertIntoCosmosTableTest8 - " +
                "DASC5-890:Insert events to a CosmosDB table when there are multiple primary keys defined");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\",\"price\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 1, "Insertion failed");
    }

    @Test
    public void insertIntoCosmosTableTest9() throws InterruptedException {
        log.info("insertIntoCosmosTableTest9 - " +
                "DASC5-892:Insert an event to a CosmosDB table when the same value was " +
                "inserted for a defined primary key");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});
        fooStream.send(new Object[]{"IBM", 55.6f, 100L});
        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 2, "Insertion failed");
    }

    @Test(expectedExceptions = CosmosTableException.class)
    public void insertIntoCosmosTableTest10() {
        log.info("insertIntoCosmosTableTest10 - " +
                "DASC5-967:Unprivileged user attempts to insert events to a CosmosDB table successfully");

        String uri = CosmosTableTestUtils
                .resolveBaseUri("cosmosdb://admin121:admin123@{{cosmos.servers}}/{{cosmos.database}}");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void insertIntoCosmosTableTest11() throws InterruptedException {
        log.info("insertIntoCosmosTableTest11 - " +
                "DASC5-969:User attempts to insert events with duplicate values for the Indexing fields " +
                "which are unique");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"price 1 {unique:true}\")" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});
        fooStream.send(new Object[]{"IBM", 55.6f, 100L});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 1, "Insertion failed");
    }

    @Test
    public void insertIntoCosmosTableTest12() throws InterruptedException {
        log.info("insertIntoCosmosTableTest12");
        //Object inserts

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@source(type='inMemory', topic='stock') " +
                "define stream FooStream (symbol string, price float, input Object); " +
                "@Store(type=\"cosmosdb\", cosmosdb.uri='" + uri + "')" +
                "@PrimaryKey(\"symbol\")" +
                "define table FooTable (symbol string, price float, input Object);";
        String query = "" +
                "@info(name = 'query1') " +
                "from FooStream " +
                "select symbol, price, input " +
                "insert into FooTable;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        HashMap<String, String> input = new HashMap<>();
        input.put("symbol", "IBM");
        fooStream.send(new Object[]{"WSO2", 55.6f, input});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 1, "Insertion failed");

    }
}

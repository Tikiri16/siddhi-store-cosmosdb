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
import org.apache.log4j.Logger;
import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateOrInsertCosmosTableTest {

    private static final Logger log = Logger.getLogger(UpdateOrInsertCosmosTableTest.class);

    private static String uri = CosmosTableTestUtils.resolveBaseUri();

    @BeforeClass
    public void init() {
        log.info("== Cosmos Table UPDATE/INSERT tests started ==");
    }

    @AfterClass
    public void shutdown() {
        log.info("== Cosmos Table UPDATE/INSERT tests completed ==");
    }

    @Test
    public void updateOrInsertCosmosTableTest1() throws InterruptedException {
        log.info("updateOrInsertCosmosTableTest1 - DASC5-929:Configure siddhi to perform insert/update on " +
                "CosmosDB document");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "update or insert into FooTable " +
                "   on FooTable.symbol== symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler stockStream = siddhiAppRuntime.getInputHandler("StockStream");
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6F, 100L});
        stockStream.send(new Object[]{"GOOG", 75.6F, 100L});
        stockStream.send(new Object[]{"WSO2", 57.6F, 100L});
        fooStream.send(new Object[]{"GOOG", 10.6, 100});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 3, "Update failed");

        Document expectedUpdatedDocument = new Document()
                .append("symbol", "GOOG")
                .append("price", 10.6)
                .append("volume", 100);
        Document updatedDocument = CosmosTableTestUtils.getDocument(uri, "FooTable", "{symbol:'GOOG'}");
        Assert.assertEquals(updatedDocument, expectedUpdatedDocument, "Update Failed");
    }

    @Test
    public void updateOrInsertCosmosTableTest2() throws InterruptedException {
        log.info("updateOrInsertCosmosTableTest2 - DASC5-930:Configure siddhi to perform insert/update on CosmosDB " +
                "Document when no any matching record exist");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "update or insert into FooTable " +
                "   on FooTable.symbol== symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler stockStream = siddhiAppRuntime.getInputHandler("StockStream");
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6F, 100L});
        stockStream.send(new Object[]{"GOOG", 75.6F, 100L});
        stockStream.send(new Object[]{"WSO2", 57.6F, 100L});
        fooStream.send(new Object[]{"GOOG_2", 10.6, 100});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 4, "Update failed");

        Document expectedUpdatedDocument = new Document()
                .append("symbol", "GOOG_2")
                .append("price", 10.6)
                .append("volume", 100);
        Document updatedDocument = CosmosTableTestUtils.getDocument(uri, "FooTable", "{symbol:'GOOG_2'}");
        Assert.assertEquals(updatedDocument, expectedUpdatedDocument, "Update Failed");
    }

    @Test
    public void updateOrInsertCosmosTableTest3() throws InterruptedException {
        log.info("updateOrInsertCosmosTableTest3 - DASC5-931:Configure siddhi to perform insert/update when there " +
                "are some of matching records exist");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "update or insert into FooTable " +
                "   on FooTable.symbol== symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler stockStream = siddhiAppRuntime.getInputHandler("StockStream");
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6F, 100L});
        stockStream.send(new Object[]{"GOOG", 75.6F, 100L});
        fooStream.send(new Object[]{"WSO2", 57.6, 100});
        fooStream.send(new Object[]{"GOOG_2", 10.6, 100});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 3, "Update failed");

        Document expectedUpdatedDocument = new Document()
                .append("symbol", "WSO2")
                .append("price", 57.6)
                .append("volume", 100);
        Document updatedDocument = CosmosTableTestUtils.getDocument(uri, "FooTable", "{symbol:'WSO2'}");
        Assert.assertEquals(updatedDocument, expectedUpdatedDocument, "Update Failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void updateOrInsertCosmosTableTest4() {
        log.info("updateOrInsertCosmosTableTest4 - DASC5-932:[N] Configure siddhi to perform insert/update with " +
                "a non existing stream");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream123 " +
                "update or insert into FooTable " +
                "   on FooTable.symbol== symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void updateOrInsertCosmosTableTest5() {
        log.info("updateOrInsertCosmosTableTest5 - DASC5-933:[N] Configure siddhi to perform insert/update with an " +
                "undefined CosmosDB Document");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "update or insert into FooTable123 " +
                "   on FooTable.symbol== symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void updateOrInsertCosmosTableTest6() {
        log.info("updateOrInsertCosmosTableTest6 - DASC5-934:[N] Configure siddhi to perform insert/update on " +
                "CosmosDB Document with a non-existing attribute");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "update or insert into FooTable " +
                "   on FooTable.symbol123 == symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void updateOrInsertCosmosTableTest7() {
        log.info("updateOrInsertCosmosTableTest7 - DASC5-935:[N] Configure siddhi to perform insert/update on " +
                "CosmosDB Document incorrect siddhi query");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooTable " +
                "update or insert into FooStream " +
                "   on FooTable.symbol == symbol ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }
}

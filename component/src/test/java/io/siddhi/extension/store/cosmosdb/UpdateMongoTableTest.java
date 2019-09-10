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
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.query.api.exception.SiddhiAppValidationException;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class UpdateCosmosTableTest {

    private static final Logger log = Logger.getLogger(UpdateCosmosTableTest.class);

    private static String uri = CosmosTableTestUtils.resolveBaseUri();

    @BeforeClass
    public void init() {
        log.info("== Cosmos Table UPDATE tests started ==");
    }

    @AfterClass
    public void shutdown() {
        log.info("== Cosmos Table UPDATE tests completed ==");
    }

    @Test
    public void updateFromCosmosTableTest1() throws InterruptedException {
        log.info("updateFromCosmosTableTest1 - DASC5-893:Update events of a CosmosDB table successfully");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "update FooTable " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "on FooTable.symbol == symbol;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler stockStream = siddhiAppRuntime.getInputHandler("StockStream");
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 74.6f, 100L});
        stockStream.send(new Object[]{"WSO2_2", 57.6f, 100L});
        fooStream.send(new Object[]{"IBM", 575.6, 500});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 3, "Update failed");

        Document expectedUpdatedDocument = new Document()
                .append("symbol", "IBM")
                .append("price", 575.6)
                .append("volume", 500);
        Document updatedDocument = CosmosTableTestUtils.getDocument(uri, "FooTable", "{symbol:'IBM'}");
        Assert.assertEquals(updatedDocument, expectedUpdatedDocument, "Update Failed");
    }

    @Test
    public void updateFromCosmosTableTest2() throws InterruptedException {
        log.info("updateFromCosmosTableTest2 - DASC5-894:Updates events of a CosmosDB table when query has less " +
                "attributes to select from");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price " +
                "update FooTable " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "   on FooTable.symbol == 'IBM' ;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler stockStream = siddhiAppRuntime.getInputHandler("StockStream");
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 74.6f, 100L});
        stockStream.send(new Object[]{"WSO2_2", 57.6f, 100L});
        fooStream.send(new Object[]{"IBM_2", 575.6f, 500L});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 3, "Update failed");
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void updateFromCosmosTableTest3() {
        log.info("updateFromCosmosTableTest3 - DASC5-895:Update events of a CosmosDB table when query has more " +
                "attributes to select from");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price, volume, length " +
                "update FooTable " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "on FooTable.symbol == symbol;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void updateFromCosmosTableTest4() {
        log.info("updateFromCosmosTableTest4 - DASC5-896:Updates events of a non existing CosmosDB table");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "update FooTable3455 " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "on FooTable.symbol == symbol;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void updateFromCosmosTableTest5() {
        log.info("updateFromCosmosTableTest5 - DASC5-897:Updates events of a CosmosDB table by selecting from non " +
                "existing stream");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream123 " +
                "select symbol, price, volume " +
                "update FooTable " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "on FooTable.symbol == symbol;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void updateFromCosmosTableTest6() {
        log.info("updateFromCosmosTableTest6 - DASC5-899:Updates events of a CosmosDB table based on a non-existing " +
                "attribute");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "update FooTable " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "on FooTable.length == length;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void updateFromCosmosTableTest7() {
        log.info("updateFromCosmosTableTest7 - DASC5-900:Updates events of a CosmosDB table for non-existing attributes");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "update FooTable " +
                "set FooTable.length = length, FooTable.age = age, FooTable.time = time " +
                "on FooTable.symbol == symbol;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void updateFromCosmosTableTest8() throws InterruptedException {
        log.info("updateFromCosmosTableTest8 - DASC5-901:Updates events of a CosmosDB table for non-existing " +
                "attribute value");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream " +
                "insert into FooTable ;" +
                "" +
                "@info(name = 'query2') " +
                "from FooStream " +
                "select symbol, price, volume " +
                "update FooTable " +
                "set FooTable.symbol = symbol, FooTable.price = price, FooTable.volume = volume " +
                "on FooTable.symbol == symbol;";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams + query);
        InputHandler stockStream = siddhiAppRuntime.getInputHandler("StockStream");
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 74.6f, 100L});
        stockStream.send(new Object[]{"WSO2_2", 57.6f, 100L});
        fooStream.send(new Object[]{"IBM_2", 575.6, 500});

        siddhiAppRuntime.shutdown();

        long totalDocumentsInCollection = CosmosTableTestUtils.getDocumentsCount(uri, "FooTable");
        Assert.assertEquals(totalDocumentsInCollection, 3, "Update failed");
    }
}

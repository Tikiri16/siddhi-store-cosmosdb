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
import org.apache.log4j.Logger;
import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DefineCosmosTableTest {

    private static final Logger log = Logger.getLogger(DefineCosmosTableTest.class);

    private static String uri = CosmosTableTestUtils.resolveBaseUri();

    @BeforeClass
    public void init() {
        log.info("== Cosmos Table DEFINITION tests started ==");
    }

    @AfterClass
    public void shutdown() {
        log.info("== Cosmos Table DEFINITION tests completed ==");
    }

    @Test
    public void cosmosTableDefinitionTest1() {
        log.info("cosmosTableDefinitionTest1 - " +
                "DASC5-958:Defining a CosmosDB event table with a non existing collection.");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");
    }

    @Test
    public void cosmosTableDefinitionTest2() {
        log.info("cosmosTableDefinitionTest2 - " +
                "DASC5-854:Defining a CosmosDB event table with an existing collection");

        CosmosTableTestUtils.createCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

    }

    @Test
    public void cosmosTableDefinitionTest3() {
        log.info("cosmosTableDefinitionTest3 - DASC5-856:Defining a CosmosDB event table with a Primary Key field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); " +
                "define stream StockStream (symbol string, price float, volume long);";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document indexExcepted = new org.bson.Document()
                .append("key", new org.bson.Document("symbol", 1))
                .append("name", "symbol_1")
                .append("v", 2)
                .append("unique", true);
        Document indexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "symbol_1");
        Assert.assertEquals(indexActual, indexExcepted, "Primary Key Definition Failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest4() {
        log.info("cosmosTableDefinitionTest4 - " +
                "DASC5-857:Defining a CosmosDB table without defining a value for Primary Key field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest5() {
        log.info("cosmosTableDefinitionTest5 - " +
                "DASC5-858:Defining a CosmosDB table with an invalid value for Primary Key field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol234')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest6() {
        log.info("cosmosTableDefinitionTest6 - " +
                "DASC5-859:Defining a CosmosDB table without having a cosmosdb uri field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb') " +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest7() {
        log.info("cosmosTableDefinitionTest7 - " +
                "DASC5-860:Defining a CosmosDB table without defining a value for cosmosdb uri field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='') " +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest8() {
        log.info("cosmosTableDefinitionTest8 - " +
                "DASC5-861:Defining a CosmosDBS table with an invalid value for cosmosdb uri field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='1234444" + uri + "') " +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void cosmosTableDefinitionTest9() {
        log.info("cosmosTableDefinitionTest9 - " +
                "DASC5-864:Defining a CosmosDB table with an invalid option defined in cosmosdburl");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "?wso2ssl=true')" +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");
    }

    @Test
    public void cosmosTableDefinitionTest10() {
        log.info("cosmosTableDefinitionTest10 - " +
                "DASC5-865:Defining a CosmosDB table with an invalid value for an option defined in cosmosdburl");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "?ssl=wso2true')" +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest11() {
        log.info("cosmosTableDefinitionTest11 - " +
                "DASC5-866:Defining a CosmosDB table without a value for an option defined in cosmosdburl");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "?maxPoolSize=')" +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void cosmosTableDefinitionTest12() {
        log.info("cosmosTableDefinitionTest12 - " +
                "DASC5-867:Defining a CosmosDB table with contradictory values for the same option defined in " +
                "cosmosdburl");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , " +
                "cosmosdb.uri='" + uri + "?ssl=true&ssl=false') " +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");
    }

    @Test
    public void cosmosTableDefinitionTest13() {
        log.info("cosmosTableDefinitionTest13 - " +
                "DASC5-868:Defining a CosmosDB event table with IndexBy field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"price 1 {background:true}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document priceIndexExpected = new Document()
                .append("name", "price_1")
                .append("v", 2)
                .append("key", new Document("price", 1))
                .append("background", true);
        Document priceIndexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "price_1");
        Assert.assertEquals(priceIndexActual, priceIndexExpected, "Index Creation Failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest14() {
        log.info("cosmosTableDefinitionTest14 - " +
                "DASC5-869:Defining a CosmosDB table without defining a value for indexing column within IndexBy field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"1 {background:true}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest15() {
        log.info("cosmosTableDefinitionTest15 - " +
                "DASC5-870:Defining a CosmosDB table with an invalid value for indexing column within IndexBy field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"symbol1234 1 {unique:true}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void cosmosTableDefinitionTest16() {
        log.info("cosmosTableDefinitionTest16 - " +
                "DASC5-872:Defining a CosmosDB table without defining a value for sorting within IndexBy field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"symbol {unique:true}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document indexExcepted = new org.bson.Document()
                .append("key", new org.bson.Document("symbol", 1))
                .append("name", "symbol_1")
                .append("v", 2)
                .append("unique", true);
        Document indexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "symbol_1");
        Assert.assertEquals(indexActual, indexExcepted, "Index Definition Failed");
    }

    @Test
    public void cosmosTableDefinitionTest17() {
        log.info("cosmosTableDefinitionTest17 - " +
                "DASC5-872:Defining a CosmosDB table without defining a value for sorting within IndexBy field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"symbol 1 {}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document indexExcepted = new org.bson.Document()
                .append("key", new org.bson.Document("symbol", 1))
                .append("name", "symbol_1")
                .append("v", 2);
        Document indexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "symbol_1");
        Assert.assertEquals(indexActual, indexExcepted, "Index Definition Failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest18() {
        log.info("cosmosTableDefinitionTest18 - " +
                "DASC5-874:Defining a CosmosDB table by defining non existing options within IndexBy field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"symbol 1 {2222unique:true}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document indexExcepted = new org.bson.Document()
                .append("key", new org.bson.Document("symbol", 1))
                .append("name", "symbol_1")
                .append("v", 2);
        Document indexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "symbol_1");
        Assert.assertEquals(indexActual, indexExcepted, "Index Definition Failed");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void cosmosTableDefinitionTest19() {
        log.info("cosmosTableDefinitionTest19 - " +
                "DASC5-875:Defining a CosmosDB table by defining an option with an invalid value within IndexBy field");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"symbol 1 {background:tr22ue}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void cosmosTableDefinitionTest20() {
        log.info("cosmosTableDefinitionTest20 - " +
                "DASC5-876:Defining a CosmosDB table by having contradictory values for an option " +
                "defined within IndexBy field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"symbol 1 {unique:true, unique: false}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document indexExcepted = new org.bson.Document()
                .append("key", new org.bson.Document("symbol", 1))
                .append("name", "symbol_1")
                .append("v", 2);
        Document indexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "symbol_1");
        Assert.assertEquals(indexActual, indexExcepted, "Index Definition Failed");
    }

    @Test
    public void cosmosTableDefinitionTest21() {
        log.info("cosmosTableDefinitionTest21 - " +
                "DASC5-948:Defining a CosmosDB event table with a new collection name");

        CosmosTableTestUtils.dropCollection(uri, "newcollection");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , " +
                "cosmosdb.uri='" + uri + "', collection.name=\"newcollection\") " +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "newcollection");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");
    }

    @Test
    public void cosmosTableDefinitionTest22() {
        log.info("cosmosTableDefinitionTest22 - " +
                "DASC5-949:Defining a CosmosDB event table with a existing collection name");

        CosmosTableTestUtils.createCollection(uri, "newcollection");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "', " +
                "collection.name=\"newcollection\") " +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "newcollection");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");
    }

    @Test
    public void cosmosTableDefinitionTest23() {
        log.info("cosmosTableDefinitionTest23 - " +
                "DASC5-965:Defining a CosmosDB event table by having multiple indexing columns");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', cosmosdb.uri='" + uri + "')" +
                "@IndexBy(\"price 1 {background:true}\",\"volume 1 {background:true}\")" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document priceIndexExpected = new Document()
                .append("name", "price_1")
                .append("v", 2)
                .append("key", new Document("price", 1))
                .append("background", true);
        Document priceIndexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "price_1");
        Assert.assertEquals(priceIndexActual, priceIndexExpected, "Index Creation Failed");

        Document volumeIndexExpected = new Document()
                .append("name", "volume_1")
                .append("v", 2)
                .append("key", new Document("volume", 1))
                .append("background", true);
        Document volumeIndexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "volume_1");
        Assert.assertEquals(volumeIndexActual, volumeIndexExpected, "Index Creation Failed");
    }

    @Test
    public void cosmosTableDefinitionTest24() {
        log.info("cosmosTableDefinitionTest24 - DASC5-856:Defining a CosmosDB event table with a Primary Key field");

        CosmosTableTestUtils.dropCollection(uri, "FooTable");

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb' , cosmosdb.uri='" + uri + "') " +
                "@PrimaryKey('symbol','price')" +
                "define table FooTable (symbol string, price float, volume long); " +
                "define stream StockStream (symbol string, price float, volume long);";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        boolean doesCollectionExists = CosmosTableTestUtils.doesCollectionExists(uri, "FooTable");
        Assert.assertEquals(doesCollectionExists, true, "Definition failed");

        Document key = new Document()
                .append("symbol", 1)
                .append("price", 1);
        Document indexExcepted = new org.bson.Document()
                .append("key", key)
                .append("name", "symbol_1_price_1")
                .append("v", 2)
                .append("unique", true);
        Document indexActual = CosmosTableTestUtils.getIndex(uri, "FooTable", "symbol_1_price_1");
        Assert.assertEquals(indexActual, indexExcepted, "Primary Key Definition Failed");
    }

}

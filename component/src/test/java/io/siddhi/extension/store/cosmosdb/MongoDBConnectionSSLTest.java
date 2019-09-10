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

import com.cosmosdb.CosmosClient;
import com.cosmosdb.CosmosClientOptions;
import com.cosmosdb.CosmosClientURI;
import com.cosmosdb.CosmosException;
import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class CosmosDBConnectionSSLTest {

    private static final Logger log = Logger.getLogger(CosmosDBConnectionSSLTest.class);

    private static String uri = CosmosTableTestUtils.resolveBaseUri();
    private static CosmosClientOptions.Builder cosmosClientOptionsBuilder = getOptionsWithSSLEnabled();
    private static String keyStorePath;

    @BeforeClass
    public void init() {
        log.info("== Cosmos Table Secure Connection tests started ==");
    }

    @AfterClass
    public void shutdown() {
        log.info("== Cosmos Table Secure Connection tests completed ==");
    }

    @Test
    public void cosmosTableSSLConnectionTest1() throws InterruptedException {
        log.info("cosmosTableSSLConnectionTest1 - " +
                "   DASC5-862:Defining a CosmosDB table with by defining ssl in cosmosdburl");

        dropCollection();

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "?authMechanism=COSMOSDB-X509&ssl=true&sslInvalidHostNameAllowed=true', " +
                "secure.connection='true', " +
                "key.store='" + keyStorePath + "', " +
                "key.store.password='123456', " +
                "trust.store='" + keyStorePath + "', " +
                "trust.store.password='123456')" +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        Assert.assertEquals(doesCollectionExists(), true, "Definition failed");
    }

    @Test
    public void cosmosTableSSLConnectionTest2() {
        log.info("cosmosTableSSLConnectionTest2 - " +
                "DASC5-863:Defining a CosmosDB table with multiple options defined in cosmosdburl");

        dropCollection();

        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "@store(type = 'cosmosdb', " +
                "cosmosdb.uri='" + uri + "?authMechanism=COSMOSDB-X509&ssl=true&sslInvalidHostNameAllowed=true', " +
                "secure.connection='true', " +
                "key.store='" + keyStorePath + "', " +
                "key.store.password='123456', " +
                "trust.store='" + keyStorePath + "', " +
                "trust.store.password='123456')" +
                "@PrimaryKey('symbol')" +
                "define table FooTable (symbol string, price float, volume long); ";
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(streams);
        siddhiAppRuntime.start();
        siddhiAppRuntime.shutdown();

        Assert.assertEquals(doesCollectionExists(), true, "Definition failed");
    }

    private void dropCollection() {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri + "?authMechanism=COSMOSDB-X509",
                cosmosClientOptionsBuilder))) {
            cosmosClient.getDatabase("admin").getCollection("FooTable").drop();
        } catch (CosmosException e) {
            log.debug("Clearing DB collection failed due to " + e.getMessage(), e);
            throw e;
        }
    }

    private boolean doesCollectionExists() {
        try (CosmosClient cosmosClient = new CosmosClient(new CosmosClientURI(uri + "?authMechanism=COSMOSDB-X509",
                cosmosClientOptionsBuilder))) {
            for (String collectionName : cosmosClient.getDatabase("admin").listCollectionNames()) {
                if ("FooTable".equals(collectionName)) {
                    return true;
                }
            }
            return false;
        } catch (CosmosException e) {
            log.debug("Checking whether collection was created failed due to" + e.getMessage(), e);
            throw e;
        }
    }

    private static CosmosClientOptions.Builder getOptionsWithSSLEnabled() {
        URL trustStoreFile = CosmosDBConnectionSSLTest.class.getResource("/cosmosdb-client.jks");
        keyStorePath = trustStoreFile.getPath();
        File keystoreFile = new File(trustStoreFile.getFile());
        try (InputStream trustStream = new FileInputStream(keystoreFile)) {
            char[] trustPassword = "123456".toCharArray();

            KeyStore trustStore;
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(trustStream, trustPassword);

            TrustManagerFactory trustFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory.init(trustStore);
            TrustManager[] trustManagers = trustFactory.getTrustManagers();

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(trustStore, trustPassword);
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(keyManagers, trustManagers, null);

            cosmosClientOptionsBuilder = CosmosClientOptions.builder();
            cosmosClientOptionsBuilder
                    .sslEnabled(true)
                    .sslInvalidHostNameAllowed(true)
                    .socketFactory(sslContext.getSocketFactory());

            return cosmosClientOptionsBuilder;
        } catch (FileNotFoundException e) {
            log.debug("Key store file not found for secure connections to cosmosdb.", e);
        } catch (IOException e) {
            log.debug("I/O Exception in creating trust store for secure connections to cosmosdb.", e);
        } catch (CertificateException e) {
            log.debug("Certificates in the trust store could not be loaded for secure connections " +
                    "to cosmosdb.", e);
        } catch (NoSuchAlgorithmException e) {
            log.debug("The algorithm used to check the integrity of the trust store cannot be " +
                    "found.", e);
        } catch (KeyStoreException e) {
            log.debug("Exception in creating trust store, no Provider supports aKeyStoreSpi " +
                    "implementation for the specified type.", e);
        } catch (UnrecoverableKeyException e) {
            log.debug("Key in the keystore cannot be recovered.", e);
        } catch (KeyManagementException e) {
            log.debug("Error in validating the key in the key store/ trust store.", e);
        }
        return null;
    }

}

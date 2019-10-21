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
package io.siddhi.extension.store.cosmosdb.util;

import io.siddhi.core.exception.CannotLoadConfigurationException;
import io.siddhi.core.util.collection.operator.CompiledExpression;
import io.siddhi.extension.store.cosmosdb.CosmosCompiledCondition;
import io.siddhi.extension.store.cosmosdb.exception.CosmosTableException;
import io.siddhi.query.api.annotation.Annotation;
import io.siddhi.query.api.annotation.Element;
import io.siddhi.query.api.definition.Attribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.siddhi.extension.store.cosmosdb.util.CosmosTableConstants.*;

/**
 * Class which holds the utility methods which are used by various units in the CosmosDB Event Table implementation.
 */
public class CosmosTableUtils {
    private static final Log log = LogFactory.getLog(CosmosTableUtils.class);

    private CosmosTableUtils() {
        //Prevent Initialization.
    }

    /**
     * Utility method which can be used to check if a given string instance is null or empty.
     *
     * @param field the string instance to be checked.
     * @return true if the field is null or empty.
     */
    public static boolean isEmpty(String field) {
        return (field == null || field.trim().length() == 0);
    }

    /**
     * Method which can be used to clear up and ephemeral SQL connectivity artifacts.
     *
     * @param rs   {@link ResultSet} instance (can be null)
     * @param stmt {@link PreparedStatement} instance (can be null)
     * @param conn {@link Connection} instance (can be null)
     */
    public static void cleanupConnection(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
                if (log.isDebugEnabled()) {
                    log.debug("Closed ResultSet");
                }
            } catch (SQLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error closing ResultSet: " + e.getMessage(), e);
                }
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
                if (log.isDebugEnabled()) {
                    log.debug("Closed PreparedStatement");
                }
            } catch (SQLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error closing PreparedStatement: " + e.getMessage(), e);
                }
            }
        }
        if (conn != null) {
            try {
                conn.close();
                if (log.isDebugEnabled()) {
                    log.debug("Closed Connection");
                }
            } catch (SQLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error closing Connection: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Method which is used to roll back a DB connection (e.g. in case of any errors)
     *
     * @param conn the {@link Connection} instance to be rolled back (can be null).
     */
    public static void rollbackConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignore) { /* ignore */ }
        }
    }

    /**
     * Util method used throughout the CosmosDB Event Table implementation which accepts a compiled condition (from
     * compile-time) and uses values from the runtime to populate the given {@link PreparedStatement}.
     *
     * @param stmt                  the {@link PreparedStatement} instance which has already been build with '?'
     *                              parameters to be filled.
     * @param compiledCondition     the compiled condition which was built during compile time and now is being provided
     *                              by the Siddhi runtime.
     * @param conditionParameterMap the map which contains the runtime value(s) for the condition.
     * @param seed                  the integer factor by which the ordinal count will be incremented when populating
     *                              the {@link PreparedStatement}.
     * @throws SQLException in the unlikely case where there are errors when setting values to the statement
     *                      (e.g. type mismatches)
     */
    public static String resolveCondition(CosmosCompiledCondition compiledCondition,
                                          Map<String, Object> conditionParameterMap, int seed) throws SQLException {
        //int maxOrdinal = 0;
        /*String stmt = compiledCondition.getCompiledQuery();
        SortedMap<Integer, Object> parameters = compiledCondition.getParameters();
        for (Map.Entry<Integer, Object> entry : parameters.entrySet()) {
            Object parameter = entry.getValue();
            //int ordinal = entry.getKey();
            *//*if (ordinal > maxOrdinal) {
                maxOrdinal = ordinal;
            }*//*
            if (parameter instanceof Constant) {
                Constant constant = (Constant) parameter;
                //maxOrdinal = seed + ordinal;
                stmt.replace("?", (CharSequence) constant.getValue());
            } else {
                Attribute variable = (Attribute) parameter;
                //maxOrdinal = seed + ordinal;
                stmt.replace("?", (String) conditionParameterMap.get(variable.getName()));
            }
        }
        return stmt;*/

        // from elasticsearch
        String condition = compiledCondition.getCompiledQuery();
        if (log.isDebugEnabled()) {
            log.debug("compiled condition for collection : " + condition);
        }
        for (Map.Entry<String, Object> entry : conditionParameterMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value.getClass().getName() == "java.lang.String") {
                condition = condition.replace("?", "'" + value.toString() + "'");
            } else {
                condition = condition.replace("?", value.toString());
            }

        }
        //set solr "select all" query if condition is not provided
        if (condition == null || condition.isEmpty()) {
            condition = "*:*";
        }
        if (log.isDebugEnabled()) {
            log.debug("Resolved condition for collection : " + condition);
        }
        return condition;
    }




    public static int enumerateUpdateSetEntries(Map<String, CompiledExpression> updateSetExpressions,
                                                PreparedStatement stmt, Map<String, Object> updateSetMap)
            throws SQLException {
        Object parameter;
        int ordinal = 1;
        for (Map.Entry<String, CompiledExpression> assignmentEntry : updateSetExpressions.entrySet()) {
            for (Map.Entry<Integer, Object> parameterEntry :
                    ((CosmosCompiledCondition) assignmentEntry.getValue()).getParameters().entrySet()) {
                parameter = parameterEntry.getValue();
                if (parameter instanceof Constant) {
                    Constant constant = (Constant) parameter;
                } else {
                    Attribute variable = (Attribute) parameter;
                }
                ordinal++;
            }
        }
        return ordinal;
    }

    /**
     * Util method which is used to populate a {@link PreparedStatement} instance with a single element.
     *
     * @param stmt    the statement to which the element should be set.
     * @param ordinal the ordinal of the element in the statement (its place in a potential list of places).
     * @param type    the type of the element to be set, adheres to
     *                {@link Attribute.Type}.
     * @param value   the value of the element.
     * @throws SQLException if there are issues when the element is being set.
     * @return
     */
    /*public static String populateStatementWithSingleElement(String stmt, int ordinal, Attribute.Type type,
                                                            Object value) throws SQLException {
        //stmt.replace(ordinal, (String) value);
        if(stmt==null){
            return stmt;
        }else if(ordinal<0 || ordinal>=stmt.length()){
            return stmt;
        }
        char[] chars = stmt.toCharArray();
        chars[ordinal] = (char) value;
        return String.valueOf(chars);

    }*/



    /**
     * Util method which validates the elements from an annotation to verify that they comply to the accepted standards.
     *
     * @param annotation the annotation to be validated.
     */
    public static void validateAnnotation(Annotation annotation) {
        if (annotation == null) {
            return;
        }
        List<Element> elements = annotation.getElements();
        for (Element element : elements) {
            if (isEmpty(element.getValue())) {
                throw new CosmosTableException("Annotation '" + annotation.getName() + "' contains illegal value(s). " +
                        "Please check your query and try again.");
            }
        }
    }

    /**
     * Util method used to convert a list of elements in an annotation to a comma-separated string.
     *
     * @param elements the list of annotation elements.
     * @return a comma-separated string of all elements in the list.
     */
    public static String flattenAnnotatedElements(List<Element> elements) {
        StringBuilder sb = new StringBuilder();
        elements.forEach(elem -> {
            sb.append(elem.getValue());
            if (elements.indexOf(elem) != elements.size() - 1) {
                sb.append(CosmosTableConstants.SEPARATOR);
            }
        });
        return sb.toString();
    }

    /**
     * Read and return all string field lengths given in an CosmosDB event table definition.
     *
     * @param fieldInfo the field length annotation from the "@Store" definition (can be empty).
     * @return a map of fields and their specified sizes.
     */
    public static Map<String, String> processFieldLengths(String fieldInfo) {
        return processKeyValuePairs(fieldInfo).stream()
                .collect(Collectors.toMap(name -> name[0], value -> value[1]));
    }

    /**
     * Converts a flat string of key/value pairs (e.g. from an annotation) into a list of pairs.
     * Used String[] since Java does not offer tuples.
     *
     * @param annotationString the comma-separated string of key/value pairs.
     * @return a list processed and validated pairs.
     */
    public static List<String[]> processKeyValuePairs(String annotationString) {
        List<String[]> keyValuePairs = new ArrayList<>();
        if (!isEmpty(annotationString)) {
            String[] pairs = annotationString.split(",");
            for (String element : pairs) {
                if (!element.contains(":")) {
                    throw new CosmosTableException("Property '" + element + "' does not adhere to the expected " +
                            "format: a property must be a key-value pair separated by a colon (:)");
                }
                String[] pair = element.split(":");
                if (pair.length != 2) {
                    throw new CosmosTableException("Property '" + pair[0] + "' does not adhere to the expected " +
                            "format: a property must be a key-value pair separated by a colon (:)");
                } else {
                    keyValuePairs.add(new String[]{pair[0].trim(), pair[1].trim()});
                }
            }
        }
        return keyValuePairs;
    }

    /**
     * Method for replacing the placeholder for conditions with the SQL Where clause and the actual condition.
     *
     * @param query     the SQL query in string format, with the "{{CONDITION}}" placeholder present.
     * @param condition the actual condition (originating from the ConditionVisitor).
     * @return the formatted string.
     */
    public static String formatQueryWithCondition(String query, String condition) {
        return query.replace(PLACEHOLDER_CONDITION, SQL_WHERE + WHITESPACE + condition);
    }

    /**
     * Utility method used for looking up DB metadata information from a given datasource.
     *
     * @param ds the datasource from which the metadata needs to be looked up.
     * @return a list of DB metadata.
     */
    /*public static Map<String, Object> lookupDatabaseInfo(DataSource ds) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            Map<String, Object> result = new HashMap<>();
            result.put(DATABASE_PRODUCT_NAME, dmd.getDatabaseProductName());
            result.put(VERSION, Double.parseDouble(dmd.getDatabaseMajorVersion() + "."
                    + dmd.getDatabaseMinorVersion()));
            return result;
        } catch (SQLException e) {
            throw new CosmosTableException("Error in looking up database type: " + e.getMessage(), e);
        } finally {
            cleanupConnection(null, null, conn);
        }
    }*/

    /**
     * Checks and returns an instance of the CosmosDB query configuration mapper.
     *
     * @return an instance of {@link CosmosDBConfigurationMapper}.
     * @throws CannotLoadConfigurationException if the configuration cannot be loaded.
     */
    /*private static CosmosDBConfigurationMapper loadCosmosDBConfigurationMapper() throws CannotLoadConfigurationException {
        if (mapper == null) {
            synchronized (CosmosTableUtils.class) {
                CosmosDBQueryConfiguration config = loadQueryConfiguration();
                mapper = new CosmosDBConfigurationMapper(config);
            }
        }
        return mapper;
    }*/

    /**
     * Isolates a particular CosmosDB query configuration entry which matches the retrieved DB metadata.
     *
     * @param ds the datasource against which the entry should be matched.
     * @return the matching CosmosDB query configuration entry.
     * @throws CannotLoadConfigurationException if the configuration cannot be loaded.
     */
    /*public static CosmosDBQueryConfigurationEntry lookupCurrentQueryConfigurationEntry(
            DataSource ds, ConfigReader configReader) throws CannotLoadConfigurationException {
        Map<String, Object> dbInfo = lookupDatabaseInfo(ds);
        CosmosDBConfigurationMapper mapper = loadCosmosDBConfigurationMapper();
        CosmosDBQueryConfigurationEntry entry = mapper.lookupEntry((String) dbInfo.get(DATABASE_PRODUCT_NAME),
                (double) dbInfo.get(VERSION), configReader);
        if (entry != null) {
            return entry;
        } else {
            throw new CannotLoadConfigurationException("Cannot find a database section in the CosmosDB "
                    + "configuration for the database: " + dbInfo);
        }
    }*/

    /**
     * Utility method which loads the query configuration from file.
     *
     * @return the loaded query configuration.
     * @throws CannotLoadConfigurationException if the configuration cannot be loaded.
     */
    /*private static CosmosDBQueryConfiguration loadQueryConfiguration() throws CannotLoadConfigurationException {
        return new CosmosDBTableConfigLoader().loadConfiguration();
    }
*/
    /**
     * Child class with a method for loading the JAXB configuration mappings.
     */
    /*private static class CosmosDBTableConfigLoader {

     *//**
     * Method for loading the configuration mappings.
     *
     * @return an instance of {@link CosmosDBQueryConfiguration}.
     * @throws CannotLoadConfigurationException if the config cannot be loaded.
     *//*
        private CosmosDBQueryConfiguration loadConfiguration() throws CannotLoadConfigurationException {
            InputStream inputStream = null;
            try {
                JAXBContext ctx = JAXBContext.newInstance(CosmosDBQueryConfiguration.class);
                Unmarshaller unmarshaller = ctx.createUnmarshaller();
                ClassLoader classLoader = getClass().getClassLoader();
                inputStream = classLoader.getResourceAsStream(CosmosDB_QUERY_CONFIG_FILE);
                if (inputStream == null) {
                    throw new CannotLoadConfigurationException(CosmosDB_QUERY_CONFIG_FILE
                            + " is not found in the classpath");
                }
                return (CosmosDBQueryConfiguration) unmarshaller.unmarshal(inputStream);
            } catch (JAXBException e) {
                throw new CannotLoadConfigurationException(
                        "Error in processing CosmosDB query configuration: " + e.getMessage(), e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error(String.format("Failed to close the input stream for %s", CosmosDB_QUERY_CONFIG_FILE));
                    }
                }
            }
        }
    }

    *//**
     * CosmosDB configuration mapping class to be used to lookup matching configuration entry with a data source.
     *//*
    private static class CosmosDBConfigurationMapper {

        private List<Map.Entry<Pattern, CosmosDBQueryConfigurationEntry>> entries = new ArrayList<>();

        public CosmosDBConfigurationMapper(CosmosDBQueryConfiguration config) {
            for (CosmosDBQueryConfigurationEntry entry : config.getDatabases()) {
                this.entries.add(Maps.immutableEntry(Pattern.compile(
                        entry.getDatabaseName().toLowerCase(Locale.ENGLISH)), entry));
            }
        }

        private boolean checkVersion(CosmosDBQueryConfigurationEntry entry, double version, ConfigReader configReader) {
            double minVersion = Double.parseDouble(configReader.readConfig(entry.getDatabaseName() + PROPERTY_SEPARATOR
                    + MIN_VERSION, String.valueOf(entry.getMinVersion())));
            double maxVersion = Double.parseDouble(configReader.readConfig(entry.getDatabaseName() + PROPERTY_SEPARATOR
                    + MAX_VERSION, String.valueOf(entry.getMaxVersion())));
            //Keeping things readable
            if (minVersion != 0 && version < minVersion) {
                return false;
            }
            if (maxVersion != 0 && version > maxVersion) {
                return false;
            }
            return true;
        }

        private List<CosmosDBQueryConfigurationEntry> extractMatchingConfigEntries(String dbName) {
            List<CosmosDBQueryConfigurationEntry> result = new ArrayList<>();
            this.entries.forEach(entry -> {
                if (entry.getKey().matcher(dbName).find()) {
                    result.add(entry.getValue());
                }
            });
            return result;
        }

        public CosmosDBQueryConfigurationEntry lookupEntry(String dbName, double version, ConfigReader configReader) {
            List<CosmosDBQueryConfigurationEntry> dbResults = this.extractMatchingConfigEntries(
                    dbName.toLowerCase(Locale.ENGLISH));
            if (dbResults.isEmpty()) {
                return null;
            }
            CosmosDBQueryConfigurationEntry versionResults = null;
            for (CosmosDBQueryConfigurationEntry entry : dbResults) {
                if (this.checkVersion(entry, version, configReader)) {
                    versionResults = entry;
                }
            }
            return versionResults;
        }
    }*/




    /**
     * Utility method tp map the values to the respective attributes before database writes.
     *
     * @param record         Object array of the runtime values.
     * @param attributeNames List containing names of the attributes.
     * @return Document
     */
    public static Map<String, Object> mapValuesToAttributes(Object[] record, List<String> attributeNames) {
        Map<String, Object> attributesValuesMap = new HashMap<>();
        for (int i = 0; i < record.length; i++) {
            attributesValuesMap.put(attributeNames.get(i), record[i]);
        }
        return attributesValuesMap;
    }



    private static String resolveCarbonHome(String filePath) {
        String carbonHome = "";
        if (System.getProperty(VARIABLE_CARBON_HOME) != null) {
            carbonHome = System.getProperty(VARIABLE_CARBON_HOME);
        } else if (System.getenv(VARIABLE_CARBON_HOME) != null) {
            carbonHome = System.getenv(VARIABLE_CARBON_HOME);
        }
        return filePath.replaceAll("\\$\\{carbon.home}", carbonHome);
    }
}


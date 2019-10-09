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

import io.siddhi.core.util.collection.operator.CompiledCondition;

import java.util.Map;

/**
 * Implementation class of {@link CompiledCondition} corresponding to the CosmosDB Event Table.
 * Maintains the condition string returned by the ConditionVisitor as well as a map of parameters to be used at runtime.
 */
public class CosmosCompiledCondition implements CompiledCondition {

    private String compiledQuery;
    private Map<String, Object> placeholders;


    public CosmosCompiledCondition(String compiledQuery, Map<String, Object> parameters) {
        this.compiledQuery = compiledQuery;
        this.placeholders = parameters;
    }

    public String getCompiledQuery() {
        return compiledQuery;
    }

    public String toString() {
        return getCompiledQuery();
    }

    public Map<String, Object> getPlaceholders() {
        return placeholders;
    }
}
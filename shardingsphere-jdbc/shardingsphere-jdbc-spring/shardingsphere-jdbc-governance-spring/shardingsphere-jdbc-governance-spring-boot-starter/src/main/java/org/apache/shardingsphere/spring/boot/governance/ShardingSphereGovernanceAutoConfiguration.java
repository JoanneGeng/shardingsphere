/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.spring.boot.governance;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.mode.config.ModeConfiguration;
import org.apache.shardingsphere.infra.yaml.config.swapper.mode.ModeConfigurationYamlSwapper;
import org.apache.shardingsphere.spring.boot.datasource.DataSourceMapSetter;
import org.apache.shardingsphere.spring.boot.governance.common.GovernanceSpringBootRootConfiguration;
import org.apache.shardingsphere.spring.boot.governance.rule.LocalRulesCondition;
import org.apache.shardingsphere.spring.boot.schema.SchemaNameSetter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Governance spring boot configuration.
 */
@Configuration
@ComponentScan("org.apache.shardingsphere.spring.boot.converter")
@EnableConfigurationProperties(GovernanceSpringBootRootConfiguration.class)
@ConditionalOnProperty(prefix = "spring.shardingsphere", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class ShardingSphereGovernanceAutoConfiguration implements EnvironmentAware {
    
    private String schemaName;
    
    private final Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();
    
    private final GovernanceSpringBootRootConfiguration root;
    
    /**
     * Get mode configuration.
     *
     * @return mode configuration
     */
    @Bean
    public ModeConfiguration modeConfiguration() {
        Preconditions.checkState(Objects.nonNull(root.getMode()), "The mode configuration is invalid, please configure mode");
        return new ModeConfigurationYamlSwapper().swapToObject(root.getMode());
    }
    
    /**
     * Get governance ShardingSphere data source bean by local configuration.
     *
     * @param rules rules configuration
     * @param modeConfig mode configuration
     * @return governance sharding data source bean
     * @throws SQLException SQL exception
     */
    @Bean
    @Conditional(LocalRulesCondition.class)
    @Autowired(required = false)
    public DataSource localShardingSphereDataSource(final ObjectProvider<List<RuleConfiguration>> rules, final ModeConfiguration modeConfig) throws SQLException {
        return createDataSource(modeConfig, Optional.ofNullable(rules.getIfAvailable()).orElse(Collections.emptyList()));
    }
    
    /**
     * Get data source bean from registry center.
     *
     * @param modeConfig mode configuration
     * @return data source bean
     * @throws SQLException SQL Exception
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(final ModeConfiguration modeConfig) throws SQLException {
        return createDataSource(modeConfig);
    }
    
    @Override
    public final void setEnvironment(final Environment environment) {
        schemaName = SchemaNameSetter.getSchemaName(environment);
        dataSourceMap.putAll(DataSourceMapSetter.getDataSourceMap(environment));
    }
    
    private DataSource createDataSource(final ModeConfiguration modeConfig, final List<RuleConfiguration> ruleConfigs) throws SQLException {
        return new ShardingSphereDataSource(schemaName, modeConfig, dataSourceMap, ruleConfigs, root.getProps());
    }
    
    private DataSource createDataSource(final ModeConfiguration modeConfig) throws SQLException {
        return new ShardingSphereDataSource(schemaName, modeConfig);
    }
}

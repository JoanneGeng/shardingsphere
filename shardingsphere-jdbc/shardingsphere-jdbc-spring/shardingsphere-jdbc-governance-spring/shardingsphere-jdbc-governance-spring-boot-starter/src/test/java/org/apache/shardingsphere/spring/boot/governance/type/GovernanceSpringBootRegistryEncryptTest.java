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

package org.apache.shardingsphere.spring.boot.governance.type;

import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.encrypt.api.config.EncryptRuleConfiguration;
import org.apache.shardingsphere.governance.repository.api.config.ClusterPersistRepositoryConfiguration;
import org.apache.shardingsphere.governance.repository.zookeeper.CuratorZookeeperRepository;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.context.manager.ContextManager;
import org.apache.shardingsphere.infra.database.DefaultSchema;
import org.apache.shardingsphere.spring.boot.governance.util.EmbedTestingServer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = GovernanceSpringBootRegistryEncryptTest.class)
@SpringBootApplication
@ActiveProfiles("registry")
public class GovernanceSpringBootRegistryEncryptTest {
    
    private static final String DATA_SOURCE_FILE = "yaml/data-source.yaml";
    
    private static final String ENCRYPT_RULE_FILE = "yaml/encrypt-rule.yaml";
    
    @Resource
    private DataSource dataSource;
    
    @BeforeClass
    public static void init() {
        EmbedTestingServer.start();
        String dataSource = readYAML(DATA_SOURCE_FILE);
        String encryptRule = readYAML(ENCRYPT_RULE_FILE);
        CuratorZookeeperRepository repository = new CuratorZookeeperRepository();
        repository.init(new ClusterPersistRepositoryConfiguration("ZooKeeper", "governance-spring-boot-test", "localhost:3183", new Properties()));
        repository.persist("/metadata/logic_db/dataSources", dataSource);
        repository.persist("/metadata/logic_db/rules", encryptRule);
        repository.persist("/props", ConfigurationPropertyKey.SQL_SHOW.getKey() + ": 'true'\n");
        repository.persist("/states/datanodes", "");
    }
    
    @Test
    public void assertWithEncryptDataSource() throws NoSuchFieldException, IllegalAccessException {
        assertTrue(dataSource instanceof ShardingSphereDataSource);
        Field field = ShardingSphereDataSource.class.getDeclaredField("contextManager");
        field.setAccessible(true);
        ContextManager contextManager = (ContextManager) field.get(dataSource);
        BasicDataSource embedDataSource = (BasicDataSource) contextManager.getMetaDataContexts().getMetaData(DefaultSchema.LOGIC_NAME).getResource().getDataSources().values().iterator().next();
        assertThat(embedDataSource.getMaxTotal(), is(100));
        assertThat(embedDataSource.getUsername(), is("sa"));
        EncryptRuleConfiguration config = (EncryptRuleConfiguration) contextManager.getMetaDataContexts().getMetaData(DefaultSchema.LOGIC_NAME).getRuleMetaData().getConfigurations().iterator().next();
        assertThat(config.getEncryptors().size(), is(1));
        ShardingSphereAlgorithmConfiguration encryptAlgorithmConfig = config.getEncryptors().get("order_encrypt");
        assertThat(encryptAlgorithmConfig, instanceOf(ShardingSphereAlgorithmConfiguration.class));
        assertThat(encryptAlgorithmConfig.getType(), is("AES"));
    }
    
    @SneakyThrows({IOException.class, URISyntaxException.class})
    private static String readYAML(final String yamlFile) {
        return Files.readAllLines(Paths.get(ClassLoader.getSystemResource(yamlFile).toURI())).stream().map(each -> each + System.lineSeparator()).collect(Collectors.joining());
    }
}

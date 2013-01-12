/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.plugins.hibernate4.internal;

import griffon.util.ConfigUtils;
import griffon.util.RunnableWithArgs;
import griffon.util.RunnableWithArgsClosure;
import groovy.util.ConfigObject;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import static griffon.util.GriffonNameUtils.isBlank;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.eachLine;

/**
 * Sets up a shared Hibernate SessionFactory.
 * Based on Spring's {@code org.springframework.orm.hibernate4.LocalSessionFactoryBean}
 * Original author: Juergen Hoeller (Spring 1.2)
 *
 * @author Andres Almiray
 */
public class HibernateConfigurationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HibernateConfigurationHelper.class);
    public static final String ENTITY_INTERCEPTOR = "entityInterceptor";
    public static final String NAMING_STRATEGY = "namingStrategy";
    public static final String PROPS = "props";

    private final ConfigObject sessionConfig;
    private final ConfigObject dataSourceConfig;
    private final String dataSourceName;
    private final DataSource dataSource;

    public HibernateConfigurationHelper(ConfigObject sessionConfig, ConfigObject dataSourceConfig, String dataSourceName, DataSource dataSource) {
        this.sessionConfig = sessionConfig;
        this.dataSourceConfig = dataSourceConfig;
        this.dataSourceName = dataSourceName;
        this.dataSource = dataSource;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public ConfigObject getSessionConfig() {
        return sessionConfig;
    }

    public ConfigObject getDataSourceConfig() {
        return dataSourceConfig;
    }

    public Configuration buildConfiguration() throws Exception {
        // Create Configuration instance.
        Configuration config = newConfiguration();

        applyEntityInterceptor(config);
        applyNamingStrategy(config);
        applyProperties(config);
        applyDialect(config);
        applyMappings(config);

        return config;
    }

    private void applyEntityInterceptor(Configuration config) {
        Object entityInterceptor = ConfigUtils.getConfigValue(sessionConfig, ENTITY_INTERCEPTOR);
        if (entityInterceptor instanceof Class) {
            config.setInterceptor((Interceptor) newInstanceOf((Class) entityInterceptor));
        } else if (entityInterceptor instanceof String) {
            config.setInterceptor((Interceptor) newInstanceOf((String) entityInterceptor));
        }
    }

    private void applyNamingStrategy(Configuration config) {
        Object namingStrategy = ConfigUtils.getConfigValue(sessionConfig, NAMING_STRATEGY);
        if (namingStrategy instanceof Class) {
            config.setNamingStrategy((NamingStrategy) newInstanceOf((Class) namingStrategy));
        } else if (namingStrategy instanceof String) {
            config.setNamingStrategy((NamingStrategy) newInstanceOf((String) namingStrategy));
        }
    }

    private void applyProperties(Configuration config) {
        Object props = ConfigUtils.getConfigValue(sessionConfig, PROPS);
        if (props instanceof Properties) {
            config.setProperties((Properties) props);
        } else if (props instanceof Map) {
            for (Map.Entry<String, String> entry : ((Map<String, String>) props).entrySet()) {
                config.setProperty(entry.getKey(), entry.getValue());
            }
        }

        if (ConfigUtils.getConfigValueAsBoolean(sessionConfig, "logSql")) {
            config.setProperty("hibernate.show_sql", "true");
        }
        if (ConfigUtils.getConfigValueAsBoolean(sessionConfig, "formatSql")) {
            config.setProperty("hibernate.format_sql", "true");
        }
    }

    private void applyDialect(Configuration config) {
        Object dialect = ConfigUtils.getConfigValue(sessionConfig, "dialect");
        if (dialect instanceof Class) {
            config.setProperty("hibernate.dialect", ((Class) dialect).getName());
        } else if (dialect != null) {
            config.setProperty("hibernate.dialect", dialect.toString());
        } else {
            DialectDetector dialectDetector = new DialectDetector(dataSource);
            config.setProperty("hibernate.dialect", dialectDetector.getDialect());
        }
    }

    private void applyMappings(final Configuration config) {
        try {
            Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/hibernate4/mappings.txt");
            while (urls.hasMoreElements()) {
                eachLine(urls.nextElement(), new RunnableWithArgsClosure(new RunnableWithArgs() {
                    @Override
                    public void run(Object[] args) {
                        String line = ((String) args[0]).trim();
                        if (isBlank(line)) return;
                        config.addResource(line);
                    }
                }));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Object newInstanceOf(String className) {
        try {
            return newInstanceOf(Thread.currentThread().getContextClassLoader().loadClass(className));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot instantiate class " + className, e);
        }
    }

    private Object newInstanceOf(Class klass) {
        try {
            return klass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot instantiate " + klass, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate " + klass, e);
        }
    }

    // -------------------------------------------------

    private Configuration newConfiguration() throws HibernateException {
        Configuration configuration = new Configuration();
        configuration.getProperties().put(Environment.DATASOURCE, dataSource);
        return configuration;
    }
}

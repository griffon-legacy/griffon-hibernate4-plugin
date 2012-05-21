/*
 * Copyright 2012 the original author or authors.
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

package griffon.plugins.hibernate4

import griffon.plugins.datasource.DataSourceConnector
import griffon.plugins.datasource.DataSourceHolder
import griffon.plugins.hibernate4.internal.HibernateConfigurationHelper
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import griffon.util.CallableWithArgs
import griffon.core.GriffonApplication

import javax.sql.DataSource

/**
 * @author Andres Almiray
 */
@Singleton
final class Hibernate4Connector implements Hibernate4Provider {
    private bootstrap

    Object withHibernate4(String sessionFactoryName = 'default', Closure closure) {
        SessionFactoryHolder.instance.withHibernate4(sessionFactoryName, closure)
    }

    public <T> T withHibernate4(String sessionFactoryName = 'default', CallableWithArgs<T> callable) {
        SessionFactoryHolder.instance.withHibernate4(sessionFactoryName, callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        def configClass = app.class.classLoader.loadClass('Hibernate4Config')
        new ConfigSlurper(griffon.util.Environment.current.name).parse(configClass)
    }

    private ConfigObject narrowConfig(ConfigObject config, String dataSourceName) {
        return dataSourceName == 'default' ? config.sessionFactory : config.sessionFactories[dataSourceName]
    }

    SessionFactory connect(GriffonApplication app, String dataSourceName = 'default') {
        if (SessionFactoryHolder.instance.isSessionFactoryAvailable(dataSourceName)) {
            return SessionFactoryHolder.instance.getSessionFactory(dataSourceName)
        }

        ConfigObject dsConfig = DataSourceConnector.instance.createConfig(app)
        if (dataSourceName == 'default') {
            dsConfig.dataSource.schema.skip = true
        } else {
            dsConfig.dataSources."$dataSourceName".schema.skip = true
        }
        DataSource dataSource = DataSourceConnector.instance.connect(app, dsConfig, dataSourceName)

        ConfigObject config = narrowConfig(createConfig(app), dataSourceName)
        app.event('Hibernate4ConnectStart', [config, dataSourceName])
        Configuration configuration = createConfiguration(app, config, dsConfig, dataSourceName)
        createSchema(dsConfig.dbCreate ?: 'create-drop', configuration)
        SessionFactory sessionFactory = configuration.buildSessionFactory()
        SessionFactoryHolder.instance.setSessionFactory(dataSourceName, sessionFactory)
        bootstrap = app.class.classLoader.loadClass('BootstrapHibernate4').newInstance()
        bootstrap.metaClass.app = app
        SessionFactoryHolder.instance.withHibernate4(dataSourceName) { dsName, session -> bootstrap.init(dsName, session) }
        app.event('Hibernate4ConnectEnd', [dataSourceName, dataSource])
        sessionFactory
    }

    void disconnect(GriffonApplication app, String dataSourceName = 'default') {
        if (!SessionFactoryHolder.instance.isSessionFactoryAvailable(dataSourceName)) return

        SessionFactory sessionFactory = SessionFactoryHolder.instance.getSessionFactory(dataSourceName)
        app.event('Hibernate4DisconnectStart', [dataSourceName, sessionFactory])
        SessionFactoryHolder.instance.withHibernate4(dataSourceName) { dsName, session -> bootstrap.destroy(dsName, session) }
        SessionFactoryHolder.instance.disconnectSessionFactory(dataSourceName)
        app.event('Hibernate4DisconnectEnd', [dataSourceName])
        ConfigObject config = DataSourceConnector.instance.createConfig(app)
        DataSourceConnector.instance.disconnect(app, config, dataSourceName)
    }

    private Configuration createConfiguration(GriffonApplication app, ConfigObject config, ConfigObject dsConfig, String dataSourceName) {
        DataSource dataSource = DataSourceHolder.instance.getDataSource(dataSourceName)
        HibernateConfigurationHelper configHelper = new HibernateConfigurationHelper(config, dsConfig, dataSourceName, dataSource)
        Configuration configuration = configHelper.buildConfiguration()
        app.event('Hibernate4ConfigurationAvailable', [[
                configuration: configuration,
                dataSourceName: dataSourceName,
                dataSourceConfig: dsConfig,
                hibernateConfig: config
        ]])
        configuration
    }

    private void createSchema(String dbCreate, Configuration configuration) {
        if (dbCreate == 'skip') dbCreate = 'validate'
        configuration.setProperty('hibernate.hbm2ddl.auto', dbCreate)
    }
}
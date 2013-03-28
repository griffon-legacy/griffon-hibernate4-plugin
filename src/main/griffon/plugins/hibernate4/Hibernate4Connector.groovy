/*
 * Copyright 2012-2013 the original author or authors.
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
import griffon.util.ConfigUtils
import griffon.core.GriffonApplication

import javax.sql.DataSource

/**
 * @author Andres Almiray
 */
@Singleton
final class Hibernate4Connector {
    private static final String DEFAULT = 'default'
    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.hibernate4) {
            app.config.pluginConfig.hibernate4 = ConfigUtils.loadConfigWithI18n('Hibernate4Config')
        }
        app.config.pluginConfig.hibernate4
    }

    private ConfigObject narrowConfig(ConfigObject config, String dataSourceName) {
        return dataSourceName == DEFAULT ? config.sessionFactory : config.sessionFactories[dataSourceName]
    }

    SessionFactory connect(GriffonApplication app, String dataSourceName = DEFAULT) {
        if (Hibernate4Holder.instance.isSessionFactoryAvailable(dataSourceName)) {
            return Hibernate4Holder.instance.getSessionFactory(dataSourceName)
        }

        ConfigObject dsConfig = DataSourceConnector.instance.createConfig(app)
        if (dataSourceName == DEFAULT) {
            dsConfig.dataSource.schema.skip = true
        } else {
            dsConfig.dataSources."$dataSourceName".schema.skip = true
        }
        DataSource dataSource = DataSourceConnector.instance.connect(app, dsConfig, dataSourceName)

        ConfigObject config = narrowConfig(createConfig(app), dataSourceName)
        app.event('Hibernate4ConnectStart', [config, dataSourceName])
        Configuration configuration = createConfiguration(app, config, dsConfig, dataSourceName)
        createSchema(dsConfig, dataSourceName, configuration)
        SessionFactory sessionFactory = configuration.buildSessionFactory()
        Hibernate4Holder.instance.setSessionFactory(dataSourceName, sessionFactory)
        app.event('Hibernate4SessionFactoryCreated', [config, dataSourceName, sessionFactory])
        bootstrap = app.class.classLoader.loadClass('BootstrapHibernate4').newInstance()
        bootstrap.metaClass.app = app
        resolveHibernate4Provider(app).withHibernate4(dataSourceName) { dsName, session -> bootstrap.init(dsName, session) }
        app.event('Hibernate4ConnectEnd', [dataSourceName, dataSource])
        sessionFactory
    }

    void disconnect(GriffonApplication app, String dataSourceName = DEFAULT) {
        if (!Hibernate4Holder.instance.isSessionFactoryAvailable(dataSourceName)) return

        SessionFactory sessionFactory = Hibernate4Holder.instance.getSessionFactory(dataSourceName)
        app.event('Hibernate4DisconnectStart', [dataSourceName, sessionFactory])
        resolveHibernate4Provider(app).withHibernate4(dataSourceName) { dsName, session -> bootstrap.destroy(dsName, session) }
        Hibernate4Holder.instance.disconnectSessionFactory(dataSourceName)
        app.event('Hibernate4DisconnectEnd', [dataSourceName])
        ConfigObject config = DataSourceConnector.instance.createConfig(app)
        DataSourceConnector.instance.disconnect(app, config, dataSourceName)
    }

    Hibernate4Provider resolveHibernate4Provider(GriffonApplication app) {
        def hibernate4Provider = app.config.hibernate4Provider
        if (hibernate4Provider instanceof Class) {
            hibernate4Provider = hibernate4Provider.newInstance()
            app.config.hibernate4Provider = hibernate4Provider
        } else if (!hibernate4Provider) {
            hibernate4Provider = DefaultHibernate4Provider.instance
            app.config.hibernate4Provider = hibernate4Provider
        }
        hibernate4Provider
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

    private void createSchema(ConfigObject config, String dataSourceName, Configuration configuration) {
        String dbCreate = 'create-drop'
        if (dataSourceName == DEFAULT) {
            dbCreate = config.dataSource.dbCreate ?: dbCreate
        } else {
            dbCreate = config.dataSources[dataSourceName].dbCreate ?: dbCreate
        }
        if (dbCreate == 'skip') dbCreate = 'validate'
        configuration.setProperty('hibernate.hbm2ddl.auto', dbCreate)
    }
}

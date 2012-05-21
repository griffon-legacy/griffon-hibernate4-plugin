/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by getApplication()licable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */

/**
 * @author Andres Almiray
 */
class Hibernate4GriffonPlugin {
    // the plugin version
    String version = '0.1'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '0.9.5 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [datasource: '0.3']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-hibernate4-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Hibernate4 support'
    String description = '''
The Hibernate plugin enables lightweight access to RDBMS using [Hibernate][1].
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * Hibernate4Config.groovy - contains the database definitions.
 * BootstrapHibernate4.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

This plugin relies on the facilities exposed by the [datasource][2] plugin.

A new dynamic method named `withHibernate4` will be injected into all controllers,
giving you access to a `org.hibernate.Session` object, with which you'll be able
to make calls to the database. Remember to make all database calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.
This method is aware of multiple databases. If no databaseName is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withHibernate4 { databaseName, session -> ... }
            withHibernate4('internal') { databaseName, session -> ... }
        }
    }
    
This method is also accessible to any component through the singleton `griffon.plugins.hibernate4.Hibernate4Connector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`Hhibernate4Enhancer.enhance(metaClassInstance, hibernateProviderInstance)`.

Configuration
-------------
### Mapping Files

The plugin expects to find mapping files that conform to the standard class name patern supported by Hibernate. For example,
a class named `sample.Person` must have a companion mapping file `sample/Person.hbm.xml`. These mapping files must be placed
under `griffon-app/resources` in order to be picked up automatically by the plugin.

### Dynamic method injection

The `withHibernate4()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.hibernate4.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * Hibernate4ConnectStart[config, dataSourceName] - triggered before connecting to the database
 * Hibernate4ConfigurationAvailable[configuration, dataSourceName, dataSourceConfig, hibernateConfig] - triggered before opening the SessionFactory
 * Hibernate4ConnectEnd[dataSourceName, sessionFactory] - triggered after connecting to the database
 * Hibernate4DisconnectStart[config, dataSourceName, sessionFactory] - triggered before disconnecting from the database
 * Hibernate4DisconnectEnd[config, dataSourceName] - triggered after disconnecting from the database

### Multiple Session Factories

The config file `Hibernate4Config.groovy` defines a default sessionFactory block. As the name
implies this is the SessionFactory used by default, however you can configure named session factories
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    sessionFactories {
        internal {
        }
    }

The name of the seesion factory must match the name of a configured dataSource in `DataSource.groovy`.
This block can be used inside the `environments()` block in the same way as the
default sessionFactory block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/hibernate4][3]

Testing
-------
The `withHibernate4()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `Hhibernate4Enhancer.enhance(metaClassInstance, hibernate4ProviderInstance)` where 
`hibernate4ProviderInstance` is of type `griffon.plugins.hibernate4.Hibernate4Provider`. The contract for this interface looks like this

    public interface Hibernate4Provider {
        Object withHibernate4(Closure closure);
        Object withHibernate4(String clientName, Closure closure);
        <T> T withHibernate4(CallableWithArgs<T> callable);
        <T> T withHibernate4(String clientName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyHibernate4Provider implements Hibernate4Provider {
        Object withHibernate4(String clientName = 'default', Closure closure) { null }
        public <T> T withHibernate4(String clientName = 'default', CallableWithArgs<T> callable) { null }      
    }
    
This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            Hibernate4Enhancer.enhance(service.metaClass, new MyHibernate4Provider())
            // exercise service methods
        }
    }


[1]: http://hibernate.org/
[2]: /plugin/datasource 
[3]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/hibernate4
'''
}

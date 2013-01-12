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

import org.hibernate.SessionFactory
import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static griffon.util.GriffonNameUtils.isBlank

/**
 * @author Andres Almiray
 */
class Hibernate4Holder {
    private static final String DEFAULT = 'default'
    private static final Logger LOG = LoggerFactory.getLogger(Hibernate4Holder)
    private final Map<String, SessionFactory> sessionFactories = [:]
    private static final Object[] LOCK = new Object[0]

    private static final Hibernate4Holder INSTANCE

    static {
        INSTANCE = new Hibernate4Holder()
    }

    static Hibernate4Holder getInstance() {
        INSTANCE
    }

    String[] getSessionFactoryNames() {
        List<String> sessionFactoryNames = new ArrayList().addAll(sessionFactories.keySet())
        sessionFactoryNames.toArray(new String[sessionFactoryNames.size()])
    }

    SessionFactory getSessionFactory(String sessionFactoryName = DEFAULT) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT
        retrieveSessionFactory(sessionFactoryName)
    }

    void setSessionFactory(String sessionFactoryName = DEFAULT, SessionFactory sf) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT
        storeSessionFactory(sessionFactoryName, sf)
    }

    boolean isSessionFactoryAvailable(String sessionFactoryName) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT
        retrieveSessionFactory(sessionFactoryName) != null
    }

    void disconnectSessionFactory(String sessionFactoryName) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT
        storeSessionFactory(sessionFactoryName, null)
    }

    SessionFactory fetchSessionFactory(String sessionFactoryName) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT
        SessionFactory sf = retrieveSessionFactory(sessionFactoryName)
        if (sf == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = Hibernate4Connector.instance.createConfig(app)
            sf = Hibernate4Connector.instance.connect(app, config, sessionFactoryName)
        }

        if (sf == null) {
            throw new IllegalArgumentException("No such SessionFactory configuration for name $sessionFactoryName")
        }
        sf
    }

    private SessionFactory retrieveSessionFactory(String sessionFactoryName) {
        synchronized (LOCK) {
            sessionFactories[sessionFactoryName]
        }
    }

    private void storeSessionFactory(String sessionFactoryName, SessionFactory sf) {
        synchronized (LOCK) {
            sessionFactories[sessionFactoryName] = sf
        }
    }
}

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

import org.hibernate.Session
import org.hibernate.SessionFactory
import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import griffon.util.CallableWithArgs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static griffon.util.GriffonNameUtils.isBlank

/**
 * @author Andres Almiray
 */
@Singleton
class SessionFactoryHolder implements Hibernate4Provider {
    private static final Logger LOG = LoggerFactory.getLogger(SessionFactoryHolder)
    private final Map<String, SessionFactory> sessionFactories = [:]
    private static final Object[] LOCK = new Object[0]

    String[] getSessionFactoryNames() {
        List<String> sessionFactoryNames = new ArrayList().addAll(sessionFactories.keySet())
        sessionFactoryNames.toArray(new String[sessionFactoryNames.size()])
    }

    SessionFactory getSessionFactory(String sessionFactoryName = 'default') {
        if (isBlank(sessionFactoryName)) sessionFactoryName = 'default'
        retrieveSessionFactory(sessionFactoryName)
    }

    void setSessionFactory(String sessionFactoryName = 'default', SessionFactory sf) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = 'default'
        storeSessionFactory(sessionFactoryName, sf)
    }

    Object withHibernate4(String sessionFactoryName = 'default', Closure closure) {
        SessionFactory sf = fetchSessionFactory(sessionFactoryName)
        if (LOG.debugEnabled) LOG.debug("Executing statements on session '$sessionFactoryName'")
        Session session = sf.openSession()
        try {
            session.beginTransaction()
            return closure(sessionFactoryName, session)
        } finally {
            if (!session.transaction.wasRolledBack()) session.transaction.commit()
            session.close()
        }
    }

    public <T> T withHibernate4(String sessionFactoryName = 'default', CallableWithArgs<T> callable) {
        SessionFactory sf = fetchSessionFactory(sessionFactoryName)
        if (LOG.debugEnabled) LOG.debug("Executing statements on session '$sessionFactoryName'")
        Session session = sf.openSession()
        try {
            session.beginTransaction()
            return callable.call([sessionFactoryName, session] as Object[])
        } finally {
            if (!session.transaction.wasRolledBack()) session.transaction.commit()
            session.close()
        }
    }

    boolean isSessionFactoryAvailable(String sessionFactoryName) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = 'default'
        retrieveSessionFactory(sessionFactoryName) != null
    }

    void disconnectSessionFactory(String sessionFactoryName) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = 'default'
        storeSessionFactory(sessionFactoryName, null)
    }

    private SessionFactory fetchSessionFactory(String sessionFactoryName) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = 'default'
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

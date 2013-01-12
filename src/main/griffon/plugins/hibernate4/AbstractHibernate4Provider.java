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

package griffon.plugins.hibernate4;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractHibernate4Provider implements Hibernate4Provider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHibernate4Provider.class);
    private static final String DEFAULT = "default";

    public <R> R withHibernate4(Closure<R> closure) {
        return withHibernate4(DEFAULT, closure);
    }

    public <R> R withHibernate4(String sessionFactoryName, Closure<R> closure) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT;
        if (closure != null) {
            SessionFactory sf = getSessionFactory(sessionFactoryName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statements on session '" + sessionFactoryName + "'");
            }
            Session session = sf.openSession();
            try {
                session.beginTransaction();
                return closure.call(sessionFactoryName, session);
            } finally {
                if (!session.getTransaction().wasRolledBack()) {
                    session.getTransaction().commit();
                }
                session.close();
            }
        }
        return null;
    }

    public <R> R withHibernate4(CallableWithArgs<R> callable) {
        return withHibernate4(DEFAULT, callable);
    }

    public <R> R withHibernate4(String sessionFactoryName, CallableWithArgs<R> callable) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT;
        if (callable != null) {
            SessionFactory sf = getSessionFactory(sessionFactoryName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statements on session '" + sessionFactoryName + "'");
            }
            Session session = sf.openSession();
            try {
                session.beginTransaction();
                return callable.call(new Object[]{sessionFactoryName, session});
            } finally {
                if (!session.getTransaction().wasRolledBack()) {
                    session.getTransaction().commit();
                }
                session.close();
            }
        }
        return null;
    }

    protected abstract SessionFactory getSessionFactory(String sessionFactoryName);
}
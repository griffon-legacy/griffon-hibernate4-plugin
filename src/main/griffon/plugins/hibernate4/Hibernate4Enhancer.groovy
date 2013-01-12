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

import griffon.util.CallableWithArgs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
final class Hibernate4Enhancer {
    private static final String DEFAULT = 'default'
    private static final Logger LOG = LoggerFactory.getLogger(Hibernate4Enhancer)

    private Hibernate4Enhancer() {}
    
    static void enhance(MetaClass mc, Hibernate4Provider provider = DefaultHibernate4Provider.instance) {
        if(LOG.debugEnabled) LOG.debug("Enhancing $mc with $provider")
        mc.withHibernate4 = {Closure closure ->
            provider.withHibernate4(DEFAULT, closure)
        }
        mc.withHibernate4 << {String sessionFactoryName, Closure closure ->
            provider.withHibernate4(sessionFactoryName, closure)
        }
        mc.withHibernate4 << {CallableWithArgs callable ->
            provider.withHibernate4(DEFAULT, callable)
        }
        mc.withHibernate4 << {String sessionFactoryName, CallableWithArgs callable ->
            provider.withHibernate4(sessionFactoryName, callable)
        }
    }
}
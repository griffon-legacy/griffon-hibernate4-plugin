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

import griffon.core.GriffonClass
import griffon.core.GriffonApplication
import griffon.plugins.hibernate4.Hibernate4Connector
import griffon.plugins.hibernate4.Hibernate4Enhancer
import griffon.plugins.hibernate4.Hibernate4ContributionHandler

/**
 * @author Andres Almiray
 */
class Hibernate4GriffonAddon {
    void addonPostInit(GriffonApplication app) {
        Hibernate4Connector.instance.connect(app)
        def types = app.config.griffon?.hibernate4?.injectInto ?: ['controller']
        for(String type : types) {
            for(GriffonClass gc : app.artifactManager.getClassesOfType(type)) {
                if (Hibernate4ContributionHandler.isAssignableFrom(gc.clazz)) continue
                Hibernate4Enhancer.enhance(gc.metaClass)
            }
        }
    }

    Map events = [
        ShutdownStart: { app ->
            Hibernate4Connector.instance.disconnect(app)
        }
    ]
}

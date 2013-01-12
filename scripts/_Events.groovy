/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.core.io.Resource

/**
 * @author Andres Almiray
 */

eventPackageResourcesEnd = {
    if (compilingPlugin('hibernate4')) return
    Resource[] mappings = resolveResources("file://${resourcesDir.canonicalPath}/**/*.hbm.xml")
    if (mappings) {
        File mappingsFile = new File("${resourcesDir}/META-INF/hibernate4/mappings.txt")
        mappingsFile.parentFile.mkdirs()
        mappingsFile.text = ''
        mappings.each { res ->
            mappingsFile.append((res.file.absolutePath - resourcesDir.absolutePath)[1..-1] + '\n')
        }
    }
}
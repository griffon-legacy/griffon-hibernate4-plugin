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

import griffon.plugins.hibernate4.internal.exceptions.DatabaseException;
import griffon.util.GriffonNameUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Andres Almiray
 */
public class DialectDetector {
    private final DataSource dataSource;
    private final DialectResolver dialectResolver;

    public DialectDetector(DataSource dataSource) {
        this.dataSource = dataSource;
        this.dialectResolver = new StandardDialectResolver();
    }

    public String getDialect() {
        Connection connection = null;

        try {
            String dbName = (String) JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName");
            connection = dataSource.getConnection();

            Dialect hibernateDialect = dialectResolver.resolveDialect(connection.getMetaData());
            String hibernateDialectClassName = hibernateDialect.getClass().getName();

            if (GriffonNameUtils.isBlank(hibernateDialectClassName)) {
                throw new DatabaseException(
                        "Could not determine Hibernate dialect for database name [" + dbName + "]!");
            }

            return hibernateDialectClassName;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }
}

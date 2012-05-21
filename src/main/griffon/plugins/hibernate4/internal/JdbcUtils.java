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

import griffon.plugins.hibernate4.internal.exceptions.MetaDataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

/**
 * <p>Generic utility methods for working with JDBC.</p>
 * <p>Contains code from Spring's {@code org.springframework.jdbc.support.JdbcUtils}.</p>
 * <p/>
 * Original author Thomas Risberg<br/>
 * Original author Juergen Hoeller<br/>
 *
 * @author Andres Almiray
 */
public abstract class JdbcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HibernateConfigurationHelper.class);

    /**
     * Close the given JDBC Connection and ignore any thrown exception.
     * This is useful for typical finally blocks in manual JDBC code.
     *
     * @param con the JDBC Connection to close (may be <code>null</code>)
     */
    public static void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                if (LOG.isTraceEnabled()) {
                    LOG.debug("Could not close JDBC Connection", ex);
                }
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                if (LOG.isTraceEnabled()) {
                    LOG.debug("Unexpected exception on closing JDBC Connection", ex);
                }
            }
        }
    }

    /**
     * Close the given JDBC Statement and ignore any thrown exception.
     * This is useful for typical finally blocks in manual JDBC code.
     *
     * @param stmt the JDBC Statement to close (may be <code>null</code>)
     */
    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Could not close JDBC Statement", ex);
                }
            } catch (Throwable ex) {
                if (LOG.isTraceEnabled()) {
                    // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                    LOG.trace("Unexpected exception on closing JDBC Statement", ex);
                }
            }
        }
    }

    /**
     * Close the given JDBC ResultSet and ignore any thrown exception.
     * This is useful for typical finally blocks in manual JDBC code.
     *
     * @param rs the JDBC ResultSet to close (may be <code>null</code>)
     */
    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Could not close JDBC ResultSet", ex);
                }
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Unexpected exception on closing JDBC ResultSet", ex);
                }
            }
        }
    }

    /**
     * Extract database meta data via the given DatabaseMetaDataCallback.
     * <p>This method will open a connection to the database and retrieve the database metadata.
     * Since this method is called before the exception translation feature is configured for
     * a datasource, this method can not rely on the SQLException translation functionality.
     * <p>Any exceptions will be wrapped in a MetaDataAccessException. This is a checked exception
     * and any calling code should catch and handle this exception. You can just log the
     * error and hope for the best, but there is probably a more serious error that will
     * reappear when you try to access the database again.
     *
     * @param dataSource the DataSource to extract metadata for
     * @param action     callback that will do the actual work
     * @return object containing the extracted information, as returned by
     *         the DatabaseMetaDataCallback's <code>processMetaData</code> method
     * @throws MetaDataAccessException if meta data access failed
     */
    public static Object extractDatabaseMetaData(DataSource dataSource, DatabaseMetaDataCallback action)
            throws MetaDataAccessException {

        Connection con = null;
        try {
            con = dataSource.getConnection();
            if (con == null) {
                // should only happen in test environments
                throw new MetaDataAccessException("Connection returned by DataSource [" + dataSource + "] was null");
            }
            DatabaseMetaData metaData = con.getMetaData();
            if (metaData == null) {
                // should only happen in test environments
                throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
            }
            return action.processMetaData(metaData);
        } catch (SQLException ex) {
            throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
        } catch (AbstractMethodError err) {
            throw new MetaDataAccessException(
                    "JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err);
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Call the specified method on DatabaseMetaData for the given DataSource,
     * and extract the invocation result.
     *
     * @param dataSource         the DataSource to extract meta data for
     * @param metaDataMethodName the name of the DatabaseMetaData method to call
     * @return the object returned by the specified DatabaseMetaData method
     * @throws MetaDataAccessException if we couldn't access the DatabaseMetaData
     *                                 or failed to invoke the specified method
     * @see java.sql.DatabaseMetaData
     */
    public static Object extractDatabaseMetaData(DataSource dataSource, final String metaDataMethodName)
            throws MetaDataAccessException {
        return extractDatabaseMetaData(dataSource,
                new DatabaseMetaDataCallback() {
                    public Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException {
                        try {
                            Method method = DatabaseMetaData.class.getMethod(metaDataMethodName, (Class[]) null);
                            return method.invoke(dbmd, (Object[]) null);
                        } catch (NoSuchMethodException ex) {
                            throw new MetaDataAccessException("No method named '" + metaDataMethodName +
                                    "' found on DatabaseMetaData instance [" + dbmd + "]", ex);
                        } catch (IllegalAccessException ex) {
                            throw new MetaDataAccessException(
                                    "Could not access DatabaseMetaData method '" + metaDataMethodName + "'", ex);
                        } catch (InvocationTargetException ex) {
                            if (ex.getTargetException() instanceof SQLException) {
                                throw (SQLException) ex.getTargetException();
                            }
                            throw new MetaDataAccessException(
                                    "Invocation of DatabaseMetaData method '" + metaDataMethodName + "' failed", ex);
                        }
                    }
                });
    }
}

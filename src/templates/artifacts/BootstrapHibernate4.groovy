import org.hibernate.Session

class BootstrapHibernate4 {
    def init = { String dataSourceName, Session session ->
    }

    def destroy = { String dataSourceName, Session session ->
    }
} 

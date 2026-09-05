package pro.mneura.data.intergration;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class HibernateIntegration {

    private static final Logger LOGGER =
            Logger.getLogger(HibernateIntegration.class.getName());

    private static final SessionFactory SESSION_FACTORY;

    // Database credentials
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/insightline";

    private static final String DB_USERNAME =
            "root";

    private static final String DB_PASSWORD =
            "M@200420802185bm";

    static {
        try {
            Configuration configuration = new Configuration()
                    .configure()
                    .setProperty("hibernate.connection.url", DB_URL)
                    .setProperty("hibernate.connection.username", DB_USERNAME)
                    .setProperty("hibernate.connection.password", DB_PASSWORD);

            SESSION_FACTORY = configuration.buildSessionFactory();

        } catch (RuntimeException exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Database session factory initialization failed: {0}",
                    exception.getClass().getName()
            );

            throw new ExceptionInInitializerError(
                    "Database configuration could not be initialized."
            );
        }
    }

    private HibernateIntegration() {
    }

    public static void shutDown() {
        SESSION_FACTORY.close();
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }
}
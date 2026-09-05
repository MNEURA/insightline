package pro.mneura.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import pro.mneura.data.intergration.HibernateIntegration;

@ApplicationPath("/api")
public class AppConfig extends ResourceConfig {
    public AppConfig() {
        HibernateIntegration.getSessionFactory();
        packages("pro.mneura.controller.api");
    }
}
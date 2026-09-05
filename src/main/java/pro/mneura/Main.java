package pro.mneura;

import pro.mneura.config.AppConfig;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;

public class Main {

    public static final int SERVER_PORT = 8080;
    public static final String CONTEXT_PATH = "/insightline";

    public static void main(String[] args) {

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(SERVER_PORT);
        tomcat.getConnector();

        File webFolder = new File("src/main/webapp").getAbsoluteFile();

        if (!webFolder.exists()) {
            throw new RuntimeException("Web directory not found: " + webFolder.getAbsolutePath());
        }

        Context context = tomcat.addWebapp(CONTEXT_PATH, webFolder.getAbsolutePath());
        ServletContainer servletContainer = new ServletContainer(new AppConfig());
        Tomcat.addServlet(context, "JerseyServlet", servletContainer);
        context.addServletMappingDecoded("/api/*", "JerseyServlet");

        try {
            tomcat.start();
            System.out.println("App running at: http://localhost:" + SERVER_PORT + CONTEXT_PATH+"/");
            tomcat.getServer().await();
        } catch (LifecycleException ex) {
            throw new RuntimeException("Failed to start embedded Tomcat", ex);
        }

    }

}
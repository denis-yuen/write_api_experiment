package io.ga4gh.reference;

import java.util.EnumSet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.ga4gh.reference.api.GitHubBuilder;
import io.ga4gh.reference.api.QuayIoBuilder;
import io.ga4gh.reference.dao.ToolDAO;
import io.ga4gh.reference.dao.ToolDescriptorDAO;
import io.ga4gh.reference.dao.ToolDockerfileDAO;
import io.ga4gh.reference.dao.ToolTestDAO;
import io.ga4gh.reference.dao.ToolVersionDAO;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.server.api.MetadataApi;
import io.swagger.server.api.ToolClassesApi;
import io.swagger.server.api.ToolsApi;
import io.swagger.server.api.factories.ToolsApiServiceFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import static javax.servlet.DispatcherType.REQUEST;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_HEADERS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_METHODS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_ORIGINS_PARAM;

/**
 *
 */
public class ServerApplication extends Application<ServerConfiguration> {
    public static void main(String[] args) throws Exception {
        new ServerApplication().run(args);
    }

    @Override
    public String getName() {
        return "tool-registry-application";
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/static/"));
        bootstrap.addBundle(new ViewBundle<>());
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setSchemes(new String[] { "http" });
        beanConfig.setHost("localhost:8080");
        beanConfig.setBasePath("/");
        beanConfig.setResourcePackage("io.swagger.server.api");
        beanConfig.setScan(true);

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "derby");
        final ToolDAO toolDAO = jdbi.onDemand(ToolDAO.class);
        final ToolVersionDAO toolVersionDAO = jdbi.onDemand(ToolVersionDAO.class);
        final ToolDescriptorDAO toolDescriptorDAO = jdbi.onDemand(ToolDescriptorDAO.class);
        final ToolDockerfileDAO toolDockerfileDAO = jdbi.onDemand(ToolDockerfileDAO.class);
        final ToolTestDAO toolTestDAO = jdbi.onDemand(ToolTestDAO.class);

        String githubToken = System.getenv().getOrDefault("githubToken", configuration.getGithubToken());
        String quayioToken = System.getenv().getOrDefault("quayioToken", configuration.getQuayioToken());

        GitHubBuilder gitHubBuilder = new GitHubBuilder(githubToken);
        QuayIoBuilder quayIoBuilder = new QuayIoBuilder(quayioToken);

        try (Handle h = jdbi.open()) {
            String createdbString = configuration.getDataSourceFactory().getProperties().getOrDefault("createdb", "false");
            boolean freshStart = Boolean.valueOf(createdbString);
            if (freshStart) {
                dropTableQuietly(h, "dockerfile");
                dropTableQuietly(h, "descriptor");
                dropTableQuietly(h, "tooltest");
                dropTableQuietly(h, "toolversion");
                dropTableQuietly(h, "tool");

                toolDAO.createToolTable();
                toolVersionDAO.createToolVersionTable();
                toolDescriptorDAO.createToolDescriptorTable();
                toolDockerfileDAO.createToolDockerfileTable();
                toolTestDAO.createToolTestTable();
            }
        }

        ToolsApiServiceFactory.setToolDAO(toolDAO);
        ToolsApiServiceFactory.setToolVersionDAO(toolVersionDAO);
        ToolsApiServiceFactory.setToolDescriptorDAO(toolDescriptorDAO);
        ToolsApiServiceFactory.setToolDockerfileDAO(toolDockerfileDAO);
        ToolsApiServiceFactory.setGitHubBuilder(gitHubBuilder);
        ToolsApiServiceFactory.setQuayIoBuilder(quayIoBuilder);

        final TemplateHealthCheck healthCheck = new TemplateHealthCheck("Hello %s!");
        environment.healthChecks().register("template", healthCheck);

        environment.jersey().register(new ToolClassesApi());
        environment.jersey().register(new ToolsApi());
        environment.jersey().register(new MetadataApi());

        // extra renderers
        environment.jersey().register(new CharsetResponseFilter());

        // swagger stuff
        // Swagger providers
        environment.jersey().register(ApiListingResource.class);
        environment.jersey().register(SwaggerSerializers.class);

        final ObjectMapper mapper = environment.getObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        // optional CORS support
        // Enable CORS headers
        final FilterHolder filterHolder = environment.getApplicationContext().addFilter(CrossOriginFilter.class, "/*", EnumSet.of(REQUEST));
        // Configure CORS parameters
        filterHolder.setInitParameter(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "GET,POST,DELETE,PUT,OPTIONS");
        filterHolder.setInitParameter(ALLOWED_ORIGINS_PARAM, "*");
        filterHolder.setInitParameter(ALLOWED_METHODS_PARAM, "GET,POST,DELETE,PUT,OPTIONS");
        filterHolder.setInitParameter(ALLOWED_HEADERS_PARAM,
                "Authorization, X-Auth-Username, X-Auth-Password, X-Requested-With,Content-Type,Accept,Origin,Access-Control-Request-Headers,cache-control");
    }

    private void dropTableQuietly(Handle h, String tableName) {
        try {
            h.execute("drop table " + tableName);
        } catch (Exception e) {
            // do nothing if the table does not already exist
            System.out.println(e.getMessage());
        }
    }
}

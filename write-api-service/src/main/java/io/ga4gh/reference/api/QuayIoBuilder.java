package io.ga4gh.reference.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.quay.client.ApiClient;
import io.swagger.quay.client.ApiException;
import io.swagger.quay.client.Configuration;
import io.swagger.quay.client.api.BuildApi;
import io.swagger.quay.client.api.RepositoryApi;
import io.swagger.quay.client.model.NewRepo;
import io.swagger.quay.client.model.RepositoryBuildRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create structures in the dockstore.org format.
 */
public class QuayIoBuilder {

    public static final String QUAY_URL = "https://quay.io/api/v1/";
    public static final String QUAY_DOMAIN = "https://quay.io/";
    private static final Logger LOG = LoggerFactory.getLogger(QuayIoBuilder.class);

    private final ApiClient apiClient;
    private final CloseableHttpClient httpClient;
    private final String token;

    public QuayIoBuilder(String token) {
        apiClient = Configuration.getDefaultApiClient();
        apiClient.addDefaultHeader("Authorization", "Bearer " + token);
        this.httpClient = HttpClientBuilder.create().build();
        this.token = token;
    }

    public static void main(String[] args) {
        QuayIoBuilder builder = new QuayIoBuilder(args[0]);
        if (!builder.repoExists("denis_yuen", "test")) {
            builder.createRepo("denis_yuen", "test", "test_repo");
        }
        if (!builder.triggerBuild("denis-yuen", "denis_yuen", "test_repo", "test", "2017.03.08")) {
            throw new RuntimeException("Could not trigger build, please check your credentials");
        }
    }

    public boolean triggerBuild(String githubOrg, String quayOrg, String gitRepo, String quayRepo, String release) {
        try {
            BuildApi buildApi = new BuildApi(apiClient);
            final String repo = quayOrg + '/' + quayRepo;
            RepositoryBuildRequest request = new RepositoryBuildRequest();
            request.setArchiveUrl("https://github.com/" + githubOrg + "/" + gitRepo + "/archive/" + release + ".tar.gz");
            request.setSubdirectory(quayRepo + "-" + release + "/" + "Dockerfile");
            List<String> tags = new ArrayList<>();
            tags.add("latest");
            request.setDockerTags(tags);
            buildApi.requestRepoBuild(repo, request);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean createRepo(String namespace, String quayRepo, String gitRepo) {
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("namespace", namespace);
        infoMap.put("repo", gitRepo);

        Gson gson = new GsonBuilder().create();
        RepositoryApi api = new RepositoryApi(apiClient);
        NewRepo newRepo = new NewRepo();
        newRepo.setNamespace(namespace);
        newRepo.setRepository(quayRepo);
        newRepo.setVisibility(NewRepo.VisibilityEnum.PUBLIC);
        newRepo.setDescription("GA4GH auto-managed repo\n\n\n" + "This is an example repository." + "\n\n" + "----------  \n"
                + "[GA4GH-generated-do-not-edit]: <> (" + gson.toJson(infoMap) + ")");
        try {
            api.createRepo(newRepo);
            return true;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean repoExists(String namespace, String repo) {
        final String repoUrl = QUAY_URL + "repository/" + namespace + "/" + repo;
        Optional<String> responseAsString = ResourceUtilities.asString(repoUrl, token, httpClient);
        return responseAsString.map(s -> true).orElse(false);
    }

    public String getQuayUrl(String namespace, String repo) {
        final String repoUrl = QUAY_DOMAIN + "repository/" + namespace + "/" + repo;
        return repoUrl;
    }
}

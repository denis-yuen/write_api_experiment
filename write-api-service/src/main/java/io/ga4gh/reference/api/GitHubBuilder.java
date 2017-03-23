package io.ga4gh.reference.api;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.OrganizationService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create structures in the dockstore.org format.
 */
public class GitHubBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubBuilder.class);

    private final GitHubClient githubClient;
    private final UserService uService;
    private final OrganizationService oService;
    private final ContentsService cService;
    private final RepositoryService service;
    private final DataService dService;
    private final CommitService commitService;

    public GitHubBuilder(String token) {
        this.githubClient = new GitHubClient();
        githubClient.setOAuth2Token(token);

        uService = new UserService(githubClient);
        oService = new OrganizationService(githubClient);
        service = new RepositoryService(githubClient);
        cService = new ContentsService(githubClient);
        dService = new DataService(githubClient);
        commitService = new CommitService(githubClient);
    }

    public static void main(String[] args) {
        GitHubBuilder builder = new GitHubBuilder(args[0]);
        String organization = "denis-yuen";
        String repo = "test_repo";
        if (!builder.repoExists(organization, repo)) {
            builder.createRepo(organization, repo);
        }
        builder.stashFile(organization, repo, "Dockerfile", "FROM ubuntu:12.04");
        builder.createBranchAndRelease(organization, repo, "v1");

    }

    public boolean createRepo(String organization, String repo) {
        try {
            Repository repoTemplate = new Repository();
            repoTemplate.setName(repo);
            service.createRepository(organization, repoTemplate);
            // need to initialize the new repo, oddly not possible via API
            Map<String, Object> map = new HashMap<>();
            byte[] encode = Base64.getEncoder().encode("Test".getBytes(StandardCharsets.UTF_8));
            map.put("content", new String(encode, StandardCharsets.UTF_8));
            map.put("message", "test");
            githubClient.put("/repos/" + organization + "/" + repo + "/contents/readme.md", map, Map.class);
        } catch (RequestException e) {
            LOG.error("Was not able to create " + organization + "/" + repo);
            // was not able to create the repo
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean repoExists(String organization, String repo) {
        try {
            return service.getRepository(organization, repo) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean stashFile(String organization, String repo, String path, String content) {
        try {
            Repository repository = service.getRepository(organization, repo);
            List<RepositoryContents> contents = new ArrayList<>();
            try {
                contents = cService.getContents(repository, path);
            } catch (IOException e) {
                LOG.info("IO Exception: " + e.getMessage());
            }
            if (contents.isEmpty()) {
                // no API for creating files? weird
                Map<String, Object> map = new HashMap<>();
                byte[] encode = Base64.getEncoder().encode(content.getBytes(StandardCharsets.UTF_8));
                map.put("content", new String(encode, StandardCharsets.UTF_8));
                map.put("message", "test");
                githubClient.put("/repos/" + organization + "/" + repo + "/contents/" + path, map, Map.class);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public boolean createBranchAndRelease(String organization, String repo, String releaseName) {
        try {
            try {
                Map<String, Object> map = lowLevelGetRequest(
                        "https://api.github.com/repos/" + organization + "/" + repo + "/releases/tags/" + releaseName);
                int releaseNumber = Double.valueOf((double)map.get("id")).intValue();
                // delete the release
                githubClient.delete("/repos/" + organization + "/" + repo + "/releases/" + releaseNumber);
                // delete the tag (makes the next release "stay" in the wrong place)
                githubClient.delete("/repos/" + organization + "/" + repo + "/git/refs/tags/" + releaseName);
            } catch (HttpResponseException e) {
                // ignore 404s
                if (e.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                    throw new RuntimeException(e);
                }
            }
            // find out the default branch and branch version from there
            Repository repository = service.getRepository(organization, repo);
            String stringId = repository.generateId();
            String defaultBranch = repository.getDefaultBranch();
            RepositoryId fromId = RepositoryId.createFromId(stringId);
            // might use reference later, but cannot figure out how to "target" a reference for branching
            //Reference reference = dService.getReference(fromId, "heads/" + defaultBranch);

            List<RepositoryCommit> commits = commitService.getCommits(fromId);
            // found latest sha
            RepositoryCommit repositoryCommit = commits.get(commits.size() - 1);

            try {
                // create branch if needed
                Map<String, Object> branchMap = new HashMap<>();
                branchMap.put("ref", "refs/heads/" + releaseName);
                branchMap.put("sha", repositoryCommit.getSha());
                Object post1 = githubClient.post("/repos/" + organization + "/" + repo + "/git/refs", branchMap, Map.class);
            } catch (RequestException e) {
                // ignore exceptions if reference already exists
                if (!e.getMessage().contains("Reference already exists")) {
                    throw new RuntimeException(e);
                } else {
                    LOG.info("Git branch already exists");
                }
            }

            // no API for creating files on releases? weird
            try {
                Map<String, Object> releaseMap = new HashMap<>();
                releaseMap.put("tag_name", releaseName);
                releaseMap.put("name", releaseName);
                LOG.info("So far so good");
                LOG.info(releaseMap.toString());
                Object post2 = githubClient.post("/repos/" + organization + "/" + repo + "/releases", releaseMap, Map.class);
            } catch (RequestException e) {
                LOG.info("Git tag already exists");
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * An annoying workaround to the issue that some github APi calls
     * are not exposed by egit library
     *
     * @param url
     * @return
     * @throws IOException
     */
    private Map<String, Object> lowLevelGetRequest(String url) throws IOException {
        NetHttpTransport transport = new NetHttpTransport.Builder().build();
        HttpRequestFactory requestFactory = transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(new GenericUrl(url));
        HttpResponse execute = httpRequest.execute();
        InputStream content = execute.getContent();
        String s = IOUtils.toString(content, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        Type stringStringMap = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(s, stringStringMap);
    }
}

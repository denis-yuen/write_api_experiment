package io.ga4gh.reference.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.api.client.http.HttpResponseException;
import org.apache.http.HttpStatus;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;
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
    private static final int FIVE = 5;
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
        builder.stashFile(organization, repo, "Dockerfile", "FROM ubuntu:12.04", "v1");
        builder.createBranchAndRelease(organization, repo, "v1");

    }

    private void wait(String organization, String repo) {
        int i = 0;
        while (!repoExists(organization, repo) && i < FIVE) {
            try {
                TimeUnit.SECONDS.sleep(i);
                i++;
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    public boolean createRepo(String organization, String repo) {
        try {
            Repository repoTemplate = new Repository();
            repoTemplate.setName(repo);
            service.createRepository(organization, repoTemplate);
            wait(organization, repo);
            // need to initialize the new repo, oddly not possible via API
            Map<String, Object> map = new HashMap<>();
            byte[] encode = Base64.getEncoder().encode("Test".getBytes(StandardCharsets.UTF_8));
            map.put("content", new String(encode, StandardCharsets.UTF_8));
            map.put("message", "test");
            String uri = "/repos/" + organization + "/" + repo + "/contents/readme.md";
            LOG.info("GIT PUT: " + uri);

            githubClient.put(uri, map, Map.class);
            wait(organization, repo);
        } catch (RequestException e) {
            LOG.error("Was not able to create " + organization + "/" + repo + " and create readme.md. " + e.getMessage());
            // was not able to create the repo
            return false;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException("Could not create GitHub repository.  Check your GitHub token.");
        }
        return true;
    }

    public boolean repoExists(String organization, String repo) {
        try {
            return service.getRepository(organization, repo) != null;
        } catch (IOException e) {
            LOG.warn("Could not get GitHub repository.");
            return false;
        }
    }

    public String getGitUrl(String owner, String name) {
        try {
            Repository repository = service.getRepository(owner, name);
            return repository.getHtmlUrl();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException("Could not get Git URL.  Check your GitHub token");
        }
    }

    public boolean stashFile(String organization, String repo, String path, String content, String branch) {
        try {
            setDefaultBranch(organization, repo, branch);
            Repository repository = service.getRepository(organization, repo);
            List<RepositoryContents> contents = new ArrayList<>();
            try {
                contents = cService.getContents(repository, path);

            } catch (IOException e) {
                LOG.info("Could not get contents of " + path);
            }
            // no API for creating files? weird
            Map<String, Object> map = new HashMap<>();
            byte[] encode = Base64.getEncoder().encode(content.getBytes(StandardCharsets.UTF_8));
            map.put("content", new String(encode, StandardCharsets.UTF_8));
            map.put("message", "test");
            map.put("branch", branch);
            if (!contents.isEmpty()) {
                map.put("sha", contents.get(0).getSha());
            }
            String uri = "/repos/" + organization + "/" + repo + "/contents/" + path;
            LOG.info("GIT PUT: " + uri);
            githubClient.put(uri, map, Map.class);
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean setDefaultBranch(String organization, String repo, String branchName) {
        Repository repository;
        wait(organization, repo);
        try {
            repository = service.getRepository(organization, repo);
            repository.setDefaultBranch(branchName);
            service.editRepository(repository);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException("Could not set default GitHub repository branch.  Check your GitHub token");
        }
        return true;
    }

    public boolean createBranchAndRelease(String organization, String repo, String releaseName) {
        try {
            wait(organization, repo);
            GitHubRequest request = new GitHubRequest();
            request.setUri("/repos/" + organization + "/" + repo + "/releases/tags/" + releaseName);
            request.setType(Map.class);
            GitHubResponse gitHubResponse = githubClient.get(request);
            Map<String, Object> map = (Map<String, Object>)gitHubResponse.getBody();
            if (map == null) {
                LOG.info("Response is null");
                throw new RuntimeException("Could not get tag");
            }
            int releaseNumber = ((Double)map.get("id")).intValue();

            // delete the release
            String uri = "/repos/" + organization + "/" + repo + "/releases/" + releaseNumber;
            LOG.info("GIT DELETE: " + uri);
            githubClient.delete(uri);
            // delete the tag (makes the next release "stay" in the wrong place)
            wait(organization, repo);
            String uri1 = "/repos/" + organization + "/" + repo + "/git/refs/tags/" + releaseName;
            LOG.info("GIT DELETE: " + uri1);
            githubClient.delete(uri1);
        } catch (RequestException e) {
            if (!e.getMessage().equals("Not Found (404)")) {
                LOG.error("Could not get tag");
                throw new RuntimeException(e);
            }
        } catch (HttpResponseException e) {
            // ignore 404s
            if (e.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                LOG.error("Could not delete release/tag");
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Currently there's no way to get a commit from another branch other than default
        // Setting default branch in order to get commit

        wait(organization, repo);
        Repository repository;

        RepositoryCommit latestRepositoryCommit = null;
        try {
            repository = service.getRepository(organization, repo);
        } catch (IOException e) {
            LOG.info("Could not get repository.");
            throw new RuntimeException(e);
        }
        try {
            List<RepositoryBranch> branches = service.getBranches(repository);
            if (branches.stream().filter(o -> o.getName().equals(releaseName)).findFirst().isPresent()) {
                setDefaultBranch(organization, repo, releaseName);
            } else {
                setDefaultBranch(organization, repo, "master");
            }
        } catch (IOException e) {
            LOG.info("Could not get branches.");
            throw new RuntimeException(e);
        }

        try {
            List<RepositoryCommit> commits;
            commits = commitService.getCommits(repository);
            latestRepositoryCommit = commits.get(0);
        } catch (IOException e) {
            LOG.error("Could not get commits");
        }

        // might use reference later, but cannot figure out how to "target" a reference for branching
        //Reference reference = dService.getReference(fromId, "heads/" + defaultBranch);
        if (latestRepositoryCommit == null) {
            throw new RuntimeException("There is no commit.");
        }
        LOG.info("The SHA1 of the tag is: " + latestRepositoryCommit.getSha());
        try {
            // create branch if needed
            Map<String, Object> branchMap = new HashMap<>();
            branchMap.put("ref", "refs/heads/" + releaseName);
            branchMap.put("sha", latestRepositoryCommit.getSha());
            String uri = "/repos/" + organization + "/" + repo + "/git/refs";
            wait(organization, repo);
            LOG.info("GIT POST: " + uri);
            Object post1 = githubClient.post(uri, branchMap, HashMap.class);
        } catch (RequestException e) {
            // ignore exceptions if reference already exists
            if (!e.getMessage().contains("Reference already exists")) {
                throw new RuntimeException(e);
            } else {
                LOG.debug("Git branch already exists");
            }
        } catch (IOException e1) {
            LOG.error("Could not create branch");
            throw new RuntimeException(e1);
        }

        // no API for creating files on releases? weird
        try {
            Map<String, Object> releaseMap = new HashMap<>();
            releaseMap.put("tag_name", releaseName);
            releaseMap.put("name", releaseName);
            String uri = "/repos/" + organization + "/" + repo + "/releases";
            LOG.info("GIT POST: " + uri);
            wait(organization, repo);
            githubClient.post(uri, releaseMap, Map.class);
        } catch (RequestException e) {
            LOG.info("Git tag already exists");
        } catch (IOException e1) {
            LOG.info("Could not create release/tag");
            throw new RuntimeException(e1);
        }
        return true;
    }
}

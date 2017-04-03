package io.swagger.server.api.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.collect.Lists;
import io.ga4gh.reference.api.GitHubBuilder;
import io.ga4gh.reference.api.QuayIoBuilder;
import io.ga4gh.reference.dao.ToolDAO;
import io.ga4gh.reference.dao.ToolDescriptorDAO;
import io.ga4gh.reference.dao.ToolDockerfileDAO;
import io.ga4gh.reference.dao.ToolVersionDAO;
import io.swagger.server.api.ApiResponseMessage;
import io.swagger.server.api.NotFoundException;
import io.swagger.server.api.ToolsApiService;
import io.swagger.server.model.Tool;
import io.swagger.server.model.ToolDescriptor;
import io.swagger.server.model.ToolDockerfile;
import io.swagger.server.model.ToolTests;
import io.swagger.server.model.ToolVersion;
import org.eclipse.jetty.http.HttpStatus;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolsApiServiceImpl extends ToolsApiService {

    private static final Logger LOG = LoggerFactory.getLogger(ToolsApiServiceImpl.class);

    private final ToolDAO toolDAO;
    private final ToolVersionDAO toolVersionDAO;
    private final ToolDescriptorDAO toolDescriptorDAO;
    private final ToolDockerfileDAO toolDockerfileDAO;
    private final GitHubBuilder gitHubBuilder;
    private final QuayIoBuilder quayIoBuilder;

    public ToolsApiServiceImpl(ToolDAO dao, ToolVersionDAO toolVersionDAO, ToolDescriptorDAO toolDescriptorDAO,
            ToolDockerfileDAO toolDockerfileDAO, GitHubBuilder gitHubBuilder, QuayIoBuilder quayIoBuilder) {
        this.toolDAO = dao;
        this.toolVersionDAO = toolVersionDAO;
        this.toolDescriptorDAO = toolDescriptorDAO;
        this.toolDockerfileDAO = toolDockerfileDAO;
        this.gitHubBuilder = gitHubBuilder;
        this.quayIoBuilder = quayIoBuilder;
    }

    @Override
    public Response toolsGet(String id, String registry, String organization, String name, String toolname, String description,
            String author, String offset, Integer limit, SecurityContext securityContext) throws NotFoundException {
        Iterator<Tool> toolIterator = toolDAO.listAllTools();
        return Response.ok().entity(Lists.newArrayList(toolIterator)).build();
    }

    @Override
    public Response toolsIdGet(String id, SecurityContext securityContext) throws NotFoundException {
        Tool byId = toolDAO.findById(id);
        if (byId != null) {
            return Response.ok().entity(byId).build();
        }
        return Response.status(HttpStatus.NOT_FOUND_404).build();
    }

    @Transaction
    @Override
    public Response toolsIdPut(String id, Tool body, SecurityContext securityContext) throws NotFoundException {
        // ensure that id matches
        if (!Objects.equals(id, body.getId())) {
            return Response.notModified().build();
        }
        int update = toolDAO.update(body);
        if (update != 1) {
            return Response.notModified().build();
        }
        return Response.ok().entity(toolDAO.findById(id)).build();
    }

    @Override
    public Response toolsIdVersionsGet(String id, SecurityContext securityContext) throws NotFoundException {
        Iterator<ToolVersion> toolVersionIterator = toolVersionDAO.listToolVersionsForTool(id);
        if (!toolVersionIterator.hasNext()) {
            return Response.noContent().build();
        }
        return Response.ok().entity(Lists.newArrayList(toolVersionIterator)).build();
    }

    @Transaction
    @Override
    public Response toolsIdVersionsPost(String id, ToolVersion body, SecurityContext securityContext) throws NotFoundException {
        // refresh the release on github
        //gitHubBuilder.createBranchAndRelease(, , body.getName());
        String[] split = id.split("/");
        LOG.info("Creating branch...");
        String organization = split[0];
        String repo = split[1];
        String version = body.getName();
        gitHubBuilder.createBranchAndRelease(organization, repo, version);
        try {
            int insert = toolVersionDAO.insert(id, version);
            if (insert != 1) {
                LOG.info("Tool version already exists in database");
                return Response.notModified().build();
            }
        } catch (UnableToExecuteStatementException e) {
            LOG.info("Tool version already exists in database");
        }
        ToolVersion byId = toolVersionDAO.findByToolVersion(id, version);
        if (byId == null) {
            return Response.notModified().build();
        }
        return Response.ok().entity(byId).build();
    }

    @Transaction
    @Override
    public Response toolsIdVersionsVersionIdDockerfileGet(String id, String versionId, SecurityContext securityContext)
            throws NotFoundException {
        ToolDockerfile byId = toolDockerfileDAO.findById(id, versionId);
        return Response.ok().entity(byId).build();
    }
    private String generateUrl(String toolId, String versionId, String filename){
        return "https://raw.githubusercontent.com/" + toolId + "/" + versionId + "/" + filename;
    }

    @Transaction
    @Override
    public Response toolsIdVersionsVersionIdDockerfilePost(String id, String versionId, ToolDockerfile dockerfile,
            SecurityContext securityContext) throws NotFoundException {
        String[] split = id.split("/");
        String organization = split[0];
        String repo = split[1];
        gitHubBuilder.stashFile(organization, repo, dockerfile.getUrl(), dockerfile.getDockerfile(), versionId);
        gitHubBuilder.createBranchAndRelease(organization, repo, versionId);
        if (!quayIoBuilder.repoExists(organization, repo)) {
            quayIoBuilder.createRepo(organization, repo, repo);
            LOG.info("Created quay.io repository.");
        }
        quayIoBuilder.triggerBuild(organization, organization, repo, repo, versionId, true);
        String url = generateUrl(id, versionId, dockerfile.getUrl());
        dockerfile.setUrl(url);
        ToolDockerfile findById = toolDockerfileDAO.findById(id, versionId);
        if (findById == null) {
            try {
                toolDockerfileDAO.insert(id, versionId, dockerfile.getDockerfile());
                toolDockerfileDAO.update(dockerfile, id, versionId);
            } catch (UnableToExecuteStatementException e) {
                LOG.info("Dockerfile already exists in database");
            }
        }
        toolDockerfileDAO.update(dockerfile, id, versionId);
        ToolDockerfile created = toolDockerfileDAO.findById(id, versionId);

        if (created != null) {
            created.setUrl(quayIoBuilder.getQuayUrl(organization, repo));
            return Response.ok().entity(created).build();
        }
        return Response.serverError().build();
    }

    @Override
    public Response toolsIdVersionsVersionIdGet(String id, String versionId, SecurityContext securityContext) throws NotFoundException {
        ToolVersion byId = toolVersionDAO.findByToolVersion(id, versionId);
        if (byId == null) {
            return Response.notModified().build();
        }
        return Response.ok().entity(byId).build();
    }

    @Transaction
    @Override
    public Response toolsIdVersionsVersionIdPut(String id, String versionId, ToolVersion body, SecurityContext securityContext)
            throws NotFoundException {
        // ensure that id matches
        if (!Objects.equals(versionId, body.getId())) {
            return Response.notModified().build();
        }
        int update = toolVersionDAO.update(body);
        if (update != 1) {
            return Response.notModified().build();
        }
        return Response.ok().entity(toolVersionDAO.findByToolVersion(id, versionId)).build();
    }

    @Override
    public Response toolsIdVersionsVersionIdTypeDescriptorGet(String type, String id, String versionId, SecurityContext securityContext)
            throws NotFoundException {
        // Removed the findById function because it could return more than one ToolDescriptor
        // TODO: change to findByPath
        // ToolDescriptor byId = toolDescriptorDAO.findById(id, versionId, type);
        ToolDescriptor byId = null;
        return Response.ok().entity(byId).build();
    }

    @Transaction
    @Override
    public Response toolsIdVersionsVersionIdTypeDescriptorPost(String type, String id, String versionId, ToolDescriptor body,
            SecurityContext securityContext) throws NotFoundException {
        String[] split = id.split("/");
        String organization = split[0];
        String repo = split[1];
        String path = body.getUrl();
        String url = generateUrl(id, versionId, body.getUrl());
        LOG.info("The URL of the descriptor is: " + url);
        ToolDescriptor byId = toolDescriptorDAO.findByPath(id, versionId, path);
        if (byId == null) {
            try {
                toolDescriptorDAO.insert(body.getDescriptor(), id, versionId, path);
            } catch (UnableToExecuteStatementException e) {
                LOG.debug("Descriptor already exists in database");

            }
        }
        gitHubBuilder.stashFile(organization, repo, body.getUrl(), body.getDescriptor(), versionId);
        // TODO: improve this, this looks slow and awkward
        body.setUrl(url);
        toolDescriptorDAO.update(body, id, versionId, path);
        byId = toolDescriptorDAO.findByPath(id, versionId, path);
        return Response.ok().entity(byId).build();
    }

    @Override
    public Response toolsIdVersionsVersionIdTypeDescriptorRelativePathGet(String type, String id, String versionId, String relativePath,
            SecurityContext securityContext) throws NotFoundException {
        ToolDescriptor byPath = toolDescriptorDAO.findByPath(id, versionId, relativePath);
        return Response.ok().entity(byPath).build();
    }

    @Transaction
    @Override
    public Response toolsIdVersionsVersionIdTypeDescriptorRelativePathPost(String type, String id, String versionId, String relativePath,
            ToolDescriptor body, SecurityContext securityContext) throws NotFoundException {
        // hook up to github

        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Override
    public Response toolsIdVersionsVersionIdTypeTestsGet(String type, String id, String versionId, SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Transaction
    @Override
    public Response toolsIdVersionsVersionIdTypeTestsPut(String type, String id, String versionId, List<ToolTests> body,
            SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Transaction
    @Override
    public Response toolsPost(Tool body, SecurityContext securityContext) throws NotFoundException {
        // try creating a repo on github for this, this should probably be made into a transaction
        if (!gitHubBuilder.repoExists(body.getOrganization(), body.getToolname())) {
            LOG.info("Repo does not exist");
            boolean repo = gitHubBuilder.createRepo(body.getOrganization(), body.getToolname());
            if (!repo) {
                return Response.notModified("Could not create github repo").build();
            }
        }
        try {
            toolDAO.insert(body.getId());
        } catch (UnableToExecuteStatementException e) {
            LOG.info("Tool already exists in database");
        }
        String gitUrl = gitHubBuilder.getGitUrl(body.getOrganization(), body.getToolname());
        body.setUrl(gitUrl);
        toolDAO.update(body);
        Tool byId = toolDAO.findById(body.getId());
        if (byId != null) {
            return Response.ok().entity(byId).build();
        }
        return Response.notModified().build();
    }
}

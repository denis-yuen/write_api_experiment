package io.ga4gh.reference.dao;



import io.ga4gh.reference.mapper.ToolDockerfileMapper;
import io.swagger.server.model.ToolDockerfile;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(ToolDockerfileMapper.class)
public interface ToolDockerfileDAO {

    @SqlUpdate("create table dockerfile ("
            + "url varchar(100) unique, "
            + "dockerfile clob, "
            + "tool_id varchar(100), "
            + "version varchar(100), "
            + "foreign key(tool_id, version) references toolversion(tool_id, version) "
            + ")")
    void createToolDockerfileTable();

    @SqlUpdate("insert into dockerfile (tool_id, version, dockerfile) values (:tool_id, :version, :dockerfile)")
    int insert(@Bind("tool_id") String toolId, @Bind("version") String version, @Bind("dockerfile") String dockerfile);

    @SqlQuery("select * from dockerfile where tool_id = :tool_id and version = :version")
    ToolDockerfile findById(@Bind("tool_id") String toolId, @Bind("version") String version);


    @SqlUpdate("update dockerfile set "
            + "url = :url "
            + "where tool_id = :tool_id and version = :version")
    int update(@BindBean ToolDockerfile t, @Bind("tool_id") String toolId, @Bind("version") String version);

}

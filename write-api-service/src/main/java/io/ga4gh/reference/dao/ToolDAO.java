package io.ga4gh.reference.dao;


import java.util.Iterator;

import io.ga4gh.reference.mapper.ToolMapper;
import io.swagger.server.model.Tool;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Represents the tool table.
 *
 * The tool_id will be of the form quay.io/namespace/repo(/toolname)
 */
@RegisterMapper(ToolMapper.class)
public interface ToolDAO {

    @SqlUpdate("create table tool "
            + "(tool_id varchar(100) primary key, "
            + "toolname varchar(100), "
            + "url varchar(100) unique, "
            + "organization varchar(100), "
            + "description varchar(100), "
            + "author varchar(100), "
            + "verified boolean, "
            + "verifiedSource varchar(100), "
            + "signed boolean, "
            + "metaVersion varchar(100))")
    void createToolTable();

    @SqlUpdate("insert into tool (tool_id) values (:tool_id)")
    int insert(@Bind("tool_id") String toolId);

    @SqlQuery("select * from tool where tool_id = :tool_id")
    Tool findById(@Bind("tool_id") String toolId);

    @SqlQuery("select * from tool")
    Iterator<Tool> listAllTools();

    @SqlUpdate("update tool set url = :url,"
            + "organization = :organization,"
            + "toolname = :toolname,"
            + "description = :description,"
            + "author = :author,"
            + "verified = :verified,"
            + "verifiedSource = :verifiedSource,"
            + "metaVersion = :metaVersion,"
            + "signed = :signed"
            + " where tool_id = :id")
    int update(@BindBean Tool t);

}

package io.ga4gh.reference.dao;

import java.util.Iterator;

import io.ga4gh.reference.mapper.ToolDescriptorMapper;
import io.swagger.server.model.ToolDescriptor;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(ToolDescriptorMapper.class)
public interface ToolDescriptorDAO {

    @SqlUpdate("create table descriptor ("
            + "url varchar(100) unique, "
            + "descriptor clob, "
            + "type varchar(100), "
            + "descriptor_path varchar(100), "
            + "tool_id varchar(100), "
            + "version varchar(100), "
            + "foreign key(tool_id, version) references toolversion(tool_id, version) " + ")")
    void createToolDescriptorTable();

    @SqlUpdate("insert into descriptor (url, type, descriptor_path, tool_id, version) values (:url, :type, :descriptor_path, :tool_id, :version)")
    int insert(
            @Bind("url") String url,
            @Bind("type") String type,
            @Bind("descriptor_path") String descriptorPath,
            @Bind("tool_id") String toolId,
            @Bind("version") String version);

    @SqlQuery("select * from descriptor where tool_id = :tool_id and version = :version and type = :type")
    ToolDescriptor findById(
            @Bind("tool_id") String toolId,
            @Bind("version") String version,
            @Bind("type") String type);

    @SqlQuery("select * from descriptor where tool_id = :tool_id and version = :version and descriptor_path = :descriptor_path")
    ToolDescriptor findByPath(
            @Bind("tool_id") String toolId,
            @Bind("version") String version,
            @Bind("descriptor_path") String descriptorPath);

    @SqlQuery("select * from descriptor where version = :version")
    Iterator<ToolDescriptor> listDescriptorsForTool(@Bind("version") String toolVersionId);

    @SqlUpdate("update descriptor set "
            + "type = :type,"
            + "descriptor_path = :descriptor_path"
            + " where version = :version and descriptor_path = :descriptor_path")
    int update(@BindBean ToolDescriptor t, @Bind("version") String version, @Bind("descriptor_path") String path);

}

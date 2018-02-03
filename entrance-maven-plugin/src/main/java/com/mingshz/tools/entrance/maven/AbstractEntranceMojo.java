package com.mingshz.tools.entrance.maven;

import com.mingshz.tools.entrance.Cluster;
import com.mingshz.tools.entrance.Entrance;
import com.mingshz.tools.entrance.Router;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @author CJ
 */
abstract class AbstractEntranceMojo extends AbstractMojo implements Entrance {

    @Setter
    @Getter
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * 构建image的名称
     */
    @Setter
    @Getter
    @Parameter(defaultValue = "${project.artifactId}-entrance")
    private String name;
    /**
     * 版本，默认就是maven project version
     */
    @Setter
    @Getter
    @Parameter(defaultValue = "${project.version}")
    private String version;

    /**
     * 支持使用api-mocker的服务器列表
     */
    @Setter
    @Getter
    @Parameter
    private List<Cluster> clusters;

    /**
     * 支持使用api-mocker的服务器列表
     */
    @Setter
    @Getter
    @Parameter
    private List<Router> routers;


}

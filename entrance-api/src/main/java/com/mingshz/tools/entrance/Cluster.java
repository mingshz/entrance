package com.mingshz.tools.entrance;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.Serializable;
import java.util.List;

/**
 * 集群
 *
 * @author CJ
 */
@Data
public class Cluster implements Serializable {
    /**
     * 集群名称
     */
    @Parameter
    private String name;

    @Parameter
    private List<Endpoint> targets;
}

package com.mingshz.tools.entrance;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author CJ
 */
@Data
public class Endpoint {
    /**
     * 主机
     */
    @Parameter
    private String host;
    /**
     * 可选的端口
     */
    @Parameter(defaultValue = "80")
    private int port = 80;
    /**
     * 在这个location开始之后马上执行的脚本
     */
    @Parameter
    private String preBlock;
    /**
     * 在这个location最后执行的脚本
     */
    @Parameter
    private String postBlock;
    /**
     * 集群名称；若非空则不再直接访问{@link #host}而是访问集群。
     */
    @Parameter
    private String clusterName;
}

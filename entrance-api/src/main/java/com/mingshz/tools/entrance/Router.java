package com.mingshz.tools.entrance;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.Serializable;
import java.util.List;

/**
 * 路由，特指一个nginx的server
 * https://nginx.org/en/docs/http/server_names.html
 *
 * @author CJ
 */
@Data
public class Router implements Serializable {
    /**
     * https://nginx.org/en/docs/http/server_names.html
     */
    @Parameter
    private String serverName;

    /**
     * 证书名称; null 表示没有在使用ssl
     */
    @Parameter
    private String certificateName;

    /**
     * 支持使用api-mocker的服务器列表
     */
    @Parameter
    private List<ApiServer> apiServers;
    /**
     * 静态服务器列表
     */
    @Parameter
    private List<StaticServer> staticServers;
}

package com.mingshz.tools.entrance;

import java.util.List;

/**
 * 入口
 *
 * @author CJ
 */
public interface Entrance {
    String getName();

    String getVersion();

    List<Router> getRouters();

    List<Cluster> getClusters();

    /**
     * @return 是否需要支持安全协议
     */
    default boolean isSslEnabled() {
        return getRouters() != null && getRouters().stream().anyMatch(router -> router.getCertificateName() != null);
    }
}

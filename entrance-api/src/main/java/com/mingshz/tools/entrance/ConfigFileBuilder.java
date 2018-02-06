package com.mingshz.tools.entrance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * nginx 配置文件生成器
 *
 * @author CJ
 */
public class ConfigFileBuilder {
    private Entrance entrance;

    private ConfigFileBuilder() {
    }

    public static ConfigFileBuilder create() {
        return new ConfigFileBuilder();
    }

    public ConfigFileBuilder forEntrance(Entrance entrance) {
        this.entrance = entrance;
        return this;
    }

    /**
     *
     * @param config
     * @param cwd  可选的运行目录
     * @param file 目标文件
     * @throws IOException
     */
    public void build(Config config, File cwd, File file) throws IOException {


        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"))) {
            // 首先构建我们的 upstream;
            if (entrance.getClusters() != null) {
                for (Cluster cluster : entrance.getClusters()) {
                    writer.append(String.format("upstream %s { \n", cluster.getName()));
                    for (Endpoint endpoint : cluster.getTargets()) {
                        writer.append(String.format("\tserver %s:%d;\n", endpoint.getHost(), endpoint.getPort()));
                    }
                    writer.append("}\n");
                }
                writer.flush();
            }
            if (entrance.getRouters() != null) {
                for (Router router : entrance.getRouters()) {
                    RouterBuilder.create()
                            .forEntrance(router)
                            .build(config, cwd, writer);
                }

            }
        }
    }


}

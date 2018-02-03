package com.mingshz.tools.entrance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author CJ
 */
class TestBase {

    /**
     * @return 一般的入口
     * @throws IOException
     */
    Entrance normalEntrance() throws IOException {
        try (InputStream stream = new ClassPathResource("/normal.json").getInputStream()) {
            final JsonNode entrance = Utils.mapper.readTree(stream);
            return useLocalFile(entrance);
        }
    }

    Entrance normalWithFrontEndsEntrance() throws IOException {
        try (InputStream stream = new ClassPathResource("/withFrontEnds.json").getInputStream()) {
            final JsonNode entrance = Utils.mapper.readTree(stream);
            return useLocalFile(entrance);
        }
    }

    Entrance normalWithSSLEntrance() throws IOException {
        try (InputStream stream = new ClassPathResource("/withSSL.json").getInputStream()) {
            final JsonNode entrance = Utils.mapper.readTree(stream);
            return useLocalFile(entrance);
        }
    }

    private Entrance useLocalFile(JsonNode entrance) throws IOException {
        ObjectNode server = (ObjectNode) entrance.get("routers").get(0).get("apiServers").get(0);
        server.put("localApiFile", new ClassPathResource("/simpleApi.json").getFile().getAbsolutePath());
        return Utils.readFromNode(entrance);
    }
}

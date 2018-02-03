package com.mingshz.tools.entrance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CJ
 */
public class Utils {
    public static final ObjectMapper mapper = new ObjectMapper();

    public static Entrance readFromJson(InputStream inputStream) throws IOException {
        JsonNode root = mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true).readTree(inputStream);
        return readFromNode(root);
    }

    public static Entrance readFromNode(JsonNode root) {
        return new Entrance() {
            @Override
            public String getName() {
                JsonNode name = root.get("name");
                return name != null ? name.asText() : null;
            }

            @Override
            public String getVersion() {
                JsonNode name = root.get("version");
                return name != null ? name.asText() : null;
            }

            @Override
            public List<Router> getRouters() {
                return readList("routers", Router.class);
            }

            @Override
            public List<Cluster> getClusters() {
                return readList("clusters", Cluster.class);
            }

            private <T> List<T> readList(String fieldName, Class<T> targetClass) {
                JsonNode array = root.get(fieldName);
                if (array == null)
                    return null;
                ArrayList<T> list = new ArrayList<>();
                try {
                    final ObjectReader objectReader = mapper.readerFor(targetClass);
                    for (int i = 0; i < array.size(); i++) {
                        JsonNode one = array.get(i);
                        list.add(objectReader.readValue(one));
                    }
                    return list;
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }

        };
    }

}

package com.mingshz.tools.entrance;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * 负责构建一个router
 *
 * @author CJ
 */
public class RouterBuilder {
    private StringWriter httpWriter;
    private StringWriter httpsWriter;
    private File cwd;
    private Pattern uriPattern = Pattern.compile("\\{[a-zA-Z0-9-_]+}");
    private Router router;
    private Config config;

    private RouterBuilder() {
    }

    public static RouterBuilder create() {
        return new RouterBuilder();
    }

    public RouterBuilder forEntrance(Router router) {
        this.router = router;
        return this;
    }

    /**
     * @param config
     * @param cwd    可选的运行目录
     * @param writer 输出目的
     * @throws IOException
     */
    public void build(Config config, File cwd, Writer writer) throws IOException {
        this.cwd = cwd;
        this.config = config;
        httpWriter = new StringWriter();
        if (router.getCertificateName() != null) {
            httpsWriter = new StringWriter();
        }
        start();// server {
        port();
        name();//    server_name  localhost;
        // ssl only
        writeHttps("\tssl on;\n");
        // 实际目录应该是  /etc/nginx/certificate
        writeHttps("\tssl_certificate   certificate/" + router.getCertificateName() + ".pem;\n" +
                "\tssl_certificate_key  certificate/" + router.getCertificateName() + ".key;\n" +
                "\tssl_session_timeout 5m;\n" +
                "\tssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;\n" +
                "\tssl_protocols TLSv1 TLSv1.1 TLSv1.2;\n" +
                "\tssl_prefer_server_ciphers on;\n");
        servers();
        frontEnds();
        end();

        if (httpWriter != null)
            writer.write(httpWriter.toString());
        if (httpsWriter != null)
            writer.write(httpsWriter.toString());
        writer.flush();
    }

    private void frontEnds() throws IOException {
        final List<StaticServer> servers = router.getStaticServers();
        if (servers != null) {
            for (StaticServer server : servers) {
                frontEnd(server);
            }
        }
    }

    private void frontEnd(StaticServer server) throws IOException {
        configProxyPass(server, server.getLocationUri(), false);
    }

    private void servers() throws IOException {
        final List<ApiServer> servers = router.getApiServers();
        if (servers != null) {
            for (ApiServer server : servers) {
                server(server);
            }
        }
    }

    private void server(ApiServer server) throws IOException {
        // 本地文件则直接获取 不然就从网上get anyway 都是一个inputStream
        try (InputStream jsonStream = apiStream(server)) {
            JsonNode paths = Utils.mapper.readTree(jsonStream).get("paths");
            if (paths == null)
                return;
            final Iterator<String> uris = paths.fieldNames();
            while (uris.hasNext()) {
                String path = uris.next();
                // 如果匹配到 {} 则替换为 .+
                // 如果匹配到了
                String locationUri;
                if (uriPattern.matcher(path).find()) {
                    String pathRegex = uriPattern.matcher(path).replaceAll(".+");
                    locationUri = "~ " + pathRegex;
                } else {
                    locationUri = "= " + path;
                }
                JsonNode uriInformation = paths.get(path);
                // 是否存在  schema 为ws or wss 的
                configProxyPass(server, locationUri, isWebSocketURI(uriInformation));
            }
        }
    }

    private boolean isWebSocketURI(JsonNode information) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(information.fields(), Spliterator.SORTED), false)
                .map(Map.Entry::getValue)
                // 只关心它的schemes
                .map(jsonNode -> jsonNode.get("schemes"))
                // 没有的就再贱了
                .filter(Objects::nonNull)
                // 目标是一个数组,全部取出来
                .flatMap(jsonNode ->
                        StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(jsonNode.elements(), Spliterator.SORTED), false
                        )
                )
                .filter(Objects::nonNull)
                .map(JsonNode::asText)
                .anyMatch(type -> "ws".equalsIgnoreCase(type) || "wss".equalsIgnoreCase(type));
    }


    private void configProxyPass(Endpoint endpoint, String locationUri, boolean isWebSocket) throws IOException {
        final boolean ngrok = config != null && config.getNgrokFrom() != null
                && config.getNgrokFrom().equalsIgnoreCase(endpoint.getHost());
        final String target;
        if (ngrok) {
            target = config.getNgrokTo();
        } else if (endpoint.getClusterName() != null) {
            target = endpoint.getClusterName();
        } else {
            final String host = endpoint.getHost();
            final String portSuffix = ":" + endpoint.getPort();
            target = host + portSuffix;
        }

        writeAll(String.format("\tlocation %s {\n", locationUri));
        if (endpoint.getPreBlock() != null)
            writeAll("\t\t" + endpoint.getPreBlock() + "\n");
        if (isWebSocket)
            // https://nginx.org/en/docs/http/websocket.html
            // proxy_pass_request_headers      on;
            writeAll(String.format("\t\tproxy_pass http://%s;\n" +
                            "\t\tproxy_http_version 1.1;\n" +
                            "\t\tproxy_set_header Upgrade $http_Upgrade;\n" +
                            "\t\tproxy_set_header Connection \"Upgrade\";\n"
                    , target));
//            writeAll(String.format("\t\tproxy_pass http://%s%s;\n" +
//                    "\t\tproxy_http_version 1.1;\n" +
//                    "\t\tproxy_set_header        X-Real-IP $remote_addr;\n" +
//                    "\t\tproxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
//                    "\t\tproxy_set_header        Host $http_host;\n"+
//                    "\t\tproxy_pass_request_headers on;\n", host, portSuffix));
        else {
            // 在ngrok模式下 它并不认可传递host和其他代理信息
            writeAll(String.format("\t\tproxy_pass http://%s;\n" +
                    "\t\tproxy_set_header        X-Real-IP $remote_addr;\n" +
                    "\t\tproxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
                    "\t\tproxy_set_header        Host $http_host;\n", target));
        }


        writeHttps("\t\tproxy_set_header        X-Client-Verify  SUCCESS;\n" +
                "\t\tproxy_set_header        X-Client-DN      $ssl_client_s_dn;\n" +
                "\t\tproxy_set_header        X-SSL-Subject    $ssl_client_s_dn;\n" +
                "\t\tproxy_set_header        X-SSL-Issuer     $ssl_client_i_dn;\n");
        if (endpoint.getPostBlock() != null)
            writeAll("\t\t" + endpoint.getPostBlock() + "\n");
        writeAll("\t}\n");
    }

    /**
     * @param server
     * @return 获取json api 的数据流
     */
    private InputStream apiStream(ApiServer server) throws IOException {
        if (server.getLocalApiFile() != null) {
            File target;
            if (cwd != null)
                target = new File(cwd, server.getLocalApiFile());
            else
                target = new File(server.getLocalApiFile());
            return new FileInputStream(target);
        }
        if (server.getProjectId() != null) {
            URL url = new URL(server.isSsl() ? "https" : "http" + "://" + server.getApiServerHost() + "/projectApiJson/" + server.getProjectId() + "/" + server.getBranch());
            return url.openStream();
        }
        throw new IllegalArgumentException(server.toString());
    }

    private void name() throws IOException {
        writeAll("\tserver_name  " + router.getServerName() + ";\n");
    }

    private void start() throws IOException {
        writeAll("server {\n");
    }

    private void end() throws IOException {
        // 默认的配置
//        writeAll("\n" +
//                "\tlocation / {\n" +
//                "\t\troot   /usr/share/nginx/html;\n" +
//                "\t\tindex  index.html index.htm;\n" +
//                "\t}\n" +
//                "\terror_page   500 502 503 504  /50x.html;\n" +
//                "\tlocation = /50x.html {\n" +
//                "\t\troot   /usr/share/nginx/html;\n" +
//                "\t}\n");
        writeAll("}\n");
        if (httpWriter != null)
            httpWriter.flush();
        if (httpsWriter != null)
            httpsWriter.flush();
    }

    private void port() throws IOException {
        writeHttp("\tlisten       80;\n");
        writeHttps("\tlisten       443;\n");
    }

    private void writeHttps(String str) throws IOException {
        if (httpsWriter != null)
            httpsWriter.append(str);
    }

    private void writeHttp(String str) throws IOException {
        if (httpWriter != null)
            httpWriter.append(str);
    }


    private void writeAll(String str) throws IOException {
        writeHttp(str);
        writeHttps(str);
    }


}

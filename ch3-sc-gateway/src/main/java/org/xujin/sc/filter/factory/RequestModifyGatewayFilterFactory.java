package org.xujin.sc.filter.factory;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.DefaultServerRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class RequestModifyGatewayFilterFactory extends AbstractGatewayFilterFactory<RequestModifyGatewayFilterFactory.Config> {

    private static final String KEY = "withParams";

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(KEY);
    }

    public RequestModifyGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerRequest serverRequest = new DefaultServerRequest(exchange);
            String method = serverRequest.methodName();
            if ("POST".equals(method)) {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(exchange.getRequest().getHeaders());

                // the new content type will be computed by bodyInserter
                // and then set in the request decorator
                headers.remove(HttpHeaders.CONTENT_LENGTH);

                // if the body is changing content types, set it here, to the bodyInserter will know about it
                if (config.getContentType() != null) {
                    headers.set(HttpHeaders.CONTENT_TYPE, config.getContentType());
                }
                ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        long contentLength = headers.getContentLength();
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.putAll(super.getHeaders());
                        if (contentLength > 0) {
                            httpHeaders.setContentLength(contentLength);
                        } else {
                            httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                        }
                        return httpHeaders;
                    }

                    @Override
                    public Flux<DataBuffer> getBody() {
                        Flux<DataBuffer> messageBody = exchange.getRequest().getBody();
                        StringBuilder jsonStr = new StringBuilder();
                        //Flux 转mono，转换为单一响应对象，只会进行一次处理
                        Mono<List<DataBuffer>> mono = messageBody.collectList();
                        //flatMap 进行数据转换，先循环把所有数据转换成一条String
                        return mono.flatMap((data)->{
                            for (DataBuffer buffer : data) {
                                byte[] content = new byte[buffer.readableByteCount()];
                                buffer.read(content);
                                DataBufferUtils.release(buffer);
                                jsonStr.append(new String(content, Charset.forName("UTF-8")));
                            }
                            JSONObject reqJson = JSONObject.parseObject(jsonStr.toString());
                            reqJson.put("username",headers.get("username"));
                            DataBuffer bodyDataBuffer = stringBuffer(reqJson.toJSONString());
                            return Mono.just(bodyDataBuffer);
                        }).flux();
                    }
                };
                return chain.filter(exchange.mutate().request(decorator).build());
            }else{
                return chain.filter(exchange);
            }
        };
    }

    /**
     * String 转换 DataBuffer
     * @param value
     * @return
     */
    private DataBuffer stringBuffer(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
        buffer.write(bytes);
        return buffer;
    }

    public static class Config {
        private String contentType;

        public Config() {
        }

        public String getContentType() {
            return this.contentType;
        }

        public RequestModifyGatewayFilterFactory.Config setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
    }
}


package org.xujin.sc.filter;

import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 调用鉴权
 */
@Component
public class AuthSignatureFilter implements GlobalFilter, Ordered {
    private static final String COUNT_Start_TIME = "countStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(COUNT_Start_TIME, System.currentTimeMillis());
        String token = exchange.getRequest().getQueryParams().getFirst("authToken");
        if (token == null || token.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            byte[] bytes = getErrorResponse("-1","authError");
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Flux.just(buffer));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -200;
    }


    private byte[] getErrorResponse(String errorCode,String errorMessage){
        JSONObject json = new JSONObject();

        json.put("status",errorCode);
        json.put("msg",errorMessage);

        return json.toJSONString().getBytes(StandardCharsets.UTF_8);
    }
}

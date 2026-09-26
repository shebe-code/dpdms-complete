package com.dpdms.gateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args){SpringApplication.run(ApiGatewayApplication.class,args);}
    @Bean RouteLocator routes(RouteLocatorBuilder b){return b.routes()
        .route("auth",r->r.path("/api/v1/auth/**").uri("lb://AUTH-SERVICE"))
        .route("flood",r->r.path("/api/v1/floods/**").uri("lb://FLOOD-SERVICE"))
        .route("drought",r->r.path("/api/v1/droughts/**").uri("lb://DROUGHT-SERVICE"))
        .route("fire",r->r.path("/api/v1/fires/**").uri("lb://FIRE-SERVICE"))
        .route("zoonotic",r->r.path("/api/v1/zoonotic/**").uri("lb://ZOONOTIC-SERVICE"))
        .route("mining",r->r.path("/api/v1/mining-accidents/**").uri("lb://MINING-SERVICE"))
        .route("reports",r->r.path("/api/v1/reports/**").uri("lb://REPORT-SERVICE"))
        .route("alerts",r->r.path("/api/v1/alerts/**").uri("lb://ALERT-SERVICE"))
        .route("dashboard",r->r.path("/api/v1/dashboard/**").uri("lb://DASHBOARD-SERVICE"))
        .build();}
}
@Component
class JwtGatewayFilter implements GlobalFilter, Ordered {
    private final SecretKey key;private final String gatewaySecret;
    JwtGatewayFilter(@Value("${dpdms.jwt-secret}")String jwtSecret,@Value("${dpdms.gateway-secret}")String gatewaySecret){key=Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));this.gatewaySecret=gatewaySecret;}
    public int getOrder(){return -100;}
    public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){
        String path=exchange.getRequest().getURI().getPath();
        if(exchange.getRequest().getMethod()==HttpMethod.OPTIONS||path.startsWith("/api/v1/auth/")||path.startsWith("/actuator/"))return chain.filter(exchange);
        String authorization=exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authorization==null||!authorization.startsWith("Bearer "))return write(exchange,401,"Authentication required.");
        try{
            Claims claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(authorization.substring(7)).getPayload();
            var request=exchange.getRequest().mutate().headers(headers->{
                headers.remove("X-User-Role");headers.remove("X-User-Hazard");headers.remove("X-User-Username");headers.remove("X-User-Ward");headers.remove("X-User-District");headers.remove("X-User-Province");
                headers.set("X-User-Role",claims.get("role",String.class));headers.set("X-User-Hazard",claims.get("hazard",String.class));headers.set("X-User-Username",claims.getSubject());
                headers.set("X-User-Ward",Objects.toString(claims.get("ward"),""));headers.set("X-User-District",Objects.toString(claims.get("district"),"Rushinga"));headers.set("X-User-Province",Objects.toString(claims.get("province"),"Mashonaland Central"));headers.set("X-Gateway-Secret",gatewaySecret);
            }).build();
            return chain.filter(exchange.mutate().request(request).build());
        }catch(Exception ex){return write(exchange,401,"Invalid or expired token.");}
    }
    private Mono<Void> write(ServerWebExchange exchange,int status,String message){
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body=("{\"error\":\""+status+"\",\"message\":\""+message+"\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}

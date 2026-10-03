
package com.dpdms.dashboard;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
/**
 * Provides CRUD and approval operations on fire incidents.
 */

@SpringBootApplication
public class DashboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardServiceApplication.class, args);
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

@Component
class DashboardAggregator {

    private final ObjectProvider<DiscoveryClient> provider;
    private final RestClient.Builder builder;
    private final String secret;

    DashboardAggregator(
            ObjectProvider<DiscoveryClient> provider,
            RestClient.Builder builder,
            @Value("${dpdms.gateway-secret}") String secret) {

        this.provider = provider;
        this.builder = builder;
        this.secret = secret;
    }

    List<Map<String, Object>> approved(
            String service,
            Map<String, String> headers) {

        DiscoveryClient discoveryClient = provider.getIfAvailable();

        if (discoveryClient == null) {
            return List.of();
        }

        List<ServiceInstance> instances =
                discoveryClient.getInstances(service);

        if (instances.isEmpty()) {
            return List.of();
        }

        String path = switch (service) {
            case "FLOOD-SERVICE" ->
                    "/api/v1/floods/approved";

            case "DROUGHT-SERVICE" ->
                    "/api/v1/droughts/approved";

            case "FIRE-SERVICE" ->
                    "/api/v1/fires/approved";

            case "ZOONOTIC-SERVICE" ->
                    "/api/v1/zoonotic/approved";

            default ->
                    "/api/v1/mining-accidents/approved";
        };

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set("X-Gateway-Secret", secret);

        headers.forEach(httpHeaders::set);

        ResponseEntity<List<Map<String, Object>>> response =
                builder.build()
                        .get()
                        .uri(instances.get(0).getUri() + path)
                        .headers(existingHeaders ->
                                existingHeaders.addAll(httpHeaders))
                        .retrieve()
                        .toEntity(
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                }
                        );

        return response.getBody() == null
                ? List.of()
                : response.getBody();
    }
}

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {

    private final DashboardAggregator aggregator;
    private final String gatewaySecret;

    DashboardController(
            DashboardAggregator aggregator,
            @Value("${dpdms.gateway-secret}") String gatewaySecret) {

        this.aggregator = aggregator;
        this.gatewaySecret = gatewaySecret;
    }

    @GetMapping("/summary")
    Map<String, Object> summary(
            @RequestHeader("X-Gateway-Secret") String gatewayHeader,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Hazard") String userHazard,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader(value = "X-User-Ward", required = false) String ward,
            @RequestHeader(value = "X-User-District", required = false) String district,
            @RequestHeader(value = "X-User-Province", required = false) String province) {

        if (!Objects.equals(gatewayHeader, gatewaySecret)) {
            throw new DashboardForbiddenException("Forbidden");
        }

        Map<String, String> headers = Map.of(
                "X-User-Role", role,
                "X-User-Hazard", userHazard,
                "X-User-Username", username,
                "X-User-Ward", Objects.toString(ward, ""),
                "X-User-District", Objects.toString(district, ""),
                "X-User-Province", Objects.toString(province, "")
        );

        List<Map<String, Object>> all = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();

        for (String service : List.of(
                "FLOOD-SERVICE",
                "DROUGHT-SERVICE",
                "FIRE-SERVICE",
                "ZOONOTIC-SERVICE",
                "MINING-SERVICE")) {

            try {
                for (Map<String, Object> incident :
                        aggregator.approved(service, headers)) {

                    Map<String, Object> incidentCopy =
                            new LinkedHashMap<>(incident);

                    incidentCopy.put(
                            "hazard",
                            service.replace("-SERVICE", "")
                    );

                    all.add(incidentCopy);
                }

            } catch (Exception e) {
                unavailable.add(service);
            }
        }

        Map<String, Long> byHazard =
                all.stream()
                        .collect(Collectors.groupingBy(
                                incident ->
                                        Objects.toString(
                                                incident.get("hazard")
                                        ),
                                TreeMap::new,
                                Collectors.counting()
                        ));

        Map<String, Long> bySeverity =
                all.stream()
                        .collect(Collectors.groupingBy(
                                incident ->
                                        Objects.toString(
                                                incident.get("severity")
                                        ),
                                TreeMap::new,
                                Collectors.counting()
                        ));

        Map<String, Long> byStatus =
                all.stream()
                        .collect(Collectors.groupingBy(
                                incident ->
                                        Objects.toString(
                                                incident.get("status")
                                        ),
                                TreeMap::new,
                                Collectors.counting()
                        ));

        List<Map<String, Object>> recent =
                all.stream()
                        .sorted(
                                Comparator.comparing(
                                        incident ->
                                                Objects.toString(
                                                        incident.get(
                                                                "occurrenceAt"
                                                        )
                                                ),
                                        Comparator.reverseOrder()
                                )
                        )
                        .limit(10)
                        .toList();

        Map<String, Long> trend =
                all.stream()
                        .filter(
                                incident ->
                                        incident.get("occurrenceAt") != null
                        )
                        .collect(Collectors.groupingBy(
                                incident -> {

                                    try {
                                        return LocalDateTime
                                                .parse(
                                                        Objects.toString(
                                                                incident.get(
                                                                        "occurrenceAt"
                                                                )
                                                        )
                                                )
                                                .toLocalDate()
                                                .toString();

                                    } catch (Exception e) {
                                        return "UNKNOWN";
                                    }
                                },
                                TreeMap::new,
                                Collectors.counting()
                        ));

        List<Map<String, Object>> mapIncidents =
                all.stream()
                        .filter(
                                incident ->
                                        incident.get("latitude") != null
                                                && incident.get("longitude") != null
                        )
                        .map(incident -> {

                            Map<String, Object> mapIncident =
                                    new LinkedHashMap<>();

                            mapIncident.put(
                                    "id",
                                    incident.get("id")
                            );

                            mapIncident.put(
                                    "hazard",
                                    incident.get("hazard")
                            );

                            mapIncident.put(
                                    "ward",
                                    incident.get("ward")
                            );

                            mapIncident.put(
                                    "severity",
                                    incident.get("severity")
                            );

                            mapIncident.put(
                                    "occurrenceAt",
                                    incident.get("occurrenceAt")
                            );

                            mapIncident.put(
                                    "latitude",
                                    incident.get("latitude")
                            );

                            mapIncident.put(
                                    "longitude",
                                    incident.get("longitude")
                            );

                            return mapIncident;
                        })
                        .toList();

        return Map.of(
                "totalApproved",
                all.size(),

                "countsByHazard",
                byHazard,

                "countsBySeverity",
                bySeverity,

                "countsByStatus",
                byStatus,

                "recentIncidents",
                recent,

                "trend",
                trend,

                "mapIncidents",
                mapIncidents,

                "unavailableHazards",
                unavailable,

                "lastRefresh",
                LocalDateTime.now()
        );
    }

    @GetMapping("/incidents")
    List<Map<String, Object>> incidents(
            @RequestHeader("X-Gateway-Secret") String gatewayHeader,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Hazard") String userHazard,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader(value = "X-User-Ward", required = false) String ward,
            @RequestHeader(value = "X-User-District", required = false) String district,
            @RequestHeader(value = "X-User-Province", required = false) String province) {

        return (List<Map<String, Object>>) summary(
                gatewayHeader,
                role,
                userHazard,
                username,
                ward,
                district,
                province
        ).get("mapIncidents");
    }
}

class DashboardForbiddenException extends RuntimeException {

    DashboardForbiddenException(String message) {
        super(message);
    }
}

@RestControllerAdvice
class DashboardErrors {

    @ExceptionHandler(DashboardForbiddenException.class)
    ResponseEntity<?> forbidden(DashboardForbiddenException exception) {

        return ResponseEntity
                .status(403)
                .body(
                        Map.of(
                                "error",
                                "FORBIDDEN",

                                "message",
                                exception.getMessage()
                        )
                );
    }
}

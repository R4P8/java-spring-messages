package com.example.myapp.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

@Component
public class TrafficScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(TrafficScheduler.class);

    private final HttpClient client = HttpClient.newHttpClient();
    private final Random random = new Random();

    @Scheduled(cron = "*/30 * * * * *") // setiap 30 detik
    public void generateTraffic() {

        try {

            // 80% normal, 20% error
            String endpoint;

            if (random.nextInt(10) < 8) {
                endpoint = "http://localhost:8004/api/messages";
            } else {
                endpoint = "http://localhost:8004/api/messages/error";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("Traffic generated. Endpoint={} Status={}",
                    endpoint,
                    response.statusCode());

        } catch (Exception e) {
            logger.error("Failed generate traffic", e);
        }
    }
}

package blog.collection.gateway_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {
    @Bean
    public Queue blackListTokenQueue() {
        return new Queue("blacklist-token-queue", true);
    }
}

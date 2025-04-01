package blog.collection.gateway_service.service;

import blog.collection.gateway_service.repository.BlackListTokenRepository;
import blog.collection.gateway_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlackListTokenService {

    private final BlackListTokenRepository blackListTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @RabbitListener(queues = "blacklist-token-queue")
    public void handleBlackListToken(String token) {
        long timeRemaining = jwtTokenProvider.getTimeRemainingOfToken(token);
        if (timeRemaining > 0) {
            blackListTokenRepository.addTokenIntoBlackList(token, timeRemaining);
        }
    }
}

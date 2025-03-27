package blog.collection.gateway_service.controller;

import blog.collection.gateway_service.security.BlackListToken;
import blog.collection.gateway_service.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {
    @Autowired
    private BlackListToken blackListToken;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/blacklist-token")
    public ResponseEntity<String> blacklistToken(@RequestBody String token) {
        try {
            long timeRemaining = jwtTokenProvider.getTimeRemainingOfToken(token);
            if (timeRemaining > 0) {
                blackListToken.addTokenIntoBlackList(token, timeRemaining);
            }
            return ResponseEntity.ok("Token blacklisted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to blacklist token: " + e.getMessage());
        }
    }
}

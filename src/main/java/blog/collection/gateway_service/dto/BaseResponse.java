package blog.collection.gateway_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class BaseResponse<T> {
    private int responseCode;
    private String responseMessage;
    private T responseData;

    public BaseResponse(T responseData) {
        this.responseData = responseData;
    }
}

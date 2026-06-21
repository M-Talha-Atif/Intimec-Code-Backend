package com.aidentx.access_portal_service.validation.access_request;


import com.aidentx.access_portal_service.dto.request.access_request.SubmitAccessRequestDTO;
import com.aidentx.access_portal_service.entity.cart.CartItem;
import com.aidentx.access_portal_service.exception.ValidationException;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AccessRequestValidator {

    public void validateSubmitRequest(SubmitAccessRequestDTO dto, List<CartItem> cartItems) {
        Map<String, String> errors = new HashMap<>();

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            errors.put("items", "At least one request item is required");
        }

        if (dto.getOverallJustification() == null || dto.getOverallJustification().isBlank()) {
            errors.put("overallJustification", "Overall justification is required");
        }


        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid access request submission", errors);
        }
    }


}

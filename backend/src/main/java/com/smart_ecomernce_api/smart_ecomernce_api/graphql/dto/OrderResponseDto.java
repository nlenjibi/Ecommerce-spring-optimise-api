package com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OrderResponseDto {
    private List<OrderResponse> content;
    private PaginatedResponse<OrderResponse> pageInfo;


}

package com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProductDto {
    private List<ProductResponse> content;
    private PaginatedResponse<ProductResponse> pageInfo;


}

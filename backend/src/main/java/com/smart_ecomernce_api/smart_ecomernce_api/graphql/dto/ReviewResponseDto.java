package com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ReviewResponseDto {
    private List<ReviewResponse> content;
    private PaginatedResponse<ReviewResponse> pageInfo;
}

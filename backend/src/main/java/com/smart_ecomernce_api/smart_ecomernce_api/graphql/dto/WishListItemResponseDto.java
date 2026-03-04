package com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistItemDto;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter


public class WishListItemResponseDto {
    private List<WishlistItemDto> content;
    private PaginatedResponse<WishlistItemDto> pageInfo;

}

package com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserDto;
import lombok.*;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserResponceDto {
    private List<UserDto> content;
    private PaginatedResponse<UserDto> pageInfo;

}

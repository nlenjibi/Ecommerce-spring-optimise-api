package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CartMapper {

    @Mapping(target = "totalPrice", expression = "java(cart.getTotalPrice())")
    @Mapping(target = "items", source = "items")
    CartDto toDto(Cart cart);

    List<CartDto> toDtoList(List<Cart> carts);

    @Mapping(target = "totalPrice", expression = "java(cartItem.getTotalPrice())")
    CartItemDto toDto(CartItem cartItem);


}

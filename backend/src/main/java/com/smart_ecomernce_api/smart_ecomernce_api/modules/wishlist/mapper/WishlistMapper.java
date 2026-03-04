package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.entity.WishlistItem;
import org.mapstruct.*;


@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WishlistMapper {

    /**
     * Maps a {@link WishlistItem} to its DTO representation.
     *
     * Computed fields use expression() so MapStruct delegates to the entity's
     * own business-logic methods rather than duplicating the logic here.
     */
    @Mapping(source = "user.id",   target = "userId")
    @Mapping(source = "createdAt", target = "addedAt")
    @Mapping(source = "product",   target = "product",              qualifiedByName = "toProductSummary")
    @Mapping(target  = "currentPrice",           expression = "java(item.getProduct().getEffectivePrice())")
    @Mapping(target  = "inStock",                expression = "java(item.getProduct().isInStock())")
    WishlistItemDto toDto(WishlistItem item);

    /**
     * Produces the lightweight {@link WishlistItemDto.ProductSummary} nested object.
     * Declared as a default method so the mapper interface stays simple while
     * still allowing the custom builder pattern required by the DTO.
     */
    @Named("toProductSummary")
    default WishlistItemDto.ProductSummary toProductSummary(Product product) {
        if (product == null) return null;

        return WishlistItemDto.ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .inStock(product.isInStock())
                .availableQuantity(product.getAvailableQuantity())
                .inventoryStatus(product.getInventoryStatus() != null
                        ? product.getInventoryStatus().name()
                        : null)
                .build();
    }
}
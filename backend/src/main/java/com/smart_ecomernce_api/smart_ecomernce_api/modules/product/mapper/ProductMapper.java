package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.ProductImage;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "slug", source = "slug")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "costPrice", source = "costPrice")
    @Mapping(target = "effectivePrice", expression = "java(product.getEffectivePrice())")
    @Mapping(target = "discountPercentage", expression = "java(product.getDiscountPercentage())")
    @Mapping(target = "stockQuantity", source = "stockQuantity")
    @Mapping(target = "reservedQuantity", source = "reservedQuantity")
    @Mapping(target = "availableQuantity", expression = "java(product.getAvailableQuantity())")
    @Mapping(target = "lowStockThreshold", source = "lowStockThreshold")
    @Mapping(target = "reorderPoint", source = "reorderPoint")
    @Mapping(target = "reorderQuantity", source = "reorderQuantity")
    @Mapping(target = "maxStockQuantity", source = "maxStockQuantity")
    @Mapping(target = "inventoryStatus", expression = "java(product.getInventoryStatus() != null ? product.getInventoryStatus().name() : null)")
    @Mapping(target = "trackInventory", source = "trackInventory")
    @Mapping(target = "allowBackorder", source = "allowBackorder")
    @Mapping(target = "expectedRestockDate", source = "expectedRestockDate")
    @Mapping(target = "lastRestockedAt", source = "lastRestockedAt")
    @Mapping(target = "featured", source = "featured")
    @Mapping(target = "isNew", source = "isNew")
    @Mapping(target = "isBestseller", source = "isBestseller")
    @Mapping(target = "viewCount", expression = "java(product.getViewCount() != null ? product.getViewCount() : 0L)")
    @Mapping(target = "salesCount", expression = "java(product.getSalesCount() != null ? product.getSalesCount() : 0L)")
    @Mapping(target = "ratingCount", expression = "java(product.getRatingCount() != null ? product.getRatingCount() : 0)")
    @Mapping(target = "ratingAverage", expression = "java(product.getRatingAverage() != null ? product.getRatingAverage() : java.math.BigDecimal.ZERO)")
    @Mapping(target = "discountPrice", expression = "java(product.getDiscountPrice() != null ? product.getDiscountPrice() : java.math.BigDecimal.ZERO)")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "thumbnailUrl", source = "thumbnailUrl")
    @Mapping(target = "additionalImages", source = "additionalImages")
    @Mapping(target = "metaTitle", source = "metaTitle")
    @Mapping(target = "metaDescription", source = "metaDescription")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "category", expression = "java(toCategoryInfo(product.getCategory()))")
    @Mapping(target = "images", expression = "java(toProductImageResponseList(product.getImages()))")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    ProductResponse toDto(Product product);

    Product toEntity(ProductCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "discountPrice", source = "discountedPrice")
    @Mapping(target = "id", ignore = true)
    void update(ProductUpdateRequest request, @MappingTarget Product product);

    default ProductResponse.CategoryInfo toCategoryInfo(Category category) {
        if (category == null) return null;
        return ProductResponse.CategoryInfo.builder()
                .id(category.getId())
                .slug(category.getSlug())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .build();
    }

    default List<ProductResponse.ProductImageResponse> toProductImageResponseList(List<ProductImage> images) {
        if (images == null) return new java.util.ArrayList<>();
        return images.stream().map(this::toProductImageResponse).collect(Collectors.toList());
    }

    default ProductResponse.ProductImageResponse toProductImageResponse(ProductImage image) {
        if (image == null) return null;
        return ProductResponse.ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.getIsPrimary() != null ? image.getIsPrimary() : false)
                .build();
    }
}

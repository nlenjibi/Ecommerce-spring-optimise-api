package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MapStruct mapper for Review entity and DTOs
 * Handles conversion between entity and various DTO representations
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {LocalDateTime.class}
)
public interface ReviewMapper {

    /**
     * Convert Review entity to ReviewResponse DTO
     */
    @Named("toDto")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSlug", source = "product.slug")
    @Mapping(target = "user.id", source = "user.id")
    @Mapping(target = "user.firstName", source = "user.firstName")
    @Mapping(target = "user.lastName", source = "user.lastName")
    @Mapping(target = "user.email", source = "user.email")
    ReviewResponse toDto(Review review);

    @IterableMapping(qualifiedByName = "toDto")
    List<ReviewResponse> toDtoList(List<Review> reviews);

    /**
     * Convert ReviewCreateRequest to Review entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "verifiedPurchase", constant = "false")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "helpfulCount", constant = "0")
    @Mapping(target = "notHelpfulCount", constant = "0")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "adminResponse", ignore = true)
    @Mapping(target = "adminResponseAt", ignore = true)
    @Mapping(target = "adminResponseBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    Review toEntity(ReviewCreateRequest request);

    /**
     * Update existing Review entity from ReviewUpdateRequest
     * Only updates non-null fields
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "verifiedPurchase", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "helpfulCount", ignore = true)
    @Mapping(target = "notHelpfulCount", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "adminResponse", ignore = true)
    @Mapping(target = "adminResponseAt", ignore = true)
    @Mapping(target = "adminResponseBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(ReviewUpdateRequest request, @MappingTarget Review review);

    /**
     * Map Review to simplified DTO (for lists)
     */
    @Named("toSimplifiedDto")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "user.id", source = "user.id")
    @Mapping(target = "user.firstName", source = "user.firstName")
    @Mapping(target = "user.lastName", source = "user.lastName")
    @Mapping(target = "user.email", source = "user.email")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "pros", ignore = true)
    @Mapping(target = "cons", ignore = true)
    ReviewResponse toSimplifiedDto(Review review);

    @IterableMapping(qualifiedByName = "toSimplifiedDto")
    List<ReviewResponse> toSimplifiedDtoList(List<Review> reviews);
}

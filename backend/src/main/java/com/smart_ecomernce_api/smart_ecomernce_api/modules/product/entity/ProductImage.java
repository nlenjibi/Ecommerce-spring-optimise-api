package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "alt_text")
    private String altText;

    @Builder.Default
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
}

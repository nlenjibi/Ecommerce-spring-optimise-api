package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Review entity representing product reviews with ratings, comments, and media
 *
 * Features:
 * - Star ratings (1-5)
 * - Optional title and detailed comment
 * - Pros and cons lists
 * - Image attachments
 * - Verified purchase badge
 * - Helpful/Not helpful voting
 * - Admin moderation and responses
 * - Soft delete support
 */
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_user", columnList = "user_id"),
        @Index(name = "idx_review_rating", columnList = "rating"),
        @Index(name = "idx_review_verified", columnList = "verified_purchase"),
        @Index(name = "idx_review_approved", columnList = "approved"),
        @Index(name = "idx_review_created", columnList = "created_at"),
        @Index(name = "idx_review_deleted", columnList = "deleted")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_product", columnNames = {"user_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"product", "user"})
@EqualsAndHashCode(callSuper = true, exclude = {"product", "user"})
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_product"))
    @NotNull(message = "Product is required")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_user"))
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "rating", nullable = false)
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Column(name = "title", length = 200)
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Review comment is required")
    @Size(min = 10, max = 2000, message = "Comment must be between 10 and 2000 characters")
    private String comment;

    @Column(name = "verified_purchase", nullable = false)
    @Builder.Default
    private Boolean verifiedPurchase = false;

    @Column(name = "approved", nullable = false)
    @Builder.Default
    private Boolean approved = true;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "not_helpful_count", nullable = false)
    @Builder.Default
    private Integer notHelpfulCount = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_images",
            joinColumns = @JoinColumn(name = "review_id", foreignKey = @ForeignKey(name = "fk_review_images"))
    )
    @Column(name = "image_url", length = 500)
    @OrderColumn(name = "image_order")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_pros",
            joinColumns = @JoinColumn(name = "review_id", foreignKey = @ForeignKey(name = "fk_review_pros"))
    )
    @Column(name = "pro", length = 500)
    @OrderColumn(name = "pro_order")
    @Builder.Default
    private List<String> pros = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_cons",
            joinColumns = @JoinColumn(name = "review_id", foreignKey = @ForeignKey(name = "fk_review_cons"))
    )
    @Column(name = "con", length = 500)
    @OrderColumn(name = "con_order")
    @Builder.Default
    private List<String> cons = new ArrayList<>();

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "admin_response_at")
    private java.time.LocalDateTime adminResponseAt;

    @Column(name = "admin_response_by")
    private Long adminResponseBy;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "has_images", nullable = false)
    @Builder.Default
    private Boolean hasImages = false;

    @Column(name = "flagged", nullable = false)
    @Builder.Default
    private Boolean flagged = false;

    // Computed field for helpfulness score
    @Formula("(helpful_count * 1.0) / NULLIF(helpful_count + not_helpful_count, 0)")
    private Double helpfulnessScore;

    // Business logic methods

    public void incrementHelpful() {
        this.helpfulCount++;
    }

    public void incrementNotHelpful() {
        this.notHelpfulCount++;
    }

    public void approve() {
        this.approved = true;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.approved = false;
        this.rejectionReason = reason;
    }

    public void addAdminResponse(String response, Long adminId) {
        this.adminResponse = response;
        this.adminResponseAt = java.time.LocalDateTime.now();
        this.adminResponseBy = adminId;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = java.time.LocalDateTime.now();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }

    public boolean canBeEditedBy(Long userId) {
        return this.user.getId().equals(userId) && !this.deleted;
    }

    public boolean canBeDeletedBy(Long userId) {
        return this.user.getId().equals(userId) && !this.deleted;
    }



}
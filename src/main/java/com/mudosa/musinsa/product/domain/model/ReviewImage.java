package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_image")
public class ReviewImage extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_image_id")
    private Long reviewImageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
    
    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;
    
    @Builder
    public ReviewImage(Review review, String imageUrl) {
        this.review = review;
        this.imageUrl = imageUrl;
    }
    
    // 도메인 로직: 리뷰 변경
    public void changeReview(Review review) {
        if (review != null) this.review = review;
    }
    
    // 도메인 로직: 이미지 URL 변경
    public void changeImageUrl(String imageUrl) {
        if (imageUrl != null) this.imageUrl = imageUrl;
    }
    
    // 도메인 로직: 특정 리뷰 소속 여부 확인
    public boolean belongsToReview(Review review) {
        return this.review != null && this.review.equals(review);
    }
    
    // 도메인 로직: 이미지 URL 존재 여부 확인
    public boolean hasImageUrl() {
        return this.imageUrl != null && !this.imageUrl.trim().isEmpty();
    }
}
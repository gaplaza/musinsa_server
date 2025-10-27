package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import com.mudosa.musinsa.order.domain.model.OrderProduct;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review")
public class Review extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_id", nullable = false)
    private OrderProduct orderProduct;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "rating", nullable = false)
    private Integer rating;
    

    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<ReviewImage> reviewImages = new java.util.ArrayList<>();
    
    @Builder
    public Review(OrderProduct orderProduct, Long userId, String content, Integer rating) {
        this.orderProduct = orderProduct;
        this.userId = userId;
        this.content = content;
        this.rating = rating;
    }
    
    // 도메인 로직: 정보 수정
    public void modify(String content, Integer rating) {
        if (content != null) this.content = content;
        if (rating != null) this.rating = rating;
    }
    
    // 도메인 로직: 주문 상품 변경
    public void changeOrderProduct(OrderProduct orderProduct) {
        if (orderProduct != null) this.orderProduct = orderProduct;
    }
    
    // 도메인 로직: 사용자 변경
    public void changeUser(Long userId) {
        if (userId != null) this.userId = userId;
    }
    

    
    // 도메인 로직: 특정 주문 상품의 리뷰 여부 확인
    public boolean belongsToOrderProduct(OrderProduct orderProduct) {
        return this.orderProduct != null && this.orderProduct.equals(orderProduct);
    }
    
    // 도메인 로직: 특정 사용자의 리뷰 여부 확인
    public boolean belongsToUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }
    

    
    // 도메인 로직: 리뷰 이미지 추가
    public void addReviewImage(ReviewImage reviewImage) {
        if (reviewImage != null) {
            this.reviewImages.add(reviewImage);
        }
    }
    
    // 도메인 로직: 리뷰 이미지 제거
    public void removeReviewImage(ReviewImage reviewImage) {
        if (reviewImage != null) {
            this.reviewImages.remove(reviewImage);
        }
    }

}
package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import com.mudosa.musinsa.product.domain.vo.OptionNameValue;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "option_name", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uniq_option_name", columnNames = {"option_name"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionName extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_name_id")
    private Long optionNameId;

    @Embedded
    private OptionNameValue optionName;

    // 연관관계 - OptionValue 연결
    @OneToMany(mappedBy = "optionName", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OptionValue> optionValues = new ArrayList<>();

    // 생성 메서드
    public static OptionName create(String optionName) {
        return new OptionName(OptionNameValue.of(optionName));
    }

    // 비즈니스 메서드
    public void updateOptionName(String optionName) {
        this.optionName = OptionNameValue.of(optionName);
    }

    // 연관관계 메서드 - OptionValue 연결
    public void addOptionValue(OptionValue optionValue) {
        optionValues.add(optionValue);
    }

    public void removeOptionValue(OptionValue optionValue) {
        optionValues.remove(optionValue);
    }

    // JPA를 위한 protected 생성자
    protected OptionName(OptionNameValue optionName) {
        this.optionName = optionName;
    }
}
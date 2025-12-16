package com.mudosa.musinsa.payment.application.service;

import com.mudosa.musinsa.exception.BusinessException;
import com.mudosa.musinsa.exception.ErrorCode;
import com.mudosa.musinsa.order.application.OrderService;
import com.mudosa.musinsa.order.domain.model.Order;
import com.mudosa.musinsa.order.domain.repository.OrderRepository;
import com.mudosa.musinsa.payment.application.dto.PaymentCreateDto;
import com.mudosa.musinsa.payment.application.dto.PaymentCreationResult;
import com.mudosa.musinsa.payment.application.dto.PaymentResponseDto;
import com.mudosa.musinsa.payment.domain.model.Payment;
import com.mudosa.musinsa.payment.domain.model.PaymentEventType;
import com.mudosa.musinsa.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentBrandAmountService paymentBrandAmountService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected PaymentCreationResult createPaymentTransaction(PaymentCreateDto request, Long userId) {
        // 1. 주문 조회 (completeOrder 전에 먼저 조회)
        Order order = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 중복 결제 방지: 이미 해당 주문에 결제가 존재하는지 확인
        Payment existingPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingPayment != null) {
            log.info("[Payment] 이미 존재하는 결제 반환 - PaymentId: {}, OrderId: {}",
                    existingPayment.getId(), order.getId());
            return PaymentCreationResult.builder()
                    .paymentId(existingPayment.getId())
                    .orderId(order.getId())
                    .userId(userId)
                    .build();
        }

        // 3. 주문 완료(재고 차감, 주문 상태 변경)
        Long orderId = orderService.completeOrder(request.getOrderNo());

        // 4. 결제 생성
        Payment payment = Payment.create(
                orderId,
                request.getTotalAmount(),
                request.getPgProvider(),
                userId
        );

        paymentRepository.save(payment);

        return PaymentCreationResult.builder()
                .paymentId(payment.getId())
                .orderId(orderId)
                .userId(userId)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approvePayment(Long paymentId, Long userId, PaymentResponseDto paymentResponseDto, Long orderId) {
        //장바구니 삭제
        orderService.deleteCartItems(orderId, userId);

        //결제 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        String pgTransactionId = paymentResponseDto.getPaymentKey();

        //결제 상태 변경
        payment.approve(pgTransactionId, userId, paymentResponseDto.getApprovedAt(), paymentResponseDto.getMethod());

        paymentRepository.save(payment);

        // 브랜드별 금액 저장 (정산 배치 성능 최적화)
        try {
            if (!paymentBrandAmountService.existsForPayment(paymentId)) {
                paymentBrandAmountService.saveForPayment(paymentId, orderId);
                log.info("[Payment] 브랜드별 금액 저장 완료 - paymentId: {}", paymentId);
            } else {
                log.info("[Payment] 브랜드별 금액 이미 존재 - paymentId: {}", paymentId);
            }
        } catch (Exception e) {
            log.error("[Payment] 브랜드별 금액 저장 실패 (정산 배치에 영향 있음) - paymentId: {}", paymentId, e);
            // 결제 승인은 성공했으므로 예외를 던지지 않음
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failPayment(Long paymentId, String errorMessage, Long userId, Long orderId) {
        //주문 및 재고 롤백
        orderService.rollbackOrder(orderId);

        //결제 상태 변경
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.fail(errorMessage, userId);

        paymentRepository.save(payment);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void manualPaymentCheck(Long paymentId, Long userId){
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        payment.addLog(PaymentEventType.REQUIRES_MANUAL_CHECK,
                "PG 승인 후 예상치 못한 오류 발생", userId);
        paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelPayment(String paymentTransactionId, String cancelReason, Long userId, LocalDateTime cancelledAt) {
        log.info(paymentTransactionId);

        //결제 조회
        Payment payment = paymentRepository.findByPgTransactionId(paymentTransactionId);

        //결제 상태 변경
        payment.cancel(cancelReason, userId, cancelledAt);
        paymentRepository.save(payment);

        //주문 관련 원복
        orderService.cancelOrder(payment.getOrderId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failCancel(String paymentKey, String message, Long userId) {
        //결제 상태 변경
        Payment payment = paymentRepository.findByPgTransactionId(paymentKey);
        payment.cancelFail(message, userId);

        //주문 및 재고 롤백
        orderService.rollbackOrderCancel(payment.getOrderId());
    }
}

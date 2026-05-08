package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.entity.ChargeHistory;
import com.example.kcomproject.domain.point.entity.ChargeStatus;
import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import com.example.kcomproject.domain.point.repository.ChargeHistoryRepository;
import com.example.kcomproject.domain.point.repository.PointHistoryRepository;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.user.repository.UserRepository;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import com.example.kcomproject.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointTransactionService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ChargeHistoryRepository chargeHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChargeHistory createPendingHistory(Long userId, Long amount, String idempotencyKey) {
        ChargeHistory history = ChargeHistory.builder()
                .userId(userId)
                .amount(amount)
                .status(ChargeStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        return chargeHistoryRepository.save(history);
    }

    @Transactional
    public User chargeAndCompleteHistory(Long userId, Long amount, Long historyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        ChargeHistory chargeHistory = chargeHistoryRepository.findById(historyId)
                .orElseThrow(() -> new PointException(ErrorCode.INTERNAL_SERVER_ERROR));

        user.charge(amount);
        User savedUser = userRepository.save(user);

        // FIFO 관리를 위한 필드 추가 (5년 만료)
        PointHistory history = PointHistory.builder()
                .userId(userId)
                .type(PointTransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(savedUser.getPointBalance())
                .expiredAt(LocalDateTime.now().plusYears(5))
                .remainAmount(amount)
                .build();
        pointHistoryRepository.save(history);

        chargeHistory.updateStatus(ChargeStatus.SUCCESS);
        chargeHistoryRepository.save(chargeHistory);

        return savedUser;
    }

    @Transactional
    public void updateHistoryStatus(Long historyId, ChargeStatus status) {
        ChargeHistory history = chargeHistoryRepository.findById(historyId)
                .orElseThrow(() -> new PointException(ErrorCode.INTERNAL_SERVER_ERROR));
        history.updateStatus(status);
        chargeHistoryRepository.save(history);
    }

    @Transactional
    public User charge(Long userId, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        user.charge(amount);
        User savedUser = userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .userId(userId)
                .type(PointTransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(savedUser.getPointBalance())
                .expiredAt(LocalDateTime.now().plusYears(5))
                .remainAmount(amount)
                .build();
        pointHistoryRepository.save(history);

        return savedUser;
    }

    @Transactional
    public void refund(Long userId, Long amount, String idempotencyKey) {
        if (chargeHistoryRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        ChargeHistory history = ChargeHistory.builder()
                .userId(userId)
                .amount(amount)
                .status(ChargeStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .build();
        chargeHistoryRepository.save(history);

        charge(userId, amount);
    }

    @Transactional
    public User use(Long userId, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        user.use(amount);
        User savedUser = userRepository.save(user);

        // FIFO 차감 로직
        long toDeduct = amount;
        List<PointHistory> chargeHistories = pointHistoryRepository
                .findByUserIdAndTypeAndRemainAmountGreaterThanAndExpiredAtAfterOrderByCreatedAtAsc(
                        userId, PointTransactionType.CHARGE, 0L, LocalDateTime.now());

        for (PointHistory chargeHistory : chargeHistories) {
            long deduction = Math.min(toDeduct, chargeHistory.getRemainAmount());
            chargeHistory.decreaseRemain(deduction);
            pointHistoryRepository.save(chargeHistory);
            toDeduct -= deduction;
            if (toDeduct == 0) break;
        }

        PointHistory history = PointHistory.builder()
                .userId(userId)
                .type(PointTransactionType.USE)
                .amount(amount)
                .balanceAfter(savedUser.getPointBalance())
                .build();
        pointHistoryRepository.save(history);

        return savedUser;
    }
}

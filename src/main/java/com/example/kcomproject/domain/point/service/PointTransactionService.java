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

        saveHistory(userId, PointTransactionType.CHARGE, amount, savedUser.getPointBalance());

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

        saveHistory(userId, PointTransactionType.CHARGE, amount, savedUser.getPointBalance());

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

        saveHistory(userId, PointTransactionType.USE, amount, savedUser.getPointBalance());

        return savedUser;
    }

    private void saveHistory(Long userId, PointTransactionType type, Long amount, Long balanceAfter) {
        PointHistory history = PointHistory.builder()
                .userId(userId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .build();
        pointHistoryRepository.save(history);
    }
}

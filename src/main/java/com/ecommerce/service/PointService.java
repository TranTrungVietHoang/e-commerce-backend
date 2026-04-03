package com.ecommerce.service;

import com.ecommerce.dto.response.PointHistoryResponse;
import com.ecommerce.dto.response.PointsResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointsRepository pointsRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PointsResponse getPointsInfo(Long userId) {
        Points points = pointsRepository.findByUserId(userId)
                .orElseGet(() -> createInitialPoints(userId));

        User user = points.getUser();
        String membershipName = user.getMembershipLevel() != null 
                ? user.getMembershipLevel().getName() : "Thành viên Mới";

        Integer totalPoints = points.getTotalPoints();
        Integer pointsToNextLevel = calculatePointsToNextLevel(totalPoints);

        return PointsResponse.builder()
                .userId(userId)
                .totalPoints(totalPoints)
                .availablePoints(points.getAvailablePoints())
                .currentMembershipLevel(membershipName)
                .pointsToNextLevel(pointsToNextLevel)
                .build();
    }

    @Transactional
    public void addPoints(Long userId, Long orderId, Integer pointsEarned, String description) {
        if (pointsEarned <= 0) return;

        Points points = pointsRepository.findByUserId(userId)
                .orElseGet(() -> createInitialPoints(userId));

        points.setTotalPoints(points.getTotalPoints() + pointsEarned);
        points.setAvailablePoints(points.getAvailablePoints() + pointsEarned);
        pointsRepository.save(points);

        Order order = orderId != null ? orderRepository.findById(orderId).orElse(null) : null;

        PointHistory history = PointHistory.builder()
                .user(points.getUser())
                .order(order)
                .points(pointsEarned)
                .type("EARN")
                .description(description)
                .build();
        pointHistoryRepository.save(history);

        checkAndUpgradeMembership(points.getUser(), points.getTotalPoints());
    }

    @Transactional
    public void usePoints(Long userId, Long orderId, Integer pointsUsed) {
        if (pointsUsed <= 0) return;

        Points points = pointsRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Tài khoản điểm chưa được khởi tạo"));

        if (points.getAvailablePoints() < pointsUsed) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không đủ điểm khả dụng để sử dụng");
        }

        points.setAvailablePoints(points.getAvailablePoints() - pointsUsed);
        pointsRepository.save(points);

        Order order = orderId != null ? orderRepository.findById(orderId).orElse(null) : null;

        PointHistory history = PointHistory.builder()
                .user(points.getUser())
                .order(order)
                .points(pointsUsed)
                .type("REDEEM")
                .description("Sử dụng điểm cho đơn hàng #" + orderId)
                .build();
        pointHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryResponse> getPointHistory(Long userId, Pageable pageable) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(history -> PointHistoryResponse.builder()
                        .id(history.getId())
                        .userId(history.getUser().getId())
                        .orderId(history.getOrder() != null ? history.getOrder().getId() : null)
                        .points(history.getPoints())
                        .type(history.getType())
                        .description(history.getDescription())
                        .createdAt(history.getCreatedAt())
                        .build());
    }

    private void checkAndUpgradeMembership(User user, Integer currentTotalPoints) {
        Optional<MembershipLevel> matchedLevel = membershipLevelRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(currentTotalPoints);

        matchedLevel.ifPresent(level -> {
            if (user.getMembershipLevel() == null || !user.getMembershipLevel().getId().equals(level.getId())) {
                user.setMembershipLevel(level);
                userRepository.save(user);
            }
        });
    }

    private Integer calculatePointsToNextLevel(Integer currentTotalPoints) {
        List<MembershipLevel> levels = membershipLevelRepository.findAllByOrderByMinPointsAsc();
        for (MembershipLevel level : levels) {
            if (level.getMinPoints() > currentTotalPoints) {
                return level.getMinPoints() - currentTotalPoints;
            }
        }
        return 0; // Đã đạt mức cao nhất
    }

    private Points createInitialPoints(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Points points = Points.builder().user(user).totalPoints(0).availablePoints(0).build();
        return pointsRepository.save(points);
    }
}

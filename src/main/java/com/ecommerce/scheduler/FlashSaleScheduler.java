package com.ecommerce.scheduler;

import com.ecommerce.entity.FlashSale;
import com.ecommerce.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlashSaleScheduler {

    private final FlashSaleRepository flashSaleRepository;

    /**
     * Chạy mỗi 1 phút để cập nhật trạng thái của các đợt Flash Sale.
     * Tự động chuyển PENDING -> ACTIVE và ACTIVE -> FINISHED theo thời gian.
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    @Transactional
    public void updateFlashSaleStatuses() {
        LocalDateTime now = LocalDateTime.now();
        log.info("--- Flash Sale Status Scheduler Running at {} ---", now);

        // 1. Chuyển PENDING sang ACTIVE nếu đã đến giờ bắt đầu
        List<FlashSale> toActive = flashSaleRepository.findAll().stream()
                .filter(fs -> "PENDING".equals(fs.getStatus()) && !fs.getStartTime().isAfter(now))
                .toList();
        
        if (!toActive.isEmpty()) {
            toActive.forEach(fs -> {
                fs.setStatus("ACTIVE");
                log.info(">>> Flash Sale '{}' [ID: {}] is now ACTIVE", fs.getName(), fs.getId());
            });
            flashSaleRepository.saveAll(toActive);
        }

        // 2. Chuyển ACTIVE sang FINISHED nếu đã qua giờ kết thúc
        List<FlashSale> toFinished = flashSaleRepository.findAll().stream()
                .filter(fs -> "ACTIVE".equals(fs.getStatus()) && fs.getEndTime().isBefore(now))
                .toList();

        if (!toFinished.isEmpty()) {
            toFinished.forEach(fs -> {
                fs.setStatus("FINISHED");
                log.info("<<< Flash Sale '{}' [ID: {}] is now FINISHED", fs.getName(), fs.getId());
            });
            flashSaleRepository.saveAll(toFinished);
        }
    }
}

package com.ecommerce.service;

import com.ecommerce.entity.FlashSale;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;

    @Transactional
    public FlashSale createFlashSale(String name, LocalDateTime start, LocalDateTime end) {
        if (start.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Thoi gian bat dau phai o tuong lai");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }

        FlashSale flashSale = FlashSale.builder()
                .name(name)
                .startTime(start)
                .endTime(end)
                .status("PENDING")
                .build();

        return flashSaleRepository.save(flashSale);
    }

    @Transactional(readOnly = true)
    public List<FlashSale> getAllFlashSales() {
        return flashSaleRepository.findAllByOrderByStartTimeDesc();
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        FlashSale fs = flashSaleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Khong tim thay Flash Sale"));
        
        fs.setStatus(status.toUpperCase());
        flashSaleRepository.save(fs);
    }

    @Transactional
    public void deleteFlashSale(Long id) {
        FlashSale fs = flashSaleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Khong tim thay Flash Sale"));
        
        if ("ACTIVE".equals(fs.getStatus())) {
            throw new BusinessException("Khong the xoa Flash Sale dang dien ra");
        }
        
        flashSaleRepository.delete(fs);
    }
}

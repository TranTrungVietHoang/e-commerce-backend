package com.ecommerce.service.impl;

import com.ecommerce.dto.request.voucher.ApplyVoucherRequest;
import com.ecommerce.dto.request.voucher.CreateVoucherRequest;
import com.ecommerce.dto.response.voucher.VoucherApplyResponse;
import com.ecommerce.dto.response.voucher.VoucherResponse;
import com.ecommerce.entity.Shop;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Voucher;
import com.ecommerce.entity.VoucherUsage;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.VoucherRepository;
import com.ecommerce.repository.VoucherUsageRepository;
import com.ecommerce.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAvailableVouchers(Long shopId, BigDecimal orderValue) {
        validateOrderValue(orderValue);
        return voucherRepository.findByShopIdOrShopIsNull(shopId).stream()
                .filter(voucher -> matchesShop(voucher, shopId))
                .filter(voucher -> isVoucherAvailable(voucher, orderValue))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherApplyResponse applyVoucher(Long userId, ApplyVoucherRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Voucher voucher = getVoucherByCode(request.getCode());
        validateVoucherScope(voucher, request.getShopId());
        return calculateVoucher(user, voucher, sanitizeOrderValue(request.getOrderValue()), false);
    }

    @Override
    @Transactional
    public VoucherApplyResponse consumeVoucher(Long userId, ApplyVoucherRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Voucher voucher = getVoucherByCode(request.getCode());
        validateVoucherScope(voucher, request.getShopId());
        BigDecimal orderValue = sanitizeOrderValue(request.getOrderValue());
        VoucherApplyResponse response = calculateVoucher(user, voucher, orderValue, true);

        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setUser(user);
        usage.setOrderValue(orderValue);
        usage.setDiscountAmount(response.getDiscountAmount());
        voucherUsageRepository.save(usage);

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getSellerVouchers(Long shopId) {
        return voucherRepository.findByShopIdOrShopIsNull(shopId).stream()
                .filter(voucher -> voucher.getShop() != null && voucher.getShop().getId().equals(shopId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(Long shopId, CreateVoucherRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));
        
        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BusinessException("Gian hàng c?a b?n dang b? khoá. Không th? t?o voucher.");
        }
        
        validateVoucherRequest(request, null);
        Voucher voucher = new Voucher();
        fillVoucher(voucher, shopId, request);
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Long voucherId, Long shopId, CreateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", voucherId));
        
        if (voucher.getShop() != null && voucher.getShop().getStatus() != ShopStatus.APPROVED) {
            throw new BusinessException("Gian hàng c?a b?n dang b? khoá ho?c chua duy?t. Không th? c?p nh?t voucher.");
        }

        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shopId)) {
            throw new BusinessException("Khong co quyen sua voucher nay");
        }
        validateVoucherRequest(request, voucherId);
        fillVoucher(voucher, shopId, request);
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public void deleteVoucher(Long voucherId, Long shopId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", voucherId));
        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shopId)) {
            throw new BusinessException("Khong co quyen xoa voucher nay");
        }
        voucherRepository.delete(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAllVouchers() {
        log.info("Admin l?y t?t c? voucher h? th?ng");
        return voucherRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Voucher getVoucherByCode(String code) {
        return voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Voucher khong ton tai"));
    }

    private VoucherApplyResponse calculateVoucher(User user, Voucher voucher, BigDecimal orderValue, boolean checkUsage) {
        LocalDateTime now = LocalDateTime.now();
        
        log.info("Checking voucher {}: now={}, startAt={}, endAt={}, orderValue={}", 
            voucher.getCode(), now, voucher.getStartAt(), voucher.getEndAt(), orderValue);

        // Kiểm tra Active
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new BusinessException("Voucher này đã bị khóa hoặc ngừng kích hoạt");
        }
        
        // Kiểm tra thời hạn (với 5 phút ân hạn cho thời gian bắt đầu)
        if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt().minusMinutes(5))) {
            throw new BusinessException("Voucher chưa đến thời điểm áp dụng (Bắt đầu lúc: " + voucher.getStartAt() + ")");
        }
        if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
            throw new BusinessException("Voucher đã hết hạn sử dụng (Kết thúc lúc: " + voucher.getEndAt() + ")");
        }
        
        // Kiểm tra lượt dùng
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BusinessException("Voucher này đã hết lượt sử dụng");
        }
        
        // Kiểm tra giá trị đơn tối thiểu
        if (voucher.getMinOrderValue() != null && orderValue.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderValue() + "đ");
        }

        if (checkUsage && voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
            throw new BusinessException("Voucher này đã được bạn sử dụng");
        }

        BigDecimal discountAmount = "PERCENT".equalsIgnoreCase(voucher.getDiscountType())
                ? orderValue.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();

        if (voucher.getMaxDiscountValue() != null && discountAmount.compareTo(voucher.getMaxDiscountValue()) > 0) {
            discountAmount = voucher.getMaxDiscountValue();
        }
        if (discountAmount.compareTo(orderValue) > 0) {
            discountAmount = orderValue;
        }

        VoucherApplyResponse response = new VoucherApplyResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setDiscountType(voucher.getDiscountType());
        response.setDiscountValue(voucher.getDiscountValue());
        response.setOrderValue(orderValue);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(orderValue.subtract(discountAmount));
        return response;
    }

    private boolean isVoucherAvailable(Voucher voucher, BigDecimal orderValue) {
        LocalDateTime now = LocalDateTime.now();
        boolean activeWindow = Boolean.TRUE.equals(voucher.getActive())
                && voucher.getStartAt() != null
                && voucher.getEndAt() != null
                && !now.isBefore(voucher.getStartAt())
                && now.isBefore(voucher.getEndAt());
        boolean usageAvailable = voucher.getUsageLimit() == null || voucher.getUsedCount() < voucher.getUsageLimit();
        boolean minOrderValid = voucher.getMinOrderValue() == null || orderValue.compareTo(voucher.getMinOrderValue()) >= 0;
        return activeWindow && usageAvailable && minOrderValid;
    }

    private boolean matchesShop(Voucher voucher, Long shopId) {
        return voucher.getShop() == null || (shopId != null && voucher.getShop().getId().equals(shopId));
    }

    private void validateVoucherScope(Voucher voucher, Long shopId) {
        if (voucher.getShop() != null && (shopId == null || !voucher.getShop().getId().equals(shopId))) {
            throw new BusinessException("Voucher khong ap dung cho shop hien tai");
        }
    }

    private BigDecimal sanitizeOrderValue(BigDecimal orderValue) {
        validateOrderValue(orderValue);
        return orderValue;
    }

    private void validateOrderValue(BigDecimal orderValue) {
        if (orderValue == null || orderValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Gia tri don hang phai lon hon 0");
        }
    }

    private void validateVoucherRequest(CreateVoucherRequest request, Long voucherId) {
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new BusinessException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
        String discountType = request.getDiscountType().trim().toUpperCase(Locale.ROOT);
        if (!discountType.equals("PERCENT") && !discountType.equals("FIXED")) {
            throw new BusinessException("discountType chi ho tro PERCENT hoac FIXED");
        }
        if (discountType.equals("PERCENT") && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Voucher phan tram khong duoc vuot qua 100");
        }
        if (request.getMinOrderValue() != null && request.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Don toi thieu khong hop le");
        }
        if (request.getMaxDiscountValue() != null && request.getMaxDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giam toi da khong hop le");
        }
        voucherRepository.findByCodeIgnoreCase(request.getCode()).ifPresent(existing -> {
            if (voucherId == null || !existing.getId().equals(voucherId)) {
                throw new BusinessException("Ma voucher da ton tai");
            }
        });
    }

    private void fillVoucher(Voucher voucher, Long shopId, CreateVoucherRequest request) {
        Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));
        voucher.setShop(shop);
        voucher.setCode(request.getCode().trim().toUpperCase(Locale.ROOT));
        voucher.setName(request.getName().trim());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType().trim().toUpperCase(Locale.ROOT));
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setMaxDiscountValue(request.getMaxDiscountValue());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setStartAt(request.getStartAt());
        voucher.setEndAt(request.getEndAt());
        voucher.setExpiresAt(request.getEndAt());
        voucher.setActive(request.getActive() == null || request.getActive());
    }

    private VoucherResponse toResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setName(voucher.getName());
        response.setDescription(voucher.getDescription());
        response.setDiscountType(voucher.getDiscountType());
        response.setDiscountValue(voucher.getDiscountValue());
        response.setMinOrderValue(voucher.getMinOrderValue());
        response.setMaxDiscountValue(voucher.getMaxDiscountValue());
        response.setUsageLimit(voucher.getUsageLimit());
        response.setUsedCount(voucher.getUsedCount());
        response.setActive(voucher.getActive());
        response.setStartAt(voucher.getStartAt());
        response.setEndAt(voucher.getEndAt());
        response.setShopId(voucher.getShop() != null ? voucher.getShop().getId() : null);
        return response;
    }
}

package com.ecommerce.service;

import com.ecommerce.entity.FlashSale;
import com.ecommerce.entity.FlashSaleProduct;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.repository.FlashSaleProductRepository;
import com.ecommerce.repository.FlashSaleRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashSaleProductService {

    private final FlashSaleProductRepository flashSaleProductRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;

    @Transactional
    public FlashSaleProduct registerProduct(Long flashSaleId, Long productId, BigDecimal flashSalePrice, Integer slots, Long shopId) {
        FlashSale fs = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new BusinessException("Khong tim thay Flash Sale"));
        
        if (!"PENDING".equals(fs.getStatus())) {
            throw new BusinessException("Chi co the dang ky vao chien dich dang cho (PENDING)");
        }

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Khong tim thay san pham"));
        
        if (!p.getShop().getId().equals(shopId)) {
            throw new BusinessException("San pham khong thuoc ve shop cua ban");
        }

        if (flashSalePrice.compareTo(p.getBasePrice()) >= 0) {
            throw new BusinessException("Gia Flash Sale phai nho hon gia goc");
        }

        FlashSaleProduct fsp = FlashSaleProduct.builder()
                .flashSale(fs)
                .product(p)
                .flashSalePrice(flashSalePrice)
                .slots(slots)
                .soldCount(0)
                .build();

        return flashSaleProductRepository.save(fsp);
    }

    @Transactional(readOnly = true)
    public List<FlashSaleProduct> getProductsByFlashSale(Long flashSaleId) {
        return flashSaleProductRepository.findByFlashSaleId(flashSaleId);
    }

    @Transactional(readOnly = true)
    public List<FlashSaleProduct> getProductsByShop(Long shopId) {
        return flashSaleProductRepository.findByProduct_Shop_Id(shopId);
    }

    @Transactional
    public void unregisterProduct(Long id, Long shopId) {
        FlashSaleProduct fsp = flashSaleProductRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Khong tim thay ban ghi"));
        
        if (!fsp.getProduct().getShop().getId().equals(shopId)) {
            throw new BusinessException("Ban khong co quyen xoa san pham nay");
        }

        if (!"PENDING".equals(fsp.getFlashSale().getStatus())) {
            throw new BusinessException("Khong the huy dang ky khi chien dich da bat dau hoac da ket thuc");
        }

        flashSaleProductRepository.delete(fsp);
    }
}

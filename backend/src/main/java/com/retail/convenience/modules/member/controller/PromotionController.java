package com.retail.convenience.modules.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.modules.member.entity.Promotion;
import com.retail.convenience.modules.member.entity.PromotionItem;
import com.retail.convenience.modules.member.mapper.PromotionItemMapper;
import com.retail.convenience.modules.member.mapper.PromotionMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/promotions")
public class PromotionController {

    private final PromotionMapper promotionMapper;
    private final PromotionItemMapper itemMapper;

    public PromotionController(PromotionMapper promotionMapper, PromotionItemMapper itemMapper) {
        this.promotionMapper = promotionMapper;
        this.itemMapper = itemMapper;
    }

    @GetMapping
    @RequirePerm("member:view")
    public ApiResult<PageResult<Promotion>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<Promotion> result = promotionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Promotion>()
                        .eq(status != null && !status.isBlank(), Promotion::getStatus, status)
                        .orderByDesc(Promotion::getStartAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("member:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Promotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            throw BizException.notFound("活动不存在");
        }
        List<PromotionItem> items = itemMapper.selectList(new LambdaQueryWrapper<PromotionItem>()
                .eq(PromotionItem::getPromotionId, id));
        return ApiResult.ok(Map.of("promotion", promotion, "items", items));
    }

    @PostMapping
    @RequirePerm("member:create")
    @Transactional
    public ApiResult<Promotion> create(@RequestBody SaveRequest request) {
        validate(request.getPromotion());
        Promotion promotion = request.getPromotion();
        promotion.setId(null);
        promotionMapper.insert(promotion);
        saveItems(promotion.getId(), request.getItems());
        return ApiResult.ok(promotion);
    }

    @PutMapping("/{id}")
    @RequirePerm("member:update")
    @Transactional
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody SaveRequest request) {
        validate(request.getPromotion());
        Promotion promotion = request.getPromotion();
        promotion.setId(id);
        promotionMapper.updateById(promotion);
        if (request.getItems() != null) {
            itemMapper.delete(new LambdaQueryWrapper<PromotionItem>()
                    .eq(PromotionItem::getPromotionId, id));
            saveItems(id, request.getItems());
        }
        return ApiResult.ok();
    }

    private void saveItems(Long promotionId, List<PromotionItem> items) {
        if (items == null) {
            return;
        }
        for (PromotionItem item : items) {
            item.setId(null);
            item.setPromotionId(promotionId);
            if (item.getOriginalPrice() == null) {
                item.setOriginalPrice(java.math.BigDecimal.ZERO);
            }
            if (item.getPromoPrice() == null) {
                item.setPromoPrice(item.getOriginalPrice());
            }
            itemMapper.insert(item);
        }
    }

    private void validate(Promotion promotion) {
        if (promotion == null || promotion.getName() == null || promotion.getName().isBlank()) {
            throw new BizException("活动名称不能为空");
        }
        if (promotion.getStartAt() == null || promotion.getEndAt() == null
                || promotion.getEndAt().isBefore(promotion.getStartAt())) {
            throw new BizException("活动时间不合法");
        }
    }

    @lombok.Data
    public static class SaveRequest {
        private Promotion promotion;
        private List<PromotionItem> items;
    }
}

package com.retail.convenience.modules.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.modules.inventory.entity.Stock;
import com.retail.convenience.modules.inventory.entity.StockBatch;
import com.retail.convenience.modules.inventory.entity.StockFlow;
import com.retail.convenience.modules.inventory.mapper.StockBatchMapper;
import com.retail.convenience.modules.inventory.mapper.StockFlowMapper;
import com.retail.convenience.modules.inventory.mapper.StockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class InventoryService {

    private final StockMapper stockMapper;
    private final StockBatchMapper batchMapper;
    private final StockFlowMapper flowMapper;

    public InventoryService(StockMapper stockMapper, StockBatchMapper batchMapper, StockFlowMapper flowMapper) {
        this.stockMapper = stockMapper;
        this.batchMapper = batchMapper;
        this.flowMapper = flowMapper;
    }

    public Stock getStock(String ownerType, Long ownerId, Long skuId) {
        return stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getOwnerType, ownerType)
                .eq(Stock::getOwnerId, ownerId)
                .eq(Stock::getSkuId, skuId)
                .last("limit 1"));
    }

    @Transactional
    public void reserve(Long storeId, Long skuId, int qty) {
        Stock stock = lockStock(storeId, skuId);
        int available = stock.getQty() - (stock.getLockedQty() == null ? 0 : stock.getLockedQty());
        if (available < qty) {
            throw new BizException("商品库存不足：可用 " + available + "，需要 " + qty);
        }
        stock.setLockedQty((stock.getLockedQty() == null ? 0 : stock.getLockedQty()) + qty);
        stockMapper.updateById(stock);
    }

    @Transactional
    public void release(Long storeId, Long skuId, int qty) {
        Stock stock = getStock("STORE", storeId, skuId);
        if (stock != null) {
            stock.setLockedQty(Math.max(0, (stock.getLockedQty() == null ? 0 : stock.getLockedQty()) - qty));
            stockMapper.updateById(stock);
        }
    }

    @Transactional
    public void commitReserved(Long storeId, Long skuId, int qty, String refType, Long refId, String operator) {
        Stock stock = lockStock(storeId, skuId);
        int locked = stock.getLockedQty() == null ? 0 : stock.getLockedQty();
        stock.setLockedQty(Math.max(0, locked - qty));
        int before = stock.getQty();
        if (before < qty) {
            throw new BizException("库存不足，无法扣减");
        }
        stock.setQty(before - qty);
        stockMapper.updateById(stock);
        deductBatches("STORE", storeId, skuId, qty);
        flow("STORE", storeId, skuId, "SALE", -qty, before, stock.getQty(), refType, refId, "销售扣减", operator);
    }

    @Transactional
    public void deduct(String ownerType, Long ownerId, Long skuId, int qty, String bizType,
                       String refType, Long refId, String remark, String operator) {
        Stock stock = lockStock(ownerType, ownerId, skuId);
        int before = stock.getQty();
        if (before < qty) {
            throw new BizException("库存不足，当前库存 " + before);
        }
        stock.setQty(before - qty);
        stockMapper.updateById(stock);
        deductBatches(ownerType, ownerId, skuId, qty);
        flow(ownerType, ownerId, skuId, bizType, -qty, before, stock.getQty(), refType, refId, remark, operator);
    }

    @Transactional
    public void inbound(String ownerType, Long ownerId, Long skuId, int qty, String batchNo,
                        LocalDate productionDate, LocalDate expiryDate, String bizType,
                        String refType, Long refId, String remark, String operator) {
        Stock stock = getStock(ownerType, ownerId, skuId);
        int before = 0;
        if (stock == null) {
            stock = new Stock();
            stock.setOwnerType(ownerType);
            stock.setOwnerId(ownerId);
            stock.setSkuId(skuId);
            stock.setQty(0);
            stock.setLockedQty(0);
            stock.setSafetyStock(10);
            stockMapper.insert(stock);
        } else {
            stock = lockStock(ownerType, ownerId, skuId);
            before = stock.getQty();
        }
        stock.setQty(before + qty);
        stockMapper.updateById(stock);
        StockBatch batch = new StockBatch();
        batch.setOwnerType(ownerType);
        batch.setOwnerId(ownerId);
        batch.setSkuId(skuId);
        batch.setBatchNo(batchNo == null || batchNo.isBlank()
                ? "B" + LocalDate.now().toString().replace("-", "") + "-" + skuId : batchNo);
        batch.setProductionDate(productionDate);
        batch.setExpiryDate(expiryDate);
        batch.setQty(qty);
        batchMapper.insert(batch);
        flow(ownerType, ownerId, skuId, bizType, qty, before, stock.getQty(), refType, refId, remark, operator);
    }

    private void deductBatches(String ownerType, Long ownerId, Long skuId, int qty) {
        List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getOwnerType, ownerType)
                .eq(StockBatch::getOwnerId, ownerId)
                .eq(StockBatch::getSkuId, skuId)
                .gt(StockBatch::getQty, 0)
                .orderByAsc(StockBatch::getExpiryDate));
        int remain = qty;
        for (StockBatch batch : batches) {
            if (remain <= 0) {
                break;
            }
            int take = Math.min(remain, batch.getQty());
            batch.setQty(batch.getQty() - take);
            batchMapper.updateById(batch);
            remain -= take;
        }
    }

    @Transactional
    public void adjust(String ownerType, Long ownerId, Long skuId, int targetQty, String bizType,
                       String refType, Long refId, String remark, String operator) {
        Stock stock = getStock(ownerType, ownerId, skuId);
        if (stock == null) {
            if (targetQty == 0) {
                return;
            }
            stock = new Stock();
            stock.setOwnerType(ownerType);
            stock.setOwnerId(ownerId);
            stock.setSkuId(skuId);
            stock.setQty(0);
            stock.setLockedQty(0);
            stock.setSafetyStock(0);
            stockMapper.insert(stock);
        }
        int before = stock.getQty();
        int delta = targetQty - before;
        if (delta == 0) {
            return;
        }
        stock.setQty(targetQty);
        stockMapper.updateById(stock);
        if (delta < 0) {
            deductBatches(ownerType, ownerId, skuId, -delta);
        } else {
            StockBatch batch = new StockBatch();
            batch.setOwnerType(ownerType);
            batch.setOwnerId(ownerId);
            batch.setSkuId(skuId);
            batch.setBatchNo("ADJ" + System.currentTimeMillis());
            batch.setProductionDate(LocalDate.now());
            batch.setExpiryDate(LocalDate.now().plusYears(1));
            batch.setQty(delta);
            batchMapper.insert(batch);
        }
        flow(ownerType, ownerId, skuId, bizType, delta, before, targetQty, refType, refId, remark, operator);
    }

    public List<StockBatch> nearExpiry(Long storeId, int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        return batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getOwnerType, "STORE")
                .eq(StockBatch::getOwnerId, storeId)
                .gt(StockBatch::getQty, 0)
                .isNotNull(StockBatch::getExpiryDate)
                .le(StockBatch::getExpiryDate, limit)
                .orderByAsc(StockBatch::getExpiryDate));
    }

    public List<StockFlow> flows(String ownerType, Long ownerId, Long skuId, int limit) {
        return flowMapper.selectList(new LambdaQueryWrapper<StockFlow>()
                .eq(StockFlow::getOwnerType, ownerType)
                .eq(StockFlow::getOwnerId, ownerId)
                .eq(skuId != null, StockFlow::getSkuId, skuId)
                .orderByDesc(StockFlow::getCreatedAt)
                .last("limit " + Math.max(1, Math.min(limit, 500))));
    }

    private Stock lockStock(Long ownerId, Long skuId) {
        return lockStock("STORE", ownerId, skuId);
    }

    private Stock lockStock(String ownerType, Long ownerId, Long skuId) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getOwnerType, ownerType)
                .eq(Stock::getOwnerId, ownerId)
                .eq(Stock::getSkuId, skuId)
                .last("FOR UPDATE"));
        if (stock == null) {
            throw new BizException("库存记录不存在");
        }
        return stock;
    }

    private void flow(String ownerType, Long ownerId, Long skuId, String bizType, int change,
                      int before, int after, String refType, Long refId, String remark, String operator) {
        StockFlow flow = new StockFlow();
        flow.setOwnerType(ownerType);
        flow.setOwnerId(ownerId);
        flow.setSkuId(skuId);
        flow.setBizType(bizType);
        flow.setChangeQty(change);
        flow.setBeforeQty(before);
        flow.setAfterQty(after);
        flow.setRefType(refType);
        flow.setRefId(refId);
        flow.setRemark(remark);
        flow.setCreatedBy(operator);
        flowMapper.insert(flow);
    }
}

package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.common.BusinessException;
import com.semiconductor.mask.entity.StorageLocation;
import com.semiconductor.mask.mapper.StorageLocationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class StorageLocationService extends ServiceImpl<StorageLocationMapper, StorageLocation> {

    public IPage<StorageLocation> pageQuery(Integer pageNum, Integer pageSize, String locationCode, String locationName, String cleanLevel, Integer status) {
        Page<StorageLocation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        if (locationCode != null && !locationCode.isEmpty()) {
            wrapper.like(StorageLocation::getLocationCode, locationCode);
        }
        if (locationName != null && !locationName.isEmpty()) {
            wrapper.like(StorageLocation::getLocationName, locationName);
        }
        if (cleanLevel != null && !cleanLevel.isEmpty()) {
            wrapper.eq(StorageLocation::getCleanLevel, cleanLevel);
        }
        if (status != null) {
            wrapper.eq(StorageLocation::getStatus, status);
        }
        wrapper.orderByAsc(StorageLocation::getLocationCode);
        return this.page(page, wrapper);
    }

    public StorageLocation getByCode(String locationCode) {
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageLocation::getLocationCode, locationCode);
        return this.getOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addLocation(StorageLocation location) {
        if (getByCode(location.getLocationCode()) != null) {
            throw new BusinessException("库位编码已存在");
        }
        if (location.getStatus() == null) {
            location.setStatus(1);
        }
        if (location.getCurrentCount() == null) {
            location.setCurrentCount(0);
        }
        if (location.getCapacity() == null) {
            location.setCapacity(1);
        }
        return this.save(location);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateLocation(StorageLocation location) {
        StorageLocation old = this.getById(location.getId());
        if (old == null) {
            throw new BusinessException("库位不存在");
        }
        if (location.getCapacity() != null && location.getCapacity() < old.getCurrentCount()) {
            throw new BusinessException("容量不能小于当前存放数量");
        }
        return this.updateById(location);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLocation(Long id) {
        StorageLocation location = this.getById(id);
        if (location == null) {
            throw new BusinessException("库位不存在");
        }
        if (location.getCurrentCount() > 0) {
            throw new BusinessException("库位中还有光罩，不能删除");
        }
        return this.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementCount(Long locationId) {
        StorageLocation location = this.getById(locationId);
        if (location == null) {
            throw new BusinessException("库位不存在");
        }
        if (location.getCurrentCount() >= location.getCapacity()) {
            throw new BusinessException("库位容量已满");
        }
        location.setCurrentCount(location.getCurrentCount() + 1);
        this.updateById(location);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decrementCount(Long locationId) {
        StorageLocation location = this.getById(locationId);
        if (location == null) {
            throw new BusinessException("库位不存在");
        }
        if (location.getCurrentCount() <= 0) {
            throw new BusinessException("库位当前数量为0，无法减少");
        }
        location.setCurrentCount(location.getCurrentCount() - 1);
        this.updateById(location);
    }

    public List<StorageLocation> getAvailableLocations(String cleanLevel) {
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageLocation::getStatus, 1);
        wrapper.apply("current_count < capacity");
        if (cleanLevel != null && !cleanLevel.isEmpty()) {
            wrapper.eq(StorageLocation::getCleanLevel, cleanLevel);
        }
        wrapper.orderByAsc(StorageLocation::getLocationCode);
        return this.list(wrapper);
    }
}

package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.StorageLocation;
import com.semiconductor.mask.service.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/location")
public class StorageLocationController {

    @Autowired
    private StorageLocationService storageLocationService;

    @GetMapping("/page")
    public Result<IPage<StorageLocation>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) String cleanLevel,
            @RequestParam(required = false) Integer status) {
        return Result.success(storageLocationService.pageQuery(pageNum, pageSize, locationCode, locationName, cleanLevel, status));
    }

    @GetMapping("/{id}")
    public Result<StorageLocation> getById(@PathVariable Long id) {
        return Result.success(storageLocationService.getById(id));
    }

    @GetMapping("/code/{locationCode}")
    public Result<StorageLocation> getByCode(@PathVariable String locationCode) {
        return Result.success(storageLocationService.getByCode(locationCode));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody StorageLocation location) {
        return Result.success(storageLocationService.addLocation(location));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody StorageLocation location) {
        return Result.success(storageLocationService.updateLocation(location));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(storageLocationService.deleteLocation(id));
    }

    @GetMapping("/available")
    public Result<List<StorageLocation>> getAvailableLocations(@RequestParam(required = false) String cleanLevel) {
        return Result.success(storageLocationService.getAvailableLocations(cleanLevel));
    }
}

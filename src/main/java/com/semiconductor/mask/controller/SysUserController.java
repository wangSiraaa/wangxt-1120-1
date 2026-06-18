package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.SysUser;
import com.semiconductor.mask.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/page")
    public Result<IPage<SysUser>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userCode,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String roleType,
            @RequestParam(required = false) Integer status) {
        return Result.success(sysUserService.pageQuery(pageNum, pageSize, userCode, userName, roleType, status));
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @GetMapping("/code/{userCode}")
    public Result<SysUser> getByUserCode(@PathVariable String userCode) {
        return Result.success(sysUserService.getByUserCode(userCode));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody SysUser user) {
        return Result.success(sysUserService.addUser(user));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody SysUser user) {
        return Result.success(sysUserService.updateUser(user));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(sysUserService.deleteUser(id));
    }
}

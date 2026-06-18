package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.common.BusinessException;
import com.semiconductor.mask.entity.SysUser;
import com.semiconductor.mask.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    public IPage<SysUser> pageQuery(Integer pageNum, Integer pageSize, String userCode, String userName, String roleType, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (userCode != null && !userCode.isEmpty()) {
            wrapper.like(SysUser::getUserCode, userCode);
        }
        if (userName != null && !userName.isEmpty()) {
            wrapper.like(SysUser::getUserName, userName);
        }
        if (roleType != null && !roleType.isEmpty()) {
            wrapper.eq(SysUser::getRoleType, roleType);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByAsc(SysUser::getUserCode);
        return this.page(page, wrapper);
    }

    public SysUser getByUserCode(String userCode) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserCode, userCode);
        return this.getOne(wrapper);
    }

    public boolean addUser(SysUser user) {
        if (getByUserCode(user.getUserCode()) != null) {
            throw new BusinessException("用户编码已存在");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        return this.save(user);
    }

    public boolean updateUser(SysUser user) {
        SysUser old = this.getById(user.getId());
        if (old == null) {
            throw new BusinessException("用户不存在");
        }
        return this.updateById(user);
    }

    public boolean deleteUser(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return this.removeById(id);
    }
}

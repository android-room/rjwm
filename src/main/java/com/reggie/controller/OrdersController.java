package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.entity.Orders;
import com.reggie.service.OrdersService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 *订单
 */
@RestController
@Slf4j
@RequestMapping("/order")
public class OrdersController {

    @Resource
    private OrdersService ordersService;

    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        ordersService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime,String endTime){
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<Orders> ordersPage = new Page<>();
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
//        queryWrapper.like(number != null,Orders::getNumber,number);
        //进行分页查询
        ordersPage = ordersService.page(pageInfo, queryWrapper);

        return R.success(ordersPage);
    }
}

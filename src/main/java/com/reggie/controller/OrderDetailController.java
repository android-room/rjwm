package com.reggie.controller;

import com.reggie.service.OrderDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 订单明细
 */
@Slf4j
@RequestMapping("/orderDetail")
@RestController
public class OrderDetailController {
    @Resource
    private OrderDetailService orderDetailService;
}

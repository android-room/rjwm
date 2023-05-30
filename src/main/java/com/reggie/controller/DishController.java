package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Resource
    private DishService dishService;

    @Resource
    private DishFlavorService dishFlavorService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private CacheManager cacheManager;


    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    /**
     *分页
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //分页构造器
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //添加排序条件， 根据price进行排序
        queryWrapper.orderByDesc(Dish::getPrice);
        //进行分页查询
        dishService.page(pageInfo,queryWrapper);


        //对象拷贝
        BeanUtils.copyProperties(pageInfo,queryWrapper,"records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((itme) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(itme,dishDto);

            Long categoryId = itme.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_"+dishDto.getCategoryId()+"_"+dishDto.getStatus();
        redisTemplate.delete(key);


        return R.success("修改菜品成功");
    }


    /**
     * 根据条件查询对应的菜品
     * @param dish
     * @return
     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
//        //添加条件，查询状态为1（起售）
//        queryWrapper.eq(Dish::getStatus,1);
//
//        //排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;

        String key ="dish_"+dish.getCategoryId()+"_"+dish.getStatus();//dish_1245841544657_1
        //redis中获取缓存数据
        dishDtoList =(List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList!=null){
            //如果存在，直接返回，无需查询数据库
            return R.success(dishDtoList);
        }

        //查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售）
        queryWrapper.eq(Dish::getStatus,1);

        //排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        dishDtoList = list.stream().map((itme) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(itme,dishDto);

            Long categoryId = itme.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long dishId = itme.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //Sql:Select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());
        //如果不存在，需要查询数据库，将查询到的菜品缓存到数据库
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    @PostMapping("/status/{sta}")
    public R<String> status(@PathVariable int sta, Long[] ids){
        log.info("sta{}",ids);
        System.out.println(sta+"=="+ids);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        for (int i = 0; i <ids.length ; i++) {
            Dish dish = new Dish();
            dish.setId(ids[i]);
            dish = dishService.getById(dish.getId());
            queryWrapper.eq(Dish::getId,ids[i]);
            if (sta == 1){
                dish.setStatus(1);
                dishService.updateById(dish);
            }else if (sta == 0){
                dish.setStatus(0);
                dishService.updateById(dish);
            }
        }
        return R.success("操作成功");
    }

    @DeleteMapping()
    public R<String> Delete(Long[] ids){
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        for (int i = 0; i <ids.length ; i++) {
            Dish dish = new Dish();
            dish.setId(ids[i]);
            dish = dishService.getById(dish.getId());
            queryWrapper.eq(Dish::getId,ids[i]);
            dishService.removeById(dish);
        }
        return R.success("操作成功");
    }

}

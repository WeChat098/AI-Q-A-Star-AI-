package com.edu.yudada.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.edu.yudada.model.dto.app.AppQueryRequest;
import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.vo.AppVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 应用服务
 *

 */
public interface AppService extends IService<App> { // service中都是继承IService
    //IService<App> 是 MyBatis-Plus 提供的接口，它提供了基本的 CRUD（创建、读取、更新、删除）操作，包括常用的查询方法。通过继承 IService<App>，AppService 接口可以使用这些基本的数据库操作方法。
    /**
     * 校验数据 校验创建的app是否合法
     *
     * @param app
     * @param add 对创建的数据进行校验
     */
    void validApp(App app, boolean add);

    /**
     * 获取查询条件 根据生成的查询条件，用于查询符合条件的应用数据
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest);
    
    /**
     * 获取应用封装 将传递过来的app 对象转换成为前端能看到的appvo对象
     *
     * @param app
     * @param request
     * @return
     */
    AppVO getAppVO(App app, HttpServletRequest request);

    /**
     * 分页获取应用封装 使用分页进行查询数据
     *
     * @param appPage
     * @param request
     * @return
     */
    Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request);
}

package com.edu.yudada.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.yudada.annotation.AuthCheck;
import com.edu.yudada.common.BaseResponse;
import com.edu.yudada.common.DeleteRequest;
import com.edu.yudada.common.ErrorCode;
import com.edu.yudada.common.ResultUtils;
import com.edu.yudada.constant.UserConstant;
import com.edu.yudada.exception.BusinessException;
import com.edu.yudada.exception.ThrowUtils;
import com.edu.yudada.model.dto.userAnswer.UserAnswerAddRequest;
import com.edu.yudada.model.dto.userAnswer.UserAnswerEditRequest;
import com.edu.yudada.model.dto.userAnswer.UserAnswerQueryRequest;
import com.edu.yudada.model.dto.userAnswer.UserAnswerUpdateRequest;
import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.entity.User;
import com.edu.yudada.model.entity.UserAnswer;
import com.edu.yudada.model.enums.ReviewStatusEnum;
import com.edu.yudada.model.vo.UserAnswerVO;
import com.edu.yudada.scoring.ScoringStrategyExecutor;
import com.edu.yudada.service.AppService;
import com.edu.yudada.service.UserAnswerService;
import com.edu.yudada.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户答案接口
 *
 */
@RestController
@RequestMapping("/userAnswer")
@Slf4j
public class UserAnswerController {

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ScoringStrategyExecutor scoringStrategyExecutor;

    // region 增删改查

    /**
     * 创建用户答案
     *@RequestBody 注解在 Spring MVC 中用于将 HTTP 请求体中的数据转换为方法的参数对象，
     * 适用于处理客户端提交的 JSON 或 XML 数据，并且主要用于 POST、PUT 等请求方法。在设计和实现 RESTful API
     * 或者需要接收复杂对象数据时非常有用，但对于简单的参数传递或者 GET 请求来说，不需要使用 @RequestBody
     * @param userAnswerAddRequest
     * @param request
     * @return
     */

    @PostMapping("/add")
    public BaseResponse<Long> addUserAnswer(@RequestBody UserAnswerAddRequest userAnswerAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userAnswerAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 在此处将实体类和 DTO 进行转换
        UserAnswer userAnswer = new UserAnswer();
        BeanUtils.copyProperties(userAnswerAddRequest, userAnswer);// 将dto类转换成为实体类
        List<String> choices = userAnswerAddRequest.getChoices();// 获取用户输入的选项
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));//将实体类中的答案填入到用户的答案中
        userAnswer.setAppId(userAnswerAddRequest.getAppId());// 初始化分数为0
        // 数据校验
        userAnswerService.validUserAnswer(userAnswer, true);// 校验应用ID是否存在
        // 判断 app 是否存在
        Long appId = userAnswerAddRequest.getAppId();// 判断用户输入的app是否存在
        App app = appService.getById(appId);// 根据ID获取这个app
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        if (!ReviewStatusEnum.PASS.equals(ReviewStatusEnum.getEnumByValue(app.getReviewStatus()))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "应用未通过审核，无法答题");
        }// 如果当前应用的状态是pass，说明没有通过审核
        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        userAnswer.setUserId(loginUser.getId());
        // 写入数据库
        try{
            boolean result = userAnswerService.save(userAnswer);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        }catch (DuplicateKeyException e){
            //ignore error
        }

        // 返回新写入的数据 id
        long newUserAnswerId = userAnswer.getId(); // 这里是写入应用的Id
        // 调用评分模块
        try {
            UserAnswer userAnswerWithResult = scoringStrategyExecutor.doScore(choices, app);
            userAnswerWithResult.setId(newUserAnswerId);
            userAnswerService.updateById(userAnswerWithResult);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "评分错误");
        }
        return ResultUtils.success(newUserAnswerId);
    }

    /**
     * 删除用户答案
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserAnswer(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        UserAnswer oldUserAnswer = userAnswerService.getById(id);
        ThrowUtils.throwIf(oldUserAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldUserAnswer.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = userAnswerService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户答案（仅管理员可用）
     *
     * @param userAnswerUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserAnswer(@RequestBody UserAnswerUpdateRequest userAnswerUpdateRequest) {
        if (userAnswerUpdateRequest == null || userAnswerUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        UserAnswer userAnswer = new UserAnswer();
        BeanUtils.copyProperties(userAnswerUpdateRequest, userAnswer);
        List<String> choices = userAnswerUpdateRequest.getChoices();
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        // 数据校验
        userAnswerService.validUserAnswer(userAnswer, false);
        // 判断是否存在
        long id = userAnswerUpdateRequest.getId();
        UserAnswer oldUserAnswer = userAnswerService.getById(id);
        ThrowUtils.throwIf(oldUserAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = userAnswerService.updateById(userAnswer);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户答案（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserAnswerVO> getUserAnswerVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        UserAnswer userAnswer = userAnswerService.getById(id);
        ThrowUtils.throwIf(userAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(userAnswerService.getUserAnswerVO(userAnswer, request));
    }

    /**
     * 分页获取用户答案列表（仅管理员可用）
     *
     * @param userAnswerQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserAnswer>> listUserAnswerByPage(@RequestBody UserAnswerQueryRequest userAnswerQueryRequest) {
        long current = userAnswerQueryRequest.getCurrent();
        long size = userAnswerQueryRequest.getPageSize();
        // 查询数据库
        Page<UserAnswer> userAnswerPage = userAnswerService.page(new Page<>(current, size),
                userAnswerService.getQueryWrapper(userAnswerQueryRequest));
        return ResultUtils.success(userAnswerPage);
    }

    /**
     * 分页获取用户答案列表（封装类）
     *
     * @param userAnswerQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserAnswerVO>> listUserAnswerVOByPage(@RequestBody UserAnswerQueryRequest userAnswerQueryRequest,
                                                                   HttpServletRequest request) {
        long current = userAnswerQueryRequest.getCurrent();
        long size = userAnswerQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<UserAnswer> userAnswerPage = userAnswerService.page(new Page<>(current, size),
                userAnswerService.getQueryWrapper(userAnswerQueryRequest));
        // 获取封装类
        return ResultUtils.success(userAnswerService.getUserAnswerVOPage(userAnswerPage, request));
    }

    /**
     * 分页获取当前登录用户创建的用户答案列表
     *
     * @param userAnswerQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<UserAnswerVO>> listMyUserAnswerVOByPage(@RequestBody UserAnswerQueryRequest userAnswerQueryRequest,
                                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(userAnswerQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        userAnswerQueryRequest.setUserId(loginUser.getId());
        long current = userAnswerQueryRequest.getCurrent();
        long size = userAnswerQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<UserAnswer> userAnswerPage = userAnswerService.page(new Page<>(current, size),
                userAnswerService.getQueryWrapper(userAnswerQueryRequest));
        // 获取封装类
        return ResultUtils.success(userAnswerService.getUserAnswerVOPage(userAnswerPage, request));
    }

    /**
     * 编辑用户答案（给用户使用）
     *
     * @param userAnswerEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUserAnswer(@RequestBody UserAnswerEditRequest userAnswerEditRequest, HttpServletRequest request) {
        if (userAnswerEditRequest == null || userAnswerEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        UserAnswer userAnswer = new UserAnswer();
        BeanUtils.copyProperties(userAnswerEditRequest, userAnswer);
        List<String> choices = userAnswerEditRequest.getChoices();
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        // 数据校验
        userAnswerService.validUserAnswer(userAnswer, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = userAnswerEditRequest.getId();
        UserAnswer oldUserAnswer = userAnswerService.getById(id);
        ThrowUtils.throwIf(oldUserAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldUserAnswer.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = userAnswerService.updateById(userAnswer);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
    @GetMapping("generate/id")
    public BaseResponse<Long> generateUserId(){
        return ResultUtils.success(IdUtil.getSnowflakeNextId());
    }
}

package com.edu.yudada.model.dto.app;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新应用请求
 *

 */
@Data
public class AppUpdateRequest implements Serializable {

    // 只有管理员可以进行更新
    /**
     * 应用id
     */
    private Long id;

    /**
     * 应用名
     */
    private String appName;

    /**
     * 应用描述
     */
    private String appDesc;

    /**
     * 应用图标
     */
    private String appIcon;

    /**
     * 应用类型（0-得分类，1-测评类）
     */
    private Integer appType;

    /**
     * 评分策略（0-自定义，1-AI）
     */
    private Integer scoringStrategy;

    /**
     * 审核状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;// 这个可有可无 主要是为了判断当前这个应用的状态是否是已经审核

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    private static final long serialVersionUID = 1L; //
}
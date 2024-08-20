package com.edu.yudada.scoring;

import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略
 *

 */
public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices 用户输入的每一题对应的结果答案
     * @param app 对应哪一个app
     * @return 返回的结果是对应的结果应该是什么 其中的内容包含 答案String 分数int 或者说测评类应用输入哪一个分类
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;

}
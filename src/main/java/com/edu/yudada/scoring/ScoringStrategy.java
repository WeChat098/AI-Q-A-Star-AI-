package com.edu.yudada.scoring;

import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略
 *
 * @author <a href="https://github.com/liedu">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
    // 通过输入app对用户输入的答案进行评分，最后返回评分的结果对象
}
package com.edu.yudada.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.edu.yudada.model.dto.question.QuestionContentDTO;
import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.entity.Question;
import com.edu.yudada.model.entity.ScoringResult;
import com.edu.yudada.model.entity.UserAnswer;
import com.edu.yudada.model.vo.QuestionVO;
import com.edu.yudada.service.QuestionService;
import com.edu.yudada.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 自定义打分类应用评分策略
 *
 */
// 通过使用自定义注解可以直接在注解上获取元数据的信息 比如这里就获取app类型的信息 以及评分策略的信息
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0) // 这里使用了自定义注解
public class CustomScoreScoringStrategy implements ScoringStrategy { // 这里实现评分策略接口

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override // 重写doscore方法
    public UserAnswer doScore(List<String> choices, App app) throws Exception { // 重写doscore方法
        Long appId = app.getId();//得到应用的id
        // 1. 根据 id 查询到题目和题目结果信息（按分数降序排序）
        Question question = questionService.getOne( // 一个app对应一个题目 所以这里通过一个appid或者一个答案
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );// wrappers表示mybatis提供的一个查询构造器，这个查询构造器作用与Question.class类，后面的eq用于构造等值查询，
        // 后面的Question：：getAppID用于表示Question类中的getAppID方法，用于找到appID对应的Question
        List<ScoringResult> scoringResultList = scoringResultService.list( // 这里是mybatis提供的一个list方法
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange)
        );
        // 这是mybatis提供的一个方法用于构造查询条件，eq用于构造一个等值查询，后面的用于将左右的评分范围进行排序，将排序后的结果用于遍历

        // 2. 统计用户的总得分
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);// 将question转换为questionVO
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
        //这里的questionContentDto包含所有的题目内容，题目中也包含了所有的questionContent

        // 遍历题目列表 代表所有的题目
        for (QuestionContentDTO questionContentDTO : questionContent) {
            // 遍历用户的选择的答案列表
            for (String answer : choices) {
                // 遍历一个dto中的所有option
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    // 如果答案和选项的key匹配
                    if (option.getKey().equals(answer)) {
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }

        // 3. 遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果
        ScoringResult maxScoringResult = scoringResultList.get(0);
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoringResult = scoringResult;
                break;
            }
        }

        // 4. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}

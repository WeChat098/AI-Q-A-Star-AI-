package com.edu.yudada.scoring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.edu.yudada.manager.AiManager;
import com.edu.yudada.model.dto.question.QuestionAnswerDTO;
import com.edu.yudada.model.dto.question.QuestionContentDTO;
import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.entity.Question;
import com.edu.yudada.model.entity.UserAnswer;
import com.edu.yudada.model.vo.QuestionVO;
import com.edu.yudada.service.QuestionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 测评类应用评分策略
 * AI 对测评类的应用进行分析
 *
 * @author <a href="https://github.com/liedu">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonClient redissonClient;

    // 分布式锁的 key
    private static final String AI_ANSWER_LOCK = "AI_ANSWER_LOCK";

    /**
     * AI 评分结果本地缓存
     */
    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    // 缓存 5 分钟移除
                    .expireAfterAccess(5L, TimeUnit.MINUTES)
                    .build();
    /**
     * AI 评分系统消息
     */
    private static final String AI_TEST_SCORING_SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来对用户进行评价：\n" +
            "1. 要求：需要给出一个明确的评价结果，包括评价名称（尽量简短）和评价描述（尽量详细，大于 200 字）\n" +
            "2. 严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\"resultName\": \"评价名称\", \"resultDesc\": \"评价描述\"}\n" +
            "```\n" +
            "3. 返回格式必须为 JSON 对象";

    public UserAnswer doScore(List<String> choices, App app) throws Exception { // 计算分数
        Long appId = app.getId();
        String jsonStr = JSONUtil.toJsonStr(choices);// 将用户的答案转换成json格式的字符串
        String cacheKey = buildCacheKey(appId, jsonStr);// 用户生成一个缓存键，这个键值是由appid和用户的答案作为key的
        String answerJson = answerCacheMap.getIfPresent(cacheKey);// 去caffine本地缓存中查找是否有这个key的缓存结果
        // 如果有缓存，直接返回
        if (StrUtil.isNotBlank(answerJson)) {// 如果缓存不为空，直接返回
            // 构造返回值，填充答案对象的属性
            UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);// 将一个json格式的字符串，answer转换成对应的java对象
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            return userAnswer;
        }

        // 定义锁  同一时刻只能有一个线程使用这个方法
        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK + cacheKey);// 如果用户的应用和回答的答案是一致的，那么结果应该是相同的
        try {
            // 竞争锁
            boolean res = lock.tryLock(3, 15, TimeUnit.SECONDS);
            // 没抢到锁，强行返回
            if (!res) {
                return null;
            }
            // 抢到锁了，执行后续业务逻辑
            // 1. 根据 id 查询到题目
            Question question = questionService.getOne(
                    Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
            );
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

            // 2. 调用 AI 获取结果
            // 封装 Prompt
            String userMessage = getAiTestScoringUserMessage(app, questionContent, choices);
            // AI 生成
            String result = aiManager.doSyncStableRequest(AI_TEST_SCORING_SYSTEM_MESSAGE, userMessage);
            // 截取需要的 JSON 信息
            int start = result.indexOf("{");
            int end = result.lastIndexOf("}");
            String json = result.substring(start, end + 1);

            // 缓存结果
            answerCacheMap.put(cacheKey, json);

            // 3. 构造返回值，填充答案对象的属性
            UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            return userAnswer;
        } finally {
            if (lock != null && lock.isLocked()) {//如果锁为空，说明当前没有获得锁，如果当前并不是锁定的状态，就不用释放锁
                if (lock.isHeldByCurrentThread()) {// 判断当前线程是否持有该锁，只有在当前线程持有锁的情况下，才能释放锁
                    lock.unlock();
                }
            }
        }

    }

    /**
     * AI 评分用户消息封装
     *
     * @param app 应用
     * @param questionContentDTOList 题目内容 表示是里面由多道题目 一道题目中包含一个标题和选项列表
     * @param choices 用户选择的答案
     * @return
     */
    private String getAiTestScoringUserMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n"); //添加应用的名称和应用的描述
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();// 返回的是一个String对象 里面包含应用的名称和描述以及一个questioncontentdto对象，
        // 这个questioncontentDto中包含一些题目的信息
    }
    /**
     * 构建缓存 key
     *
     * @param appId
     * @param choices
     * @return
     */
    private String buildCacheKey(Long appId, String choices) {
        // 通过appid和用户选择的答案构建一个cachekey
        return DigestUtil.md5Hex(appId + ":" + choices);// 通过md5算法对appid和用户选择的答案进行加密
    }
}

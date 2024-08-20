package com.edu.yudada.model.dto.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建题目请求
 *

 */
@Data
public class QuestionAddRequest implements Serializable {

    /**
     * 题目内容（json格式）
     */
    private List<QuestionContentDTO> questionContent;
    // 每一道题目对应的都是一个List，这里List中对应了多个QuestionContent

    /**
     * 应用 id
     */
    private Long appId;// 想给哪个应用添加Questison

    private static final long serialVersionUID = 1L;
}
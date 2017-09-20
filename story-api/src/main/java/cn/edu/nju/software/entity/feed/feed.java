package cn.edu.nju.software.entity.feed;

import lombok.Data;

import java.util.Date;

/**
 * Created by xmc1993 on 2017/9/15.
 */
@Data
public class Feed {
    private Integer id;
    private Integer mid;//MessageID
    private Integer fid;//发送方
    private Integer tid;//接收方
    private String content;
    private MessageType type;
    private int valid;
    private int read = 0;//是否已读
    private Date createTime;
    private Date updateTime;
}

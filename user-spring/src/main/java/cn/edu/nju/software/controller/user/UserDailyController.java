package cn.edu.nju.software.controller.user;

import cn.edu.nju.software.controller.BaseController;
import cn.edu.nju.software.dto.MsgVo;
import cn.edu.nju.software.entity.Daily;
import cn.edu.nju.software.entity.ResponseData;
import cn.edu.nju.software.entity.User;
import cn.edu.nju.software.entity.Feed;
import cn.edu.nju.software.entity.feed.MessageType;
import cn.edu.nju.software.enums.Visibility;
import cn.edu.nju.software.feed.service.MessageFeedService;
import cn.edu.nju.software.service.DailyService;
import cn.edu.nju.software.service.FollowService;
import cn.edu.nju.software.service.StoryTagService;
import cn.edu.nju.software.service.user.AppUserService;
import cn.edu.nju.software.util.TokenConfig;
import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by xmc1993 on 2017/5/12.
 */

@Api(value = "/daily", description = "和日志有关的接口")
@Controller
@RequestMapping("/user/daily")
public class UserDailyController extends BaseController {
    @Autowired
    private DailyService dailyService;
    @Autowired
    StoryTagService storyTagService;
    @Autowired
    AppUserService appUserService;
    @Autowired
    FollowService followService;
    @Autowired
    MessageFeedService messageFeedService;

    @ApiOperation(value = "获取某个用户的日志列表", notes = "需要登录")
    @RequestMapping(value = "/getDailyListByUserIdByPage", method = {RequestMethod.GET})
    @ResponseBody
    public ResponseData<List<Daily>> getDailyListByUserId(
            @ApiParam("用户ID") @RequestParam("userId") int userId,
            @ApiParam("page") @RequestParam int page,
            @ApiParam("pageSize") @RequestParam int pageSize,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<List<Daily>> responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }
        //获取访问者与被访者的关系
        int relation = followService.getRelation(userId, user.getId());
        List<Daily> dailyList = dailyService.getDailyListByUserId(userId, relation, page, pageSize);
        responseData.jsonFill(1, null, dailyList);
        //TODO 正确的数量
        responseData.setCount(dailyService.getDailyCountByUserId(userId, relation));
        return responseData;
    }

    @ApiOperation(value = "根据ID获得一个日志", notes = "需要登录")
    @RequestMapping(value = "/getDailyById", method = {RequestMethod.GET})
    @ResponseBody
    public ResponseData<Daily> getDailyById(
            @ApiParam("日志ID") @RequestParam("id") int id,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Daily> responseData = new ResponseData();
        Daily daily = dailyService.getDailyById(id);
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }
        if (daily == null) {
            responseData.jsonFill(2, "日志不存在", null);
            return responseData;
        }

        responseData.jsonFill(1, null, daily);
        return responseData;
    }


    @ApiOperation(value = "删除自己的某个日志", notes = "需登录")
    @RequestMapping(value = "/deleteDaily", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<Boolean> deleteDaily(
            @ApiParam("日志ID") @RequestParam("dailyId") int dailyId,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Boolean> responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }

        Daily daily = dailyService.getDailyById(dailyId);
        if (daily == null) {
            responseData.jsonFill(2, "日志不存在", null);
            response.setStatus(404);
            return responseData;
        }
        if (!daily.getUserId().equals(user.getId())) {
            responseData.jsonFill(2, "无效的请求。", null);
            response.setStatus(404);
            return responseData;
        }
        boolean res = dailyService.deleteDaily(dailyId);
        if(res){
                messageFeedService.unfeedFollowers(dailyId, user.getId(), true);
        }
        responseData.jsonFill(res ? 1 : 2, null, res);
        return responseData;
    }

    @ApiOperation(value = "发布日志", notes = "需登录")
    @RequestMapping(value = "/publishDaily", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<Daily> publishDaily(
            @ApiParam("标题") @RequestParam("title") String title,
            @ApiParam("内容") @RequestParam("content") String content,
            @ApiParam("单图片url") @RequestParam(value = "picUrl",required = false) String picUrl,
            @ApiParam("多图片url") @RequestParam(value = "picUrls",required = false) ArrayList<String> picUrls,
            @ApiParam("可见性") @RequestParam("visibility") int visibility,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Daily> responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }
        Daily daily = new Daily();
        daily.setUserId(user.getId());
        daily.setTitle(title);
        daily.setContent(content);
        if (picUrl!=null)
            daily.setPicUrl(picUrl);
        if (picUrls!=null)
            daily.setPicUrls(new Gson().toJson(picUrls));
        daily.setVisibility(visibility);
        daily.setCreateTime(new Date());
        daily.setUpdateTime(new Date());
        daily = dailyService.saveDaily(daily);
        if (daily == null) {
            responseData.jsonFill(2, "发布失败", null);
        } else {
            responseData.jsonFill(1, null, daily);
            //如果对关注自己的人则feed TODO 抽取到service
            MsgVo msgVo = new MsgVo();
            msgVo.setUserId(user.getId());
            msgVo.setHeadImgUrl(user.getHeadImgUrl());
            msgVo.setUserName(user.getNickname());
            Feed feed = new Feed();
            feed.setCreateTime(new Date());
            feed.setUpdateTime(new Date());
            feed.setFid(user.getId());
            feed.setContent(new Gson().toJson(msgVo));
            feed.setMid(daily.getId());
            feed.setType(MessageType.NEW_DAILY);
            feed.setTid(user.getId());
            messageFeedService.feed(feed);
            if ((daily.getVisibility() & Visibility.FOLLOWER) > 0) {
                messageFeedService.feedFollowers(feed, user.getId(), false);
            }
        }
        return responseData;
    }

    @ApiOperation(value = "添加日志草稿", notes = "需登录")
    @RequestMapping(value = "/publishDraftDaily", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<Daily> publishDraftDaily(
            @ApiParam("标题") @RequestParam("title") String title,
            @ApiParam("内容") @RequestParam("content") String content,
            @ApiParam("单图片url") @RequestParam(value ="picUrl",required = false) String picUrl,
            @ApiParam("多图片url") @RequestParam(value ="picUrls",required = false) String picUrls,
            @ApiParam("可见性") @RequestParam("visibility") int visibility,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Daily> responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }
        Daily daily = new Daily();
        daily.setUserId(user.getId());
        daily.setTitle(title);
        daily.setContent(content);

        if (picUrl!=null){
            daily.setPicUrl(picUrl);
        }
        if (picUrls!=null){
            daily.setPicUrls(picUrls);
        }
        daily.setVisibility(visibility);
        daily.setCreateTime(new Date());
        daily.setUpdateTime(new Date());
        //先清除所有的草稿
        dailyService.deleteDraftDaily(user.getId());
        //然后发布草稿
        daily = dailyService.saveDraftDaily(daily);
        if (daily == null) {
            responseData.jsonFill(2, "添加草稿日志失败", null);
        } else {
            responseData.jsonFill(1, null, daily);
        }
        return responseData;
    }

    @ApiOperation(value = "删除日志草稿", notes = "需登录")
    @RequestMapping(value = "/deleteDraftDaily", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<Boolean> deleteDraftDaily(
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Boolean> responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }
        boolean res = dailyService.deleteDraftDaily(user.getId());
        responseData.jsonFill(res ? 1 : 2, null, res);
        return responseData;
    }

    @ApiOperation(value = "获得草稿日志", notes = "需要登录")
    @RequestMapping(value = "/getDraftDaily", method = {RequestMethod.GET})
    @ResponseBody
    public ResponseData<Daily> getDraftDaily(
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Daily> responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "请先登录", null);
            response.setStatus(401);
            return responseData;
        }
        responseData.jsonFill(1, null, dailyService.getDraftDaily(user.getId()));
        return responseData;
    }

    @ApiOperation(value = "读日志", notes = "")
    @RequestMapping(value = "/readDaily", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<Boolean> readDaily(
            @ApiParam("日志id") @RequestParam("id") Integer id,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Boolean> responseData = new ResponseData();
//        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
//        if (user == null) {
//            responseData.jsonFill(2, "请先登录", null);
//            response.setStatus(401);
//            return responseData;
//        }

        responseData.jsonFill(1, null, dailyService.newRead(id));
        return responseData;
    }

}

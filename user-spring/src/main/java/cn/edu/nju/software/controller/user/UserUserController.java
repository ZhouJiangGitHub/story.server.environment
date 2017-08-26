package cn.edu.nju.software.controller.user;

import cn.edu.nju.software.controller.BaseController;
import cn.edu.nju.software.entity.Business;
import cn.edu.nju.software.entity.ResponseData;
import cn.edu.nju.software.entity.User;
import cn.edu.nju.software.entity.UserBase;
import cn.edu.nju.software.enums.GrantType;
import cn.edu.nju.software.service.user.AppUserService;
import cn.edu.nju.software.service.user.UserWeChatLoginService;
import cn.edu.nju.software.util.*;
import cn.edu.nju.software.vo.WeChatOAuthVo;
import cn.edu.nju.software.vo.WeChatUserInfoVo;
import cn.edu.nju.software.vo.response.LoginResponseVo;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Api("user controller")
@Controller
@RequestMapping("/user")
public class UserUserController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(UserUserController.class);
    private static final String IMAGE_ROOT = "/head/";
    private final String default_avatar = "default_avatar.jpg";
    @Autowired
    private AppUserService userService;
    @Autowired
    private UserWeChatLoginService weChatLoginService;
    @Autowired
    private Business business;


    @ApiOperation(value = "获取用户信息", notes = "获取用户信息")
    @RequestMapping(value = "/getUserBaseInfo", method = {RequestMethod.GET})
    @ResponseBody
    public ResponseData<User> getUserInfo(
            @ApiParam("用户ID") @RequestParam("userId") int userId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResponseData responseData = new ResponseData();
        UserBase userBase = userService.getUserBaseById(userId);
        if (userBase == null) {
            responseData.jsonFill(2, "用户不存在。", null);
            return responseData;
        }
        responseData.jsonFill(1, null, userBase);
        return responseData;
    }

    @ApiOperation(value = "得到当前用户信息", notes = "获取用户信息")
    @RequestMapping(value = "/getUserSelfInfo", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<User> getUserSelfInfo(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResponseData responseData = new ResponseData();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "用户尚未登录。", false);
            return responseData;
        }
        user = userService.loginByUnionId(user.getWxUnionId());
        Jedis jedis = JedisUtil.getJedis();
        jedis.set(user.getAccessToken().getBytes(), ObjectAndByte.toByteArray(user));
        jedis.close();
        user.setVerifyCode(null);
        user.setExpireTime(null);
        user.setWxUnionId(null);
        user.setPassword(null);

        responseData.jsonFill(1, null, user);
        return responseData;
    }
    @ApiOperation(value = "修改当前用户信息", notes = "修改用户信息")
    @RequestMapping(value = "/updateUserSelfInfo", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<Boolean> getUserSelfInfo(HttpServletRequest request, HttpServletResponse response,
            @ApiParam("昵称") @RequestParam(value = "nickname",required = false) String nickname,
            @ApiParam("性别") @RequestParam(value = "sex",required = false) String sex,
            @ApiParam("公司") @RequestParam(value = "company",required = false) String company,
            @ApiParam("城市") @RequestParam(value = "city",required = false) String city
            ) throws Exception {
        ResponseData<Boolean> responseData=new ResponseData<>();
        User user = (User) request.getAttribute(TokenConfig.DEFAULT_USERID_REQUEST_ATTRIBUTE_NAME);
        if (user == null) {
            responseData.jsonFill(2, "用户尚未登录。", false);
            return responseData;
        }
        if(nickname!=null) user.setNickname(nickname);
        if(sex!=null) user.setSex(sex);
        if(company!=null) user.setCompany(company);
        if(city!=null) user.setCity(city);
        user.setUpdateTime(new Date());
        userService.updateUser(user);
        String AccessToken = request.getHeader(TokenConfig.DEFAULT_ACCESS_TOKEN_HEADER_NAME);
        Jedis jedis = JedisUtil.getJedis();
        jedis.set(AccessToken.getBytes(), ObjectAndByte.toByteArray(user));
        jedis.close();
        responseData.jsonFill(1, null,true);
        return responseData;
    }

    @ApiOperation(value = "用户登录", notes = "用户登录")
    @RequestMapping(value = "/loginByWeChat", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<LoginResponseVo> loginByWeChat(@ApiParam("code 授权码") @RequestParam("code") String code,
                                                       HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResponseData responseData = new ResponseData();
        WeChatOAuthVo weChatOAuthVo = weChatLoginService.getAccessToken(business.getWxAppId(),business.getWxSecret(), GrantType.AUTHORIZATION_CODE, code);
        if (null == weChatOAuthVo) {
            responseData.jsonFill(2, "微信OAuth失败", false);
            return responseData;
        }

        User user = userService.loginByUnionId(weChatOAuthVo.getUnionId());
        //如果数据库没有相应的用户那么新建一个用户
        if (null == user) {
            //获取微信用户信息
            WeChatUserInfoVo userInfo = weChatLoginService.getUserInfo(weChatOAuthVo.getAccessToken(), weChatOAuthVo.getOpenId());
            if (null == userInfo) {
                responseData.jsonFill(2, "获取微信用户信息失败", false);
                return responseData;
            }

            user = new User();
            user.setNickname(userInfo.getNickName());
            user.setWxUnionId(userInfo.getUnionId());//设置unionId
            user.setSex(parse(userInfo.getSex()));
            user.setVerifyCode(null);
            user.setCreateTime(new Date());
            user.setCity(userInfo.getCity());

            String realPath = UploadFileUtil.getBaseUrl() + IMAGE_ROOT;
            String avatar = userInfo.getUnionId() + ".jpg";

            try {
                DownloadUtil.download(userInfo.getHeadImgUrl(), avatar, realPath);
            } catch (Exception e) {
                logger.error("微信头像下载失败!");
                e.printStackTrace();
                //如果微信头像下载失败那么就使用默认头像
                user.setHeadImgUrl(UploadFileUtil.SOURCE_BASE_URL + IMAGE_ROOT + default_avatar);
            }
            user.setHeadImgUrl(UploadFileUtil.SOURCE_BASE_URL + IMAGE_ROOT + avatar);
            Util.setNewAccessToken(user);//设置token
            long currentTime = System.currentTimeMillis();
            currentTime += 1000 * 60 * 60 * 24 * 30;//设置为30天后失效
            user.setExpireTime(new Date(currentTime));
            user = userService.addOrUpdateUser(user);
        }

        //如果用户创建失败
        if (user == null) {
            responseData.jsonFill(2, "服务器出错", null);
            return responseData;
        }
        //获取到用户信息
        LoginResponseVo loginResponseVo = new LoginResponseVo();
        loginResponseVo.setAccessToken(user.getAccessToken());
        loginResponseVo.setId(user.getId());

        //登录信息写入缓存
        JedisUtil.getJedis().set(user.getAccessToken().getBytes(), ObjectAndByte.toByteArray(user));
        JedisUtil.getJedis().expire(user.getAccessToken().getBytes(), 60 * 60 * 24 * 30);//缓存用户信息30天

        responseData.jsonFill(1, null, loginResponseVo);
        return responseData;
    }


    @ApiOperation(value = "游客登录", notes = "")
    @RequestMapping(value = "/guestLogin", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseData<LoginResponseVo> guestLogin(
            @ApiParam("DeviceId") @RequestParam String deviceId) {
        ResponseData<LoginResponseVo> responseData = new ResponseData<>();
        User user = userService.getUserByDeviceId(deviceId);

        if (user == null) {
            user = new User();
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            user.setNickname("游客");
            user.setDeviceId(deviceId);
            user.setHeadImgUrl(UploadFileUtil.SOURCE_BASE_URL + IMAGE_ROOT + default_avatar);//设置默认头像
        }

        Util.setNewAccessToken(user);
        long currentTime = System.currentTimeMillis();
        currentTime += 1000 * 60 * 60 * 24 * 30;//设置为30天后失效
        user.setExpireTime(new Date(currentTime));

        user = userService.addOrUpdateUser(user);
        if (user == null) {
            responseData.jsonFill(2, "获取用户信息失败", null);
            return responseData;
        }
        //登录信息写入缓存
        Jedis jedis = JedisUtil.getJedis();
        jedis.set(user.getAccessToken().getBytes(), ObjectAndByte.toByteArray(user));
        jedis.expire(user.getAccessToken().getBytes(), 60 * 60 * 24 * 30);//缓存用户信息30天
        jedis.close();

        LoginResponseVo loginResponseVo = new LoginResponseVo();
        loginResponseVo.setAccessToken(user.getAccessToken());
        loginResponseVo.setId(user.getId());
        responseData.jsonFill(1, null, loginResponseVo);
        return responseData;
    }

    @ApiOperation(value = "测试用直接用ID登录(仅用于测试)", notes = "用户登录")
    @RequestMapping(value = "/mockLogin", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseData<LoginResponseVo> mockLogin(
            @ApiParam("用户ID") @RequestParam int id
    ) {
        ResponseData<LoginResponseVo> responseData = new ResponseData<>();
        User user = userService.getUserByMobileOrId(String.valueOf(id));
        if (user == null) {
            responseData.jsonFill(2, "错误", null);
            return responseData;
        }

        Util.setNewAccessToken(user);
        //TODO 更新数据库
        //登录信息写入缓存
        JedisUtil.getJedis().set(user.getAccessToken().getBytes(), ObjectAndByte.toByteArray(user));
        JedisUtil.getJedis().expire(user.getAccessToken().getBytes(), 60 * 60 * 24 * 30);//缓存用户信息30天

        LoginResponseVo loginResponseVo = new LoginResponseVo();
        loginResponseVo.setAccessToken(user.getAccessToken());
        loginResponseVo.setId(user.getId());
        responseData.jsonFill(1, null, loginResponseVo);   
        return responseData;
    }
    
    @RequestMapping(value = "/testError", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public boolean testError() {
        logger.error("ERROR");
        return true;
    }
    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public User test() {
        User user = new User();
        user.setNickname("xmc");
        user.setCity("南京");
        return user;
    }

    private String parse(int gendar) {
        if (gendar == 1) {
            return "男";
        } else if (gendar == 2) {
            return "女";
        }
        return "未知";
    }
}

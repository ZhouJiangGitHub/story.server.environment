package cn.edu.nju.software.service.impl;

import cn.edu.nju.software.dao.FollowRelationDao;
import cn.edu.nju.software.dao.UserDao;
import cn.edu.nju.software.dao.user.AppUserDao;
import cn.edu.nju.software.entity.FollowRelation;
import cn.edu.nju.software.enums.Visibility;
import cn.edu.nju.software.service.FollowService;
import cn.edu.nju.software.util.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by xmc1993 on 2017/5/12.
 */
@Service
public class FollowServiceImpl implements FollowService {
    @Autowired
    private FollowRelationDao followRelationDao;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private UserDao userDao;


    @Override
    public boolean saveFollowRelation(FollowRelation followRelation) {
        FollowRelation relation = followRelationDao.getFollowRelation(followRelation.getFollowerId(), followRelation.getFolloweeId());
        //已经关注了不能再次关注
        if (relation != null) {
            return false;
        }
        boolean res1 = followRelationDao.saveFollowRelation(followRelation);
        if (!res1) return false;
        boolean res2 = appUserDao.newFollower(followRelation.getFolloweeId());//被关注的人粉丝+1
        boolean res3 = appUserDao.newFollowee(followRelation.getFollowerId());//关注者关注的人数+1
        return res2 && res3;

    }

    @Override
    public boolean deleteFollowRelation(int followerId, int followeeId) {
        boolean res1 = followRelationDao.deleteFollowRelation(followerId, followeeId);
        if (!res1) return false;
        boolean res2 = appUserDao.loseFollower(followeeId);//被取消关注的人粉丝-1
        boolean res3 = appUserDao.removeFollowee(followerId);//取消关注者关注的人数-1
        return res2 && res3;
    }

    @Override
    public boolean deleteFollowRelationById(int id) {
        return followRelationDao.deleteFollowRelationById(id);
    }

    @Override
    public List<Integer> getUserFollowerList(int userId, int offset, int limit) {
        offset = offset < 0 ? Const.DEFAULT_OFFSET : offset;
        limit = limit < 0 ? Const.DEFAULT_LIMIT : limit;
        List<Integer> followerIdList = followRelationDao.getFollowerIdListByUserId(userId, offset, limit);
        return followerIdList;
    }

    @Override
    public Integer getUserFollowerCountByUserId(int userId){
        return followRelationDao.getFollowerCountByUserId(userId);
    }

    @Override
    public List<Integer> getUserFolloweeList(int userId, int offset, int limit) {
        offset = offset < 0 ? Const.DEFAULT_OFFSET : offset;
        limit = limit < 0 ? Const.DEFAULT_LIMIT : limit;
        List<Integer> followeeIdList = followRelationDao.getFolloweeIdListByUserId(userId, offset, limit);
        return followeeIdList;
    }

    @Override
    public Integer getUserFolloweeCountByUserId(int userId){
        return followRelationDao.getFolloweeCountByUserId(userId);
    }

    @Override
    public int getStatusBetween(int curUserId, int userId) {
        boolean res1 = followRelationDao.getFollowRelation(curUserId, userId) != null;
        boolean res2 = followRelationDao.getFollowRelation(userId, curUserId) != null;
        if (res1 && res2) {
            return 3;//互相关注
        }
        if (res2) {
            return 2;//单方面被关注
        }
        if (res1) {
            return 1;//单方面关注
        }
        return 0;//无关联
    }

    @Override
    public int getRelation(Integer visitedUserId, Integer visitorId) {
        //权限以visitedUserId为主体，即以被访问者为主体
        if (visitedUserId.equals(visitorId)) {
            //本人
            return Visibility.SELF;
        }
        int relation = getStatusBetween(visitedUserId, visitorId);
        switch (relation) {
            case 0://陌生人
                return Visibility.STRANGER;
            case 2://当方面被关注
                return Visibility.FOLLOWER;
            case 1://单方面关注了访客
                return Visibility.FOLLOWEE;
            case 3://相互关注
                return Visibility.MUTUAL;
            default:
                return 0;
        }
    }

    @Override
    public Boolean dummyFollowRelation(int userId) {
        Random random=new Random();
        //随机获取2-14个僵尸粉的id
        int count=random.nextInt(14)+2;
        List<Integer> list=userDao.getDummyIdListByCount(count);

        FollowRelation followRelation = new FollowRelation();
        followRelation.setCreateTime(new Date());
        followRelation.setUpdateTime(new Date());
        followRelation.setFolloweeId(userId);

        for (Integer dummyId : list) {
            followRelation.setFollowerId(dummyId);
            boolean res1 = followRelationDao.saveFollowRelation(followRelation);
            if (!res1) return false;
            //刷新僵尸粉的关注时间(使他关注平均)
            userDao.updateDummyTime(dummyId);
            boolean res2 = appUserDao.newFollower(followRelation.getFolloweeId());//被关注的人粉丝+1
            boolean res3 = appUserDao.newFollowee(followRelation.getFollowerId());//关注者关注的人数+1
            //如果失败了就返回false
            if (!(res2 && res3)){
                return false;
            }
        }
        return true;
    }
}

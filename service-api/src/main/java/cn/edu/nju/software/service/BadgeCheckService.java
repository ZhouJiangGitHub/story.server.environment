package cn.edu.nju.software.service;

import cn.edu.nju.software.entity.Badge;
import cn.edu.nju.software.entity.ResponseData;
import cn.edu.nju.software.entity.User;
import cn.edu.nju.software.entity.Works;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface BadgeCheckService {

    List<Badge> judgeAddBadgesWhenPublish(User user, Works works);

    ResponseData<Boolean> judgeAddFirstShareBadge(Integer userId);

    Badge judgeAddFirstAddOriginalStoryBadge(Integer userId);

    Badge judgeAddFirstFollowBadge(Integer userId, Integer followeeId);

    List<Badge> judgeAddBadgeWhenListen(User user);

    void judgeUserAddBadgeByListen(Integer authorId);

    /*Boolean freshBadge();*/
}

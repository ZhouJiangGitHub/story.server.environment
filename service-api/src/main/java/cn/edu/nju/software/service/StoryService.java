package cn.edu.nju.software.service;

import cn.edu.nju.software.entity.Story;

import java.io.File;
import java.util.List;

/**
 * Created by xmc1993 on 2017/5/12.
 */
public interface StoryService {
    boolean saveStory(Story story);

    boolean deleteStoryById(int id);

    Story getStoryById(int id);

    Story getStoryByIdHard(int id);

    List<Story> getAllStories();

    Story updateStory(Story story);

    List<Story> getStoryListByPage(int offset, int limit);

    List<Story> getStoryListByIdList(List<Integer> idList, Integer offset, Integer limit);

    List<Story> getStoryListByTitle(String title, int offset, int limit);

    boolean recommendStory(int id);

    boolean cancelRecommendStory(int id);

    List<Story> getRecommendedStoryListByPage(int offset, int limit);

    Integer getRecommendedStoryCount();

    boolean newTell(int id);

    boolean deleteTell(int id);

    Integer getStoryCount();

    String getOriginSoundLength(File file);

    Integer getStoryCountByTitle(String query);

    Integer getStoryCountByIdList(List<Integer> idList, int offset, int limit);
}

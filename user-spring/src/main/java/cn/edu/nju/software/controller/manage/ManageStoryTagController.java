package cn.edu.nju.software.controller.manage;

import cn.edu.nju.software.annotation.RequiredPermissions;
import cn.edu.nju.software.entity.ResponseData;
import cn.edu.nju.software.entity.StoryTag;
import cn.edu.nju.software.service.CheckValidService;
import cn.edu.nju.software.service.StoryTagService;
import cn.edu.nju.software.service.wxpay.util.RandCharsUtils;
import cn.edu.nju.software.util.UploadFileUtil;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * Created by xmc1993 on 2017/5/15.
 */
@Api(value = "Admin", description = "管理接口")
@Controller()
@RequestMapping("/manage")
public class ManageStoryTagController {
    private static final Logger logger = LoggerFactory.getLogger(ManageStoryTagController.class);
    @Autowired
    private CheckValidService checkValidService;
    @Autowired
    private StoryTagService storyTagService;

    private static final String ICON_ROOT = "/icon/"; //头像的基础路径

    @RequiredPermissions({1, 6})
    @ApiOperation(value = "新增标签", notes = "")
    @RequestMapping(value = "/storyTags", method = {RequestMethod.POST})
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public StoryTag publishStoryTag(
            @ApiParam("icon文件") @RequestParam(value = "uploadFile", required = false) MultipartFile uploadFile,
            @ApiParam("标签文字") @RequestParam("content") String content,
            @ApiParam("父标签ID") @RequestParam("parentId") int parentId,
            HttpServletRequest request, HttpServletResponse response) {
        StoryTag storyTag = new StoryTag();
        storyTag.setParentId(parentId);
        if (storyTag.getParentId() != 0 && !checkValidService.isTagExist(storyTag.getParentId())) {
            logger.error("无效的parentId");
            throw new RuntimeException("无效的parentId");
        }
        if (!uploadFile.isEmpty()) {
            String url = uploadFile(uploadFile);
            if (url == null) {
                throw new RuntimeException("上传图标失败");
            }
            storyTag.setIconUrl(url);
        }
        storyTag.setContent(content);
        storyTag.setCreateTime(new Date());
        storyTag.setUpdateTime(new Date());
        storyTag.setValid(1);
        storyTagService.saveStoryTag(storyTag);
        return storyTag;
    }

    @RequiredPermissions({1, 6})
    @ApiOperation(value = "新增标签V3", notes = "")
    @RequestMapping(value = "/v3/storyTags", method = {RequestMethod.POST})
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public StoryTag publishStoryTagV3(
            @ApiParam("icon文件url") @RequestParam(value = "url", required = false) String url,
            @ApiParam("标签文字") @RequestParam("content") String content,
            @ApiParam("父标签ID") @RequestParam("parentId") int parentId,
            HttpServletRequest request, HttpServletResponse response) {
        StoryTag storyTag = new StoryTag();
        storyTag.setParentId(parentId);
        if (storyTag.getParentId() != 0 && !checkValidService.isTagExist(storyTag.getParentId())) {
            logger.error("无效的parentId");
            throw new RuntimeException("无效的parentId");
        }
        storyTag.setIconUrl(url);
        storyTag.setContent(content);
        storyTag.setCreateTime(new Date());
        storyTag.setUpdateTime(new Date());
        storyTag.setValid(1);
        storyTagService.saveStoryTag(storyTag);
        return storyTag;
    }

    @RequiredPermissions({3, 6})
    @ApiOperation(value = "更新标签")
    @RequestMapping(value = "/v3/storyTags/{id}", method = {RequestMethod.POST})
    @ResponseBody
    public StoryTag updateStoryTagV3(
            @ApiParam("标签ID") @PathVariable int id,
            @ApiParam("icon文件") @RequestParam(value = "url", required = false) String url,
            @ApiParam("标签文字") @RequestParam("content") String content,
            @ApiParam("父标签ID") @RequestParam("parentId") int parentId,
            HttpServletRequest request, HttpServletResponse response) {
        StoryTag storyTag = storyTagService.getStoryTagById(id);
        if (storyTag == null) {
            throw new RuntimeException("标签不存在");
        }
        storyTag.setContent(content);
        storyTag.setParentId(parentId);
        if (url != null) {
            storyTag.setIconUrl(url);
        }
        storyTag.setUpdateTime(new Date());
        return storyTagService.updateStoryTag(storyTag);
    }

    @RequiredPermissions({3, 6})
    @ApiOperation(value = "更新标签", notes = "")
    @RequestMapping(value = "/storyTags/{id}", method = {RequestMethod.POST})
    @ResponseBody
    public StoryTag updateStoryTag(
            @ApiParam("标签ID") @PathVariable int id,
            @ApiParam("icon文件") @RequestParam(value = "uploadFile", required = false) MultipartFile uploadFile,
            @ApiParam("标签文字") @RequestParam("content") String content,
            @ApiParam("父标签ID") @RequestParam("parentId") int parentId,
            HttpServletRequest request, HttpServletResponse response) {
        StoryTag storyTag = storyTagService.getStoryTagById(id);
        if (storyTag == null) {
            throw new RuntimeException("标签不存在");
        }
        storyTag.setContent(content);
        storyTag.setParentId(parentId);
        if (!uploadFile.isEmpty()) {
            String url = uploadFile(uploadFile);
            if (url == null) {
                throw new RuntimeException("上传图标失败");
            }
            UploadFileUtil.deleteFileByUrl(storyTag.getIconUrl());
            storyTag.setIconUrl(url);
        }
        storyTag.setUpdateTime(new Date());
        return storyTagService.updateStoryTag(storyTag);
    }

    @RequiredPermissions({2, 6})
    @ApiOperation(value = "删除标签", notes = "")
    @RequestMapping(value = "/storyTags/{id}", method = {RequestMethod.DELETE})
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStoryTag(
            @ApiParam("标签ID") @PathVariable int id,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<Boolean> responseData = new ResponseData<>();
        if (!checkValidService.isTagExist(id)) {
            logger.error("无效的id");
            throw new RuntimeException("无效的id");
        }
        boolean success = storyTagService.deleteStoryTag(id);
        if (!success) {
            throw new RuntimeException("删除失败");
        }
    }

    @RequiredPermissions({4, 6})
    @ApiOperation(value = "获得一个标签的子标签列表", notes = "")
    @RequestMapping(value = "/storyTags/{id}/storyTags", method = {RequestMethod.GET})
    @ResponseBody
    public ResponseData<List<StoryTag>> getStoryTagListByParentId(
            @ApiParam("父标签ID") @PathVariable int id,
            HttpServletRequest request, HttpServletResponse response) {
        ResponseData<List<StoryTag>> result = new ResponseData<>();
        if (id != 0 && !checkValidService.isTagExist(id)) {
            logger.error("无效的parentId");
            throw new RuntimeException("无效的parentId");
        }
        List<StoryTag> tagList = storyTagService.getStoryTagListByParentId(id);
        if (tagList == null) {
            result.jsonFill(2, "获取一个标签的子标签列表失败", null);
            return result;
        } else {
            result.jsonFill(1, null, tagList);
            result.setCount(tagList.size());
            return result;
        }
    }

    @RequiredPermissions({4, 6})
    @ApiOperation(value = "根据ID获得标签", notes = "")
    @RequestMapping(value = "/storyTags/{id}", method = {RequestMethod.GET})
    @ResponseBody
    public StoryTag getStoryById(
            @ApiParam("标签ID") @PathVariable int id,
            HttpServletRequest request, HttpServletResponse response) {
        StoryTag storyTag = storyTagService.getStoryTagById(id);
        if (storyTag == null) {
            throw new RuntimeException("无效的ID");
        } else {
            return storyTag;
        }
    }

    @RequiredPermissions({4, 6})
    @ApiOperation(value = "标签列表", notes = "")
    @RequestMapping(value = "/storyTags", method = {RequestMethod.GET})
    @ResponseBody
    public ResponseData<List<StoryTag>> getAllStoryTags(
            @ApiParam("OFFSET") @RequestParam int offset,
            @ApiParam("LIMIT") @RequestParam int limit,
            HttpServletRequest request, HttpServletResponse response) {
        List<StoryTag> tagList = storyTagService.getStoryTagsByPage(offset, limit);
        ResponseData<List<StoryTag>> result = new ResponseData<>();
        if (tagList == null) {
            result.jsonFill(2, "获取故事标签列表失败", null);
            return result;
        } else {
            result.jsonFill(1, null, tagList);
            result.setCount(storyTagService.getStoryTagCount());
            return result;
        }
    }

    private String uploadFile(MultipartFile file) {
        String realPath = UploadFileUtil.getBaseUrl() + ICON_ROOT;
        String fileName = RandCharsUtils.getRandomString(16) + "." + UploadFileUtil.getSuffix(file.getOriginalFilename());
        boolean success = UploadFileUtil.mvFile(file, realPath, fileName);
        if (!success) {
            throw new RuntimeException("文件上传失败");
        }
        String url = UploadFileUtil.SOURCE_BASE_URL + ICON_ROOT + fileName;
        return url;
    }

    @RequiredPermissions({4, 6})
    @ApiOperation(value = "获取故事标签数量", notes = "")
    @RequestMapping(value = "/storyTagCount", method = {RequestMethod.GET})
    @ResponseBody
    public Integer getStoryTagCount() {
        return storyTagService.getStoryTagCount();
    }
}

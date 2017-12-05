package cn.edu.nju.software.service.impl;

import cn.edu.nju.software.service.OssService;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OssServiceImpl implements OssService {
    private static Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);
    private final static String host = "http://warmtale-backend-bucket.oss-cn-beijing.aliyuncs.com";
    private final static String accessId = "LTAIQLXhrN5gIJlk";

    @Autowired
    private OSSClient client;

    @Override
    public Map<String, String> getToken() {
        try {
            String dir = "backend/";
            long gapTime = 8;
            long expireEndTime = System.currentTimeMillis();
            Date expiration = new Date(expireEndTime - 8*60*60*1000);
            //表单限制
            PolicyConditions policyConditions = new PolicyConditions();
            policyConditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 500000000);
            policyConditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);
            //生成表单签名
            String postPolicy = client.generatePostPolicy(expiration, policyConditions);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            Map<String, String> respMap = new LinkedHashMap<>();
            respMap.put("accessid", accessId);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("host", host);
            respMap.put("dir", dir);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));
            return respMap;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}

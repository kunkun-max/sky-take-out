package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.properties.GithubProperties;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import java.util.*;

@RestController
@RequestMapping("/admin/common")
@Api(tags="通用接口")
@Slf4j
@RequiredArgsConstructor
public class CommonController {

    private final GithubProperties githubProperties;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping( value = "/upload")
    @ApiOperation("文件上传")
    public Result<String>upload(@RequestParam("file") MultipartFile file){
        log.info("文件上传：{}",file);

        try {
            //获取原始文件名及其后缀
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            //生成UUID新文件名，以防文件重名导致覆盖的冲突
            String objectName = UUID.randomUUID()+extension;

            //构建上传路径
            String datePath = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
            String fullPath = "img/"+datePath+"/"+objectName;

            //调用GitHub API上传（返回jsDelivr加速链接)
            String fileUrl = uploadToGitHub(file.getBytes(),fullPath);
            return Result.success(fileUrl);

        }catch (Exception e){
            log.error("文件上传失败:{}",e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }
    private String uploadToGitHub(byte[] fileBytes,String fullPath) throws Exception{

        String owner = githubProperties.getOwner();
        String repo = githubProperties.getRepo();
        String token = githubProperties.getToken();
        String branch =githubProperties.getBranch();
        String cdn = githubProperties.getCdn();


        //文件内容以Base64编码
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        //构建GitHub API请求URL
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s",owner,repo,fullPath);

        //构建请求体JSON
        Map<String,String>body = new HashMap<>();
        body.put("message","upload:"+fullPath);
        body.put("content",base64Content);
        body.put("branch",branch);

        //设置请求头
        RestTemplate restTemplate=new RestTemplate();
        HttpHeaders headers=new HttpHeaders();
        headers.set("Authorization","Bearer "+token);
        headers.set("Accept","application/vnd.github.v3+json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        //组装请求实体并发送PUT请求
        HttpEntity<Map<String,String>>request= new HttpEntity<>(body,headers);

        //发起请求
        restTemplate.exchange(apiUrl, HttpMethod.PUT,request,String.class);

        //返回jsDelivr加速链接
        return cdn+"/"+fullPath;
    }
}

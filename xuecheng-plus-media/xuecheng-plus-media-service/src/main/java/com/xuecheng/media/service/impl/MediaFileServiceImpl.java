package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.ToString;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Service
public class MediaFileServiceImpl implements MediaFileService {

 private static final Logger log = LoggerFactory.getLogger(MediaFileServiceImpl.class);
 @Autowired
 MediaFilesMapper mediaFilesMapper;
  @Autowired
 private MinioClient minioClient;
  @Autowired
  MediaFileService currentProxy;
  @Value("${minio.bucket.files}")
  private String bucket_Files;
 @Value("${minio.bucket.videofiles}")
 private String bucket_videoFiles;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }
 private String getMimeType(String extension){
  if (extension==null){
   extension="";
  }
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
  if(extensionMatch!=null){
   mimeType = extensionMatch.getMimeType();

  }
  return mimeType;
 }

 public boolean addMediaFilesToMinIO(String localFilePath,String objectName,String mimeType,String burket){
     UploadObjectArgs uploadObjectArgs = null;
     try {
         uploadObjectArgs = UploadObjectArgs.builder()
                 .bucket(burket)
                 .filename(localFilePath)
                 .object(objectName)
                 .contentType(mimeType)
                 .build();
      minioClient.uploadObject(uploadObjectArgs);
      log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket_Files,objectName);
      System.out.println("上传成功");
      return true;
     } catch (Exception e) {
         e.printStackTrace();
         log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket_Files,objectName,e.getMessage(),e);
      XueChengPlusException.cast("上传文件到文件系统失败");
     }
  return false;


 }
@Transactional(rollbackFor = Exception.class)
 public MediaFiles addMediaFilesToDb(String fileMd5,UploadFileParamsDto uploadFileParamsDto,String  objectName,
 Long companyId,String bucket){
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(mediaFiles==null){
   mediaFiles = new MediaFiles();
   BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
   mediaFiles.setFileId(fileMd5);
   mediaFiles.setFilePath(objectName);
   mediaFiles.setId(fileMd5);
   mediaFiles.setUrl("/"+bucket+"/"+objectName);
   mediaFiles.setCreateDate(LocalDateTime.now());
   mediaFiles.setCompanyId(companyId);
   mediaFiles.setBucket(bucket);
   mediaFiles.setAuditStatus("002003");
   mediaFiles.setStatus("1");
   int insert = mediaFilesMapper.insert(mediaFiles);

   if (insert<=0){
    log.error("上传文件到数据库出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName);
    XueChengPlusException.cast("保存文件信息失败");
   }
   log.debug("保存文件信息到数据库成功:{}",mediaFiles.toString());
  }
  return mediaFiles;
 }

 @Override
 public RestResponse<Boolean> checkFile(String fileMd5) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles!=null){
   GetObjectArgs getObjectArgs = GetObjectArgs.builder()
           .bucket(mediaFiles.getBucket())
           .object(mediaFiles.getFilePath())
           .build();
   FilterInputStream inputStream = null;
   try {
    inputStream = minioClient.getObject(getObjectArgs);
    if (inputStream!=null){
     return RestResponse.success(true);
    }
   } catch (Exception e) {
    e.printStackTrace();

   }

  }


  return RestResponse.success(false);
 }

 @Override
 public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
  GetObjectArgs getObjectArgs = GetObjectArgs.builder()
          .bucket(bucket_videoFiles)
          .object(getChunkFileFolderPath(fileMd5)+chunkIndex)
          .build();
  FilterInputStream inputStream = null;
  try {
   inputStream = minioClient.getObject(getObjectArgs);
   if (inputStream!=null){
    return RestResponse.success(true);
   }
  } catch (Exception e) {
   e.printStackTrace();

  }

  return RestResponse.success(false);
 }

 @Override
 public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
  String objectName=getChunkFileFolderPath(fileMd5)+chunk;
  String mimeType = getMimeType(null);
  boolean b = addMediaFilesToMinIO(localChunkFilePath, objectName, mimeType, bucket_videoFiles);
  if (!b){
   log.info("上传分块文件失败:{}",objectName);
   return RestResponse.validfail(false,"上传分块失败");
  }
  log.info("上传分块文件成功:{}",objectName);
  return RestResponse.success(true);
 }

 @Override
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
  String filename = uploadFileParamsDto.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  String filePathByMd5 = getFilePathByMd5(fileMd5, extension);
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  List<ComposeSource> composeSources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i -> ComposeSource.builder().bucket(bucket_videoFiles)
          .object(chunkFileFolderPath+i).build()).collect(Collectors.toList());

  ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder().bucket(bucket_videoFiles).object(filePathByMd5)
          .sources(composeSources)
          .build();
     try {
         minioClient.composeObject(composeObjectArgs);
      log.debug("合并文件成功:{}",filePathByMd5);
     } catch (Exception e) {
         e.printStackTrace();
         log.debug("合并分块失败,filemd5:{},异常信息:{}",fileMd5,e.getMessage());
     }
   //验证
  File file = downloadFileFromMinIO(bucket_videoFiles, filePathByMd5);
     if (file==null){
      log.debug("下载合并后文件失败,mergeFilePath:{}",filePathByMd5);
      return RestResponse.validfail(false, "下载合并后文件失败。");
     }
  try(FileInputStream fileInputStream = new FileInputStream(file)){
   String s = DigestUtils.md5Hex(fileInputStream);
   if (!s.equals(fileMd5)){
      log.debug("合并模块文件验证失败,filemd5:{},合并md5:{}",fileMd5,s);
    return RestResponse.validfail(false, "下载合并后文件失败。");
   }
   uploadFileParamsDto.setFileSize(file.length());
  }catch (Exception e){
   log.debug("合并模块文件验证失败,filemd5:{},异常信息:{}",fileMd5,e.getMessage());
   return RestResponse.validfail(false, "下载合并后文件失败。");
 }

  //添加到表
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(fileMd5, uploadFileParamsDto, filePathByMd5, companyId, bucket_videoFiles);
  if (mediaFiles==null){
   log.debug("添加到表失败");
  }
  clearChunkFiles(chunkFileFolderPath,chunkTotal);
  return RestResponse.success(true);


 }

 private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
  try {
   List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
           .limit(chunkTotal)
           .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
           .collect(Collectors.toList());

   RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
   Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
   results.forEach(r->{
    DeleteError deleteError = null;
    try {
     deleteError = r.get();
    } catch (Exception e) {
     e.printStackTrace();
     log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
    }
   });
  } catch (Exception e) {
   e.printStackTrace();
   log.error("清楚分块文件失败,chunkFileFolderPath:{}",chunkFileFolderPath,e);
  }

 }
 public File downloadFileFromMinIO(String bucket,String objectName){
  //临时文件
  File minioFile = null;
  FileOutputStream outputStream = null;
  try{
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .build());
   //创建临时文件
   minioFile=File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   IOUtils.copy(stream,outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  }finally {
   if(outputStream!=null){
    try {
     outputStream.close();
    } catch (IOException e) {
     e.printStackTrace();
    }
   }
  }
  return null;
 }

 private String getFilePathByMd5(String fileMd5,String fileExt){
  return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
 }

 //得到分块文件的目录
 private String getChunkFileFolderPath(String fileMd5) {
  return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
 }

 private String getDefaultFolderPath(){
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/") + "/";
  return folder;
 }
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }



 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
  File file = new File(localFilePath);
  String bucket=bucket_Files;
  String filename = uploadFileParamsDto.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  String mimeType = getMimeType(extension);
  String defaultFolderPath = getDefaultFolderPath();
  String fileMd5 = getFileMd5(new File(localFilePath));
  String  objectName=defaultFolderPath+fileMd5+extension;
  boolean b = addMediaFilesToMinIO(localFilePath, objectName, mimeType, bucket);
  uploadFileParamsDto.setFileSize(file.length());
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(fileMd5, uploadFileParamsDto, objectName, companyId, bucket);

  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
  return uploadFileResultDto;


 }
}

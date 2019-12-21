package com.cj.photogallery;

import android.util.Log;

import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;

import java.util.ArrayList;
import java.util.List;

public class QiniuPhotos {
    /*
    七牛云存储
    AccessKey: 
    SecretKey: 
     机房位置：   华南    Zone.zone2() ;     
     存储空间名： 
    测试域名:   http://xxxxx

    样式分隔符设置Style separator set:   -
    图片样式（缩略图模式，自建） :       imageFop

    http://xxxxx/1.jpg
    http://xxxx/1.jpg-imageFop

    关于Zone对象和机房的关系如下：

    机房 Zone对象
    华东 Zone.zone0()
    华北 Zone.zone1()
    华南 Zone.zone2()
    北美 Zone.zoneNa0()

    android-sdk:
        compile 'com.qiniu:qiniu-android-sdk:7.3.10'
    java-sdk:
        compile 'com.qiniu:qiniu-java-sdk:7.2.8'

     */
    private static final String qiniuPhotosURL = "http://q2uwdcslc.bkt.clouddn.com";
    private static final String TAG = "QiniuPhotos";

    private static final String accessKey = "JTttw7p-hX2PpR6nQtWlgFWi-77Z7LIfwIZU2f5J";
    private static final String secretKey = "o6njTIgJqRyf2IQX9yqEY7zDzrGou7UoI02Traz4";
    private static final String bucket = "cjunsh";
    private static final String StyleSeparator = "-";
    private static final String ImageMode = "imageFop";


    public List<GalleryItem> fetchRecentPhotos() {
        return downloadGalleryItems(null);
    }

    public List<GalleryItem> searchPhotos(String query) {
        return downloadGalleryItems(query);
    }

    private List<GalleryItem> downloadGalleryItems(String query) {

        List<GalleryItem> items = new ArrayList<>();

        Log.i(TAG, "query:" + query);

        getFileList(items, query);

        return items;
    }

    private void getFileList(List<GalleryItem> items, String query) {
        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Zone.zone2());
        //...其他参数参考类注释

        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);

        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 1000;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";

        //列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] FileItems = fileListIterator.next(); //UnknownHostException

            if (FileItems == null)//NullPointerException
                continue;

            for (FileInfo item : FileItems) {
                /*System.out.println(item.key);  file name
                System.out.println(item.hash);
                System.out.println(item.fsize);   file size
                System.out.println(item.mimeType);
                System.out.println(item.putTime);
                System.out.println(item.endUser);*/
                Log.i(TAG, "key:" + item.key + ",hash:" + item.hash + ",fsize:" + item.fsize + ",mimeType:" + item.mimeType);

                if (query != null && !item.key.contains(query))
                    continue;

                GalleryItem galleryItem = new GalleryItem();
                galleryItem.setId(item.hash);
                galleryItem.setCation(item.key);
                //galleryItem.setUrl(qiniuPhotosURL + "/"+ item.key + StyleSeparator + ImageMode);
                galleryItem.setUrl(qiniuPhotosURL + "/" + item.key);
                Log.i(TAG, "url:" + galleryItem.getUrl());

                items.add(galleryItem);
            }
        }
    }
}

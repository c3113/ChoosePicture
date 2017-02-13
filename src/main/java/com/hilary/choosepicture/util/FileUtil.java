package com.hilary.choosepicture.util;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by hilary on 16/7/11.
 *
 */
public class FileUtil {

    private final String APP = "/guantao";

    private final String PICTURE = "/picture";

    private final String PICTURE_PATH = Environment.getExternalStorageDirectory().getPath() + APP + PICTURE;

    private static FileUtil mUtil = new FileUtil();

    public static FileUtil getInstance() {
        return mUtil;
    }

    public String getPicturePath() {
        return PICTURE_PATH;
    }

    public String getPictureTempPath() {
        File file = new File(PICTURE_PATH);
        file.mkdirs();
        return getPicturePath() + "/temp.png";
    }

    /**
     * 检查外面存储是否可用
     * @return
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean savePictureToStorage(String fileName) {
        if(isExternalStorageWritable()) {
            File pictureFile = new File(PICTURE_PATH, fileName);
            pictureFile.mkdirs();
            try {
                return pictureFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}

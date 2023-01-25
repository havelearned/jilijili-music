package wang.jilijili.music.common.utils;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * @Auther: Amani
 * @Date: 2023/1/22 22:25
 * @Description:
 */
@Component(value = "readcsvutil")
public class ReadCSVUtil {
    /**
     * 读取路径下所有.csv文件
     *
     * @return List<String>
     */
    public List<String> getFileInPath(String filePath) {
        List<String> csvList = new ArrayList<>();
        File f = new File(filePath);
        File[] ts = f.listFiles();
        for (int i = 0; i < ts.length; i++) {
            if (ts[i].isFile()) {
                String fileName = ts[i].toString();
                //获取最后一个.的位置
                int lastIndexOf = fileName.lastIndexOf(".");
                //获取文件的后缀名
                String suffix = fileName.substring(lastIndexOf);
                if (suffix.equals(".csv")) {
                    csvList.add(fileName);
                }
            }
        }
        return csvList;
    }


    /**
     * 将.csv文件逐个处理并写入到json文件中;相当于main方法
     *
     * @return Integer
     */
    public Integer csv2txt(String filePath) throws IOException, JSONException {
        List<String> list = getFileInPath(filePath);
        int counter = 0;
        if (list != null) {
            for (String fileName : list) {
                counter += readCSVAndWrite(fileName, filePath);
            }
        }
        return counter;
    }

    public Integer readCSVAndWrite(String fileName, String filePath) throws IOException, JSONException {
        List<String[]> list = new ArrayList<>();
        FileInputStream inputStream = null;
        Scanner sc = null;
        int counter = 0;
        try {
            inputStream = new FileInputStream(fileName);

            sc = new Scanner(inputStream, "UTF-8");
            String headLine = new String();
            String[] headArray = null;
            if (sc.hasNextLine()) {
                headLine = sc.nextLine();
                headArray = headLine.split(",");
            }
            while (sc.hasNextLine()) {

                //因为.csv文件是按逗号分割的，为避免字段中含逗号而分割错误，先进行处理
                String line = sc.nextLine();
                String[] strArray = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (strArray.length != 0) {
                    list.add(strArray);
                    counter++;
                }

                //为避免数据量过于庞大导致内存溢出，分批处理
                if (list.size() == 20000) {
                    List<JSONObject> jsons = csv2JSON(headArray, list);
                    writeObj(jsons, filePath, fileName);
                    list.clear();
                } else if (!sc.hasNextLine()) {
                    List<JSONObject> jsons = csv2JSON(headArray, list);
                    writeObj(jsons, filePath, fileName);
                }
            }
            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
        return counter;
    }


    public Integer readCSVAndWrite(String fileName, String filePath, CountDownLatch downLatch) throws IOException, JSONException {
        List<String[]> list = new ArrayList<>();
        FileInputStream inputStream = null;
        Scanner sc = null;
        int counter = 0;
        try {
            inputStream = new FileInputStream(fileName);
            if (inputStream.getChannel().size() == 0) {
                downLatch.countDown();
            }
            sc = new Scanner(inputStream, "UTF-8");
            String headLine = new String();
            String[] headArray = null;
            if (sc.hasNextLine()) {
                headLine = sc.nextLine();
                headArray = headLine.split(",");
            }
            while (sc.hasNextLine()) {

                //因为.csv文件是按逗号分割的，为避免字段中含逗号而分割错误，先进行处理
                String line = sc.nextLine();
                String[] strArray = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (strArray.length != 0) {
                    list.add(strArray);
                    counter++;
                }

                //为避免数据量过于庞大导致内存溢出，分批处理
                if (list.size() == 20000) {
                    List<JSONObject> jsons = csv2JSON(headArray, list);
                    writeObj(jsons, filePath, fileName);
                    list.clear();
                } else if (!sc.hasNextLine()) {
                    List<JSONObject> jsons = csv2JSON(headArray, list);
                    downLatch.countDown();
                    writeObj(jsons, filePath, fileName);
                }
            }
            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
        return counter;
    }


    /**
     * 将csv中的一行数据转换成一个一级json
     *
     * @param keys   json的key，顺序需与csv中的value对应
     * @param values csv中数据作为value
     * @return
     */
    public JSONObject csv2JSON(String[] keys, String[] values) throws JSONException {
        JSONObject json = new JSONObject();
        for (int i = 0; i < keys.length; i++) {
            try {
                String value = values[i].replace("\"", "");
                json.put(keys[i], value);
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        return json;
    }

    /**
     * 将csv的每一行数据都转换成一级json，返回json数组
     *
     * @param keys        json的key，顺序需与csv中的value对应
     * @param stringsList 读取csv返回的List<String[]>
     * @return
     */
    public List<JSONObject> csv2JSON(String[] keys, List<String[]> stringsList) throws JSONException {
        List<JSONObject> jsons = new ArrayList<JSONObject>() {
        };
        int index = 0;
        for (String[] strings : stringsList
        ) {
            JSONObject json = this.csv2JSON(keys, strings);
            jsons.add(json);
        }
        return jsons;
    }


    /**
     * 将处理完后的数据存入.json文件中
     *
     * @return
     */
    public void writeObj(List<JSONObject> jsonObjects, String path, String fileName) throws IOException {
        String newFileName = fileName.replace(".csv", ".json");
        try {
            // 防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw
            /* 写入Txt文件 */
            File writename = new File(path);// 相对路径，如果没有则要建立一个新的output。txt文件
            if (!writename.exists()) {
                writename.mkdirs();
            }
            writename = new File(newFileName);// 相对路径，如果没有则要建立一个新的output。txt文件
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename, true));
            for (JSONObject jsonObject : jsonObjects) {
                out.write(jsonObject.toString() + "\n");
            }
            System.out.println("写入成功！");
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
package com.istar.mediabroken.utils

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUploadBase
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.http.HttpStatus
import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook

import javax.servlet.http.HttpServletRequest

/**
 * Author : YCSnail
 * Date   : 2017-04-12
 * Email  : liyancai1986@163.com
 */

class UploadUtil {

    public static Object uploadImg(HttpServletRequest request, String filePath, String imgType) {

        //获取并解析文件类型和支持最大值
        def fileTypeLib = ["jpg", "jpeg", "gif", "png"];
        String maxSize = "2048";   //KB

        DiskFileItemFactory factory = new DiskFileItemFactory();
        //最大缓存
        factory.setSizeThreshold(10 * 1024);
        //设置临时文件目录
        factory.setRepository(new File(filePath));

        ServletFileUpload upload = new ServletFileUpload(factory);
        //文件最大上限
        upload.setSizeMax(Integer.valueOf(maxSize) * 1024);

        def result = null
        try {
            //获取所有文件列表
            List<FileItem> items = upload.parseRequest(request);
            String completeFilePath = ''
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    //文件名
                    String fileName = item.getName();
                    //检查文件后缀格式
                    String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

                    if (fileTypeLib.indexOf(fileExt) == -1) {
                        result = [
                                status: HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                                msg   : '请上传png/jpg/gif格式的图片'
                        ]
                        break;
                    }

                    //真实上传路径
                    completeFilePath = new StringBuilder()
                            .append('/upload/')
                            .append(imgType)
                            .append(DateFormatUtils.format(new Date(), "/yyyy/MM/dd/"))
                            .toString()

                    String newFileName = new StringBuilder()
                            .append(DateFormatUtils.format(new Date(), "HHmmssSSS"))
                            .append(RandomUtils.nextInt(0, 1000))
                            .append('.')
                            .append(fileExt)
                            .toString()

                    //写入文件
                    File fileDirectory = new File(filePath + completeFilePath);
                    if (!fileDirectory.exists()) {
                        fileDirectory.mkdirs()
                    }
                    item.write(new File(filePath + completeFilePath + newFileName));

                    String host = UrlUtils.getBjjHost(request)
                    result = [
                            status: HttpStatus.SC_OK,
                            msg   : host + completeFilePath + newFileName
                    ]
                    break
                }
            }
            //上传成功
            return result
        } catch (FileUploadBase.SizeLimitExceededException e) {
            return [
                    status: HttpStatus.SC_REQUEST_TOO_LONG,
                    msg   : '请将图片大小控制在2M以内'
            ]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object uploadImgFile(HttpServletRequest request, String filePath, String fileType) {
        def result = uploadFileByType(request, filePath, fileType, ["jpg", "jpeg", "gif", "png"]);
        return result
    }

    public static Object uploadWord(HttpServletRequest request, String filePath, String fileType) {
        def result = uploadFileByType(request, filePath, fileType, ["doc", "docx"]);
        return result
    }

    public static Object uploadExcel(HttpServletRequest request, String filePath, String fileType) {
        def result = uploadFileByType(request, filePath, fileType, ["xls"]);
        return result
    }

    public static Object uploadFileByType(HttpServletRequest request, String filePath, String fileType,
                                          def allowFileType) {

        //获取并解析文件类型和支持最大值
        def fileTypeLib = allowFileType;
        String maxSize = "512";   //KB
        request.setCharacterEncoding("utf-8")
        DiskFileItemFactory factory = new DiskFileItemFactory();
        //最大缓存
        factory.setSizeThreshold(5 * 1024);
        //设置临时文件目录
        factory.setRepository(new File(filePath));

        ServletFileUpload upload = new ServletFileUpload(factory);
        //文件最大上限
        upload.setSizeMax(Integer.valueOf(maxSize) * 1024);
        String fileName = "文件名"
        def result = null
        try {
            //获取所有文件列表
            List<FileItem> items = upload.parseRequest(request);
            String completeFilePath = ''
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    //文件名
                    fileName = item.getName();
                    //检查文件后缀格式
                    String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

                    if (fileTypeLib.indexOf(fileExt) == -1) {
                        result = [
                                status: HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                msg   : '格式不支持'
                        ]
                        break;
                    }

                    //真实上传路径
                    completeFilePath = new StringBuilder()
                            .append('/upload/')
                            .append(fileType)
                            .append(DateFormatUtils.format(new Date(), "/yyyy/MM/dd/"))
                            .toString()

                    String newFileName = new StringBuilder()
                            .append(DateFormatUtils.format(new Date(), "HHmmssSSS"))
                            .append(RandomUtils.nextInt(0, 1000))
                            .append('.')
                            .append(fileExt)
                            .toString()

                    //写入文件
                    File fileDirectory = new File(filePath + completeFilePath);
                    if (!fileDirectory.exists()) {
                        fileDirectory.mkdirs()
                    }
                    item.write(new File(filePath + completeFilePath + newFileName));

                    result = [
                            status  : HttpStatus.SC_OK,
                            msg     : completeFilePath + newFileName,
                            fileName: fileName
                    ]
                    break
                }
            }
            //上传成功
            return result
        } catch (FileUploadBase.SizeLimitExceededException e) {
            return [
                    status: HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    msg   : '文件大小请控制在500k以内'
            ]
        } catch (Exception e) {
            return [
                    status: HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    msg   : '上传文件发生错误'
            ]
        }
    }

    public static Object uploadFile(HttpServletRequest request, String filePath, String fileType) {

        //获取并解析文件类型和支持最大值
        def fileTypeLib = ["jpg", "jpeg", "gif", "png", "xls"];
        String maxSize = "500";   //KB

        DiskFileItemFactory factory = new DiskFileItemFactory();
        //最大缓存
        factory.setSizeThreshold(5 * 1024);
        //设置临时文件目录
        factory.setRepository(new File(filePath));

        ServletFileUpload upload = new ServletFileUpload(factory);
        //文件最大上限
        upload.setSizeMax(Integer.valueOf(maxSize) * 1024);

        def result = null
        try {
            //获取所有文件列表
            List<FileItem> items = upload.parseRequest(request);
            String completeFilePath = ''
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    //文件名
                    String fileName = item.getName();
                    //检查文件后缀格式
                    String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

                    if (fileTypeLib.indexOf(fileExt) == -1) {
                        result = [
                                status: HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                                msg   : '格式不支持'
                        ]
                        break;
                    }

                    //真实上传路径
                    completeFilePath = new StringBuilder()
                            .append('/upload/')
                            .append(fileType)
                            .append(DateFormatUtils.format(new Date(), "/yyyy/MM/dd/"))
                            .toString()

                    String newFileName = new StringBuilder()
                            .append(DateFormatUtils.format(new Date(), "HHmmssSSS"))
                            .append(RandomUtils.nextInt(0, 1000))
                            .append('.')
                            .append(fileExt)
                            .toString()

                    //写入文件
                    File fileDirectory = new File(filePath + completeFilePath);
                    if (!fileDirectory.exists()) {
                        fileDirectory.mkdirs()
                    }
                    item.write(new File(filePath + completeFilePath + newFileName));

                    result = [
                            status: HttpStatus.SC_OK,
                            msg   : completeFilePath + newFileName
                    ]
                    break
                }
            }
            //上传成功
            return result
        } catch (FileUploadBase.SizeLimitExceededException e) {
            return [
                    status: HttpStatus.SC_REQUEST_TOO_LONG,
                    msg   : '文件大小请控制在500k以内'
            ]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*批量导入站点*/

    public static List<Map<String, String>> parseExcel(InputStream fis) {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>(); ;
        try {
            HSSFWorkbook book = new HSSFWorkbook(fis);
            HSSFSheet sheet = book.getSheetAt(0);
            int firstRow = 2//sheet.getFirstRowNum();从第三行开始读表头
            int lastRow = sheet.getLastRowNum();
            //除去表头和第一行
//          ComnDao dao = SysBeans.getComnDao();
            for (int i = firstRow + 5; i < lastRow + 1; i++) {
                Map map = new HashMap();

                HSSFRow row = sheet.getRow(i);
                if (row != null) {
                    for (int j = 1; j < 4; j++) {
                        HSSFCell cellKey = sheet.getRow(firstRow).getCell(j);
                        String key = cellKey.getStringCellValue();

                        HSSFCell cell = row.getCell(j);
                        if (cell != null) {
                            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                                cell.setCellType(HSSFCell.CELL_TYPE_STRING);
                            }
                            String val = cell.getStringCellValue();
                            if (val == null || "".equals(val)) {
                                continue;
                            }
                            map.put(key, val);
                        } else {
                            map.put(key, "");
                        }
                    }
                }
                if (map.size() != 0) {
                    data.add(map);
                } else {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /*导入用户自己的栏目*/

    public static List<Map<String, String>> parseExcel2Tag(InputStream fis) {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>(); ;
        try {
            HSSFWorkbook book = new HSSFWorkbook(fis);
            HSSFSheet sheet = book.getSheetAt(0);
            int firstRow = 0//sheet.getFirstRowNum();从第2行开始读表头
            int lastRow = sheet.getLastRowNum();
            //除去表头和第一行
//          ComnDao dao = SysBeans.getComnDao();
            for (int i = firstRow + 1; i < lastRow + 1; ++i) {
                Map map = new HashMap();

                HSSFRow row = sheet.getRow(i);
                if (row != null) {
                    for (int j = 0; j < 6; j++) {
                        HSSFCell cellKey = sheet.getRow(firstRow).getCell(j);
                        if (cellKey) {
                            String key = cellKey.getStringCellValue();

                            HSSFCell cell = row.getCell(j);
                            if (cell != null) {
                                if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                                    cell.setCellType(HSSFCell.CELL_TYPE_STRING);
                                }
                                String val = cell.getStringCellValue();
                                if (val == null || "".equals(val)) {
                                    continue;
                                }
                                map.put(key, val);
                            } else {
                                map.put(key, "");
                            }
                        }

                    }
                }
                if (map.size() != 0) {
                    data.add(map);
                } else {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}

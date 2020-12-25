package cz.zcu.kiv.offscreen.storage;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileLoader {

    private static final int MAX_FILE_SIZE = 512000000;

    private final List<FileItem> fileItems;

    public FileLoader(HttpServletRequest request) throws FileUploadException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(MAX_FILE_SIZE);

        this.fileItems = upload.parseRequest(request);
    }

    /**
     * Get multipart data from request which was send and return it as a map where key is field name from form and
     * second value are data as string.
     *
     * @return loaded multipart data in map or empty map.
     */
    public Map<String, String> loadFormFields() {
        Map<String, String> resultMap = new HashMap<>();
        for (FileItem item : fileItems) {
            if (item.isFormField()) {
                resultMap.put(item.getFieldName(), item.getString());
            } else {
                resultMap.put("filename", item.getName());
            }
        }
        return resultMap;
    }

    public byte[] loadFileAsBytes(String name) {
        for (FileItem item : fileItems) {
            if (!item.isFormField() && item.getName().equals(name)) {
                return item.get();
            }
        }
        return null;
    }

}

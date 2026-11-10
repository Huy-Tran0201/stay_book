package com.staybook.staybook.room;

import com.staybook.staybook.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
    public static final long MAX_SIZE = 2L * 1024 * 1024;
    private final Path uploadDirectory;

    public ImageStorageService(@Value("${staybook.upload-dir:${UPLOAD_DIR:uploads}}") String uploadDirectory) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("Ảnh không được để trống.");
        if (file.getSize() > MAX_SIZE) throw new BusinessException("Ảnh không được vượt quá 2 MB.");
        String format = detectFormat(file);
        if (!"jpg".equals(format) && !"png".equals(format)) {
            throw new BusinessException("Chỉ chấp nhận ảnh JPG hoặc PNG hợp lệ.");
        }
        String fileName = UUID.randomUUID() + "." + format;
        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(uploadDirectory.resolve(fileName));
            return fileName;
        } catch (IOException e) {
            throw new BusinessException("Không thể lưu ảnh. Vui lòng thử lại.", e);
        }
    }

    private String detectFormat(MultipartFile file) {
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) return "";
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return "";
            String format = readers.next().getFormatName().toLowerCase();
            return "jpeg".equals(format) ? "jpg" : format;
        } catch (IOException e) {
            return "";
        }
    }

    public Path resolve(String fileName) {
        Path resolved = uploadDirectory.resolve(fileName).normalize();
        if (!resolved.getParent().equals(uploadDirectory)) throw new BusinessException("Tên ảnh không hợp lệ.");
        return resolved;
    }
}

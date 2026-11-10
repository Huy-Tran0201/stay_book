package com.staybook.staybook.room;

import static org.junit.jupiter.api.Assertions.assertThrows;
import com.staybook.staybook.common.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {
    @TempDir java.nio.file.Path tempDir;

    @Test
    void rejectsExecutableContentEvenWhenFileNameLooksLikeImage() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg",
            "MZ-not-an-image".getBytes());
        assertThrows(BusinessException.class, () -> service.store(file));
    }

    @Test
    void rejectsThreeMegabyteImage() throws Exception {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] image = pngBytes();
        byte[] oversized = java.util.Arrays.copyOf(image, 3 * 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile("image", "large.png", "image/png", oversized);
        assertThrows(BusinessException.class, () -> service.store(file));
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

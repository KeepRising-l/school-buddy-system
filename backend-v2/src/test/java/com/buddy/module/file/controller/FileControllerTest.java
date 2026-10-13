package com.buddy.module.file.controller;

import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.store.AuxStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 上传接口回归测试：类型/大小/伪装文件/频率限制/Redis 降级
 */
class FileControllerTest {

    private FileController controller;
    private AuxStore auxStore;

    /** 已写入的测试文件，测试后清理 */
    private Path savedFile;

    @BeforeEach
    void setUp() {
        controller = new FileController();
        auxStore = mock(AuxStore.class);
        lenient().when(auxStore.increment(anyString(), any())).thenReturn(1L);
        ReflectionTestUtils.setField(controller, "auxStore", auxStore);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (savedFile != null) {
            Files.deleteIfExists(savedFile);
        }
    }

    private static byte[] jpegBytes() {
        byte[] data = new byte[64];
        data[0] = (byte) 0xFF;
        data[1] = (byte) 0xD8;
        data[2] = (byte) 0xFF;
        data[3] = (byte) 0xE0;
        return data;
    }

    private static byte[] pngBytes() {
        byte[] data = new byte[64];
        data[0] = (byte) 0x89;
        data[1] = 0x50;
        data[2] = 0x4E;
        data[3] = 0x47;
        return data;
    }

    private R<String> upload(MockMultipartFile file) {
        R<String> result = controller.uploadAvatar(file);
        String url = result.getData();
        if (url != null && url.startsWith("/uploads/avatar/")) {
            savedFile = Paths.get("uploads/avatar/" + url.substring("/uploads/avatar/".length()));
        }
        return result;
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请选择要上传的文件");
    }

    @Test
    void rejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg",
                new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2MB");
    }

    @Test
    void rejectsWrongContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持jpg/png");
    }

    @Test
    void rejectsFakeImageContent() {
        // 声称是 png、实际是文本 -> 魔数校验拦截
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持jpg/png");
    }

    @Test
    void acceptsJpegAndForcesSafeExtension() {
        // 文件名伪装成 .html，内容是真 JPEG -> 保存为 .jpg（不信任客户端文件名）
        MockMultipartFile file = new MockMultipartFile("file", "evil.html", "image/jpeg", jpegBytes());

        R<String> result = upload(file);

        assertThat(result.getData()).matches("/uploads/avatar/[0-9a-f]{32}\\.jpg");
    }

    @Test
    void acceptsPng() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", pngBytes());

        R<String> result = upload(file);

        assertThat(result.getData()).matches("/uploads/avatar/[0-9a-f]{32}\\.png");
    }

    @Test
    void rejectsWhenRateLimitExceeded() {
        when(auxStore.increment(anyString(), any())).thenReturn(21L);
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", jpegBytes());

        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过于频繁");
    }

    @Test
    void uploadProceedsWhenAuxStoreUnavailable() {
        when(auxStore.increment(anyString(), any())).thenThrow(new RuntimeException("store down"));
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", jpegBytes());

        R<String> result = upload(file);

        assertThat(result.getData()).startsWith("/uploads/avatar/");
    }
}

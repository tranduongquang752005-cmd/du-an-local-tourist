package util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

public final class QRCodeUtil {

    private static final int MIN_SIZE = 120;
    private static final int MAX_SIZE = 1000;
    private static final int DEFAULT_SIZE = 320;
    private static final int MAX_QR_CONTENT_LENGTH = 1000;

    private QRCodeUtil() {
    }

    public static String toPngBase64(String content) {
        return toPngBase64(content, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public static String toPngBase64(String content, int width, int height) {
        String cleanContent = cleanString(content);

        if (cleanContent == null) {
            throw new IllegalArgumentException("QR content khong duoc de trong.");
        }

        if (cleanContent.length() > MAX_QR_CONTENT_LENGTH) {
            throw new IllegalArgumentException("QR content qua dai.");
        }

        int safeWidth = normalizeSize(width);
        int safeHeight = normalizeSize(height);

        try {
            BitMatrix bitMatrix = createBitMatrix(cleanContent, safeWidth, safeHeight);
            BufferedImage image = toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            throw new IllegalStateException("Tao QR PNG Base64 that bai.", e);
        }
    }

    public static String toPngDataUri(String content) {
        return toPngDataUri(content, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public static String toPngDataUri(String content, int width, int height) {
        return "data:image/png;base64," + toPngBase64(content, width, height);
    }

    private static BitMatrix createBitMatrix(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    }

    private static BufferedImage toBufferedImage(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int black = 0xFF000000;
        int white = 0xFFFFFFFF;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? black : white);
            }
        }

        return image;
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }

        if (size < MIN_SIZE) {
            return MIN_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private static String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

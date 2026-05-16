package com.uit.backend_cinema.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QRCodeUtil {

    private QRCodeUtil() {
        // Utility class, không cho khởi tạo
    }

    /**
     * Tạo ảnh QR Code dưới dạng byte[] theo định dạng PNG.
     *
     * @param text   Nội dung cần mã hoá vào QR (VD: "TICKET:1:BOOKING:1:SEAT:5")
     * @param width  Chiều rộng ảnh (px)
     * @param height Chiều cao ảnh (px)
     * @return mảng byte PNG của ảnh QR Code
     */
    public static byte[] generateQRCodeImage(String text, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Không thể tạo mã QR Code cho nội dung: " + text, e);
        }
    }
}

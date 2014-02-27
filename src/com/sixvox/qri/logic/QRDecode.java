package com.sixvox.qri.logic;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class QRDecode {
	private static Logger l = Logger.getLogger(QRDecode.class.getCanonicalName());

	private static final Map<DecodeHintType, Object> DECODE_HINTS;
	static {
		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		Collection<BarcodeFormat> vector = new ArrayList<BarcodeFormat>(2);
		vector.add(BarcodeFormat.CODE_39);
		vector.add(BarcodeFormat.QR_CODE);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		DECODE_HINTS = Collections.unmodifiableMap(hints);
	}

	public Result decode(URI uri) throws IOException {
		BufferedImage image;
		try {
			image = ImageIO.read(uri.toURL());
		} catch (IllegalArgumentException iae) {
			throw new FileNotFoundException("Resource not found: " + uri);
		}
		if (image == null) {
			l.warning(uri.toString() + ": Could not load image");
			return null;
		}
		try {
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			return new MultiFormatReader().decode(bitmap, DECODE_HINTS);
		} catch (NotFoundException nfe) {
			l.info(uri.toString() + ": No barcode found");
			return null;
		}
	}
}
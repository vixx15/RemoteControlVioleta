import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class ByteToBufferedImage {
    public static BufferedImage convert(byte[] imageData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            return ImageIO.read(bais);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

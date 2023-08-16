import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PokreniVideoPrenos implements Runnable{

    DataOutputStream dos;

    PokreniVideoPrenos(DataOutputStream doss){
        dos = doss;
    }

    @Override
    public void run() {
        try{
            Thread.sleep(200);
            BufferedImage slika = napraviScreenshot();
            byte[] slikaKaoNizBajtova = pretvoriSlikuUBajtove(slika);


            dos.writeInt(slikaKaoNizBajtova.length);
            dos.write(slikaKaoNizBajtova);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private byte[] pretvoriSlikuUBajtove(BufferedImage slika) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(slika, "png", baos);
        } catch (IOException e) {
            System.out.println("greksa prilikom pretvaranja slike u bajtove");
        }
        byte[] imageData = baos.toByteArray();
        return imageData;

    }

    private BufferedImage napraviScreenshot()  {
        Robot robot = null;
        BufferedImage screenshot;
        try {
            robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            screenshot = robot.createScreenCapture(screenRect);
            return screenshot;
        } catch (AWTException e) {
            System.out.println("Greska prilikom pravljenja screenshot-a.");
        }
        return null;
    }
}

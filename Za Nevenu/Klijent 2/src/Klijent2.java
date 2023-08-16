import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class Klijent2 {

    // Pretpostavljena rezolucija ekrana servera je 1920x1080
    static final int originalScreenWidth = 1920;
    static final int originalScreenHeight = 1080;

    JFrame frame;
    static JLabel label;
    DataOutputStream dos;

    // Konstruktor klase Klijent2
    Klijent2(Socket clientSocket) {
        // Inicijalizacija prozora
        frame = new JFrame("Klijent 2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 420);
        frame.setLayout(new BorderLayout());  // Podesavanje rasporeda za BorderLayout

        // Postavljanje labela u centar prozora
        label = new JLabel("Cekamo slicicu", SwingConstants.CENTER);

        frame.add(label, BorderLayout.CENTER);
        frame.setVisible(true);

        // Inicijalizacija izlaznog toka
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Dodavanje listenera za događaj klika mišem
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    // Izračunavanje razmere između originalne i trenutne veličine slike
                    int displayedImageWidth = label.getWidth();
                    int displayedImageHeight = label.getHeight();

                    double scaleX = (double) originalScreenWidth / displayedImageWidth;
                    double scaleY = (double) originalScreenHeight / displayedImageHeight;

                    // Izračunavanje stvarne pozicije klika
                    int actualX = (int) (e.getX() * scaleX);
                    int actualY = (int) (e.getY() * scaleY);

                    // Slanje koordinata i tipa klika serveru
                    dos.writeInt(actualX);
                    dos.writeInt(actualY);
                    dos.writeInt(e.getButton());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        frame.add(label);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            // Povezivanje sa serverom
            Socket clientSocket = new Socket("localhost", 12345);
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

            Klijent2 klijent2 = new Klijent2(clientSocket);

            // Čekanje i prijem slike od servera
            while (true) {
                int imageDataLength = dis.readInt();
                byte[] imageData = new byte[imageDataLength];
                dis.readFully(imageData);

                // Konverzija bajtova u sliku i postavljanje slike na labelu
                BufferedImage slika = ByteToBufferedImage.convert(imageData);
                Icon slikaa = new ImageIcon(slika);
                label.setIcon(slikaa);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

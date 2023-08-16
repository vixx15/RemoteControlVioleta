import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class KlijentGUI {
    private JPanel panel1;
    private JLabel ipLabel;
    private JTextField ipInput;
    private JButton ipConfirm;
    private JLabel ipInfo;
    private JPanel PanelSlike;
    private JPanel PanelDugmica;
    private JLabel label;

    private String serverIpAddress;

    static final int originalScreenWidth = 1920;
    static final int originalScreenHeight = 1080;

    KlijentGUI() {
        ipConfirm.addActionListener(e -> {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {

                  //OVDJE TREBA DA SE NAPRAVI VALIDACIJA INPUTA DA BUDE IP I DA SE ONDA TAJ INPUT PROSLIJEDI U START CLIENT ZA KREIRANJE SOCKETA (tekst iz ipInput)
                    startClient();
                    return null;
                }
            };
            worker.execute();
        });
    }

    private void startClient() {
        Socket clientSocket = null;
        DataOutputStream dos = null;

        try {
            clientSocket = new Socket("localhost", 12345);
            dos = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("Created output stream!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {

            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            System.out.println("Created input stream!");


            while (true) {
                System.out.println("Waiting for image from server...");
                int imageDataLength = dis.readInt();
                byte[] imageData = new byte[imageDataLength];
                dis.readFully(imageData);

                // Convert bytes to image and update label
                //BufferedImage slika = ByteToBufferedImage.convert(imageData);
                //Icon slikaa = new ImageIcon(slika);

                BufferedImage slika = ImageIO.read(new ByteArrayInputStream(imageData));
                Icon slikaa = new ImageIcon(slika);

                SwingUtilities.invokeLater(() -> {
                    label.setIcon(slikaa);
                    System.out.println("Image received and displayed!");
                    label.revalidate(); // Refresh the label
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the socket if needed
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }




        //slanje klika
        DataOutputStream finalDos = dos;
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
                    int eventType = 0;//FLAG da se radi o kliku misa, da bi uvijek znao da sta se salje

                    // Slanje koordinata i tipa klika serveru
                    finalDos.writeInt(eventType);
                    finalDos.writeInt(actualX);
                    finalDos.writeInt(actualY);
                    finalDos.writeInt(e.getButton());
                    finalDos.flush();

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        //slanje unosa sa tastature
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int eventType = 1;
                int keycode = e.getKeyCode();

                try {
                    finalDos.writeInt(eventType);
                    finalDos.writeInt(keycode);
                    finalDos.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            }
        };

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("KlijentGUI");
        KlijentGUI klijentGUI = new KlijentGUI();
        frame.setContentPane(klijentGUI.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


    }
}

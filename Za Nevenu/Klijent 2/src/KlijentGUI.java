import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class KlijentGUI {
    private JPanel panel1;
    private JPanel gore;
    private JLabel adresa;
    private JButton ipConfirm;
    private JPanel PanelSlike;
    private JLabel label;

    private String serverIpAddress;

    static int originalScreenWidth = 1920;
    static int originalScreenHeight = 1080;

    KlijentGUI() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String ipAddress = localhost.getHostAddress();

            adresa.setText("Vasa adresa: " + ipAddress);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
            originalScreenWidth = dis.readInt();
            originalScreenHeight = dis.readInt();

            //PRIMA SLIKU PREKO THREADA U POZADINI
            //Ako je ne prima tako onda se zaglavi u toj petlji i ne desava se nsita drugo, ne dodaje se npr listener za klik
            SwingWorker<Void, BufferedImage> worker = new SwingWorker<Void, BufferedImage>() {
                @Override
                protected Void doInBackground() throws Exception {
                    while (true) {
                        System.out.println("Waiting for image from server...");
                        int imageDataLength = dis.readInt();
                        byte[] imageData = new byte[imageDataLength];
                        dis.readFully(imageData);

                        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
                        BufferedImage scaledImage = scaleImageToFitLabel(originalImage);

                        publish(scaledImage);
                    }
                }

                @Override
                protected void process(java.util.List<BufferedImage> chunks) {
                    // Update UI with the latest image on the EDT
                    BufferedImage latestImage = chunks.get(chunks.size() - 1);
                    label.setText("");
                    label.setIcon(new ImageIcon(latestImage));
                    label.revalidate();
                }
            };

            worker.execute(); // Start the background image receiving task


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
            /*try {

            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            System.out.println("Created input stream!");


            originalScreenWidth = dis.readInt();
            originalScreenHeight = dis.readInt();

            while (true) {
                System.out.println("Waiting for image from server...");
                int imageDataLength = dis.readInt();
                byte[] imageData = new byte[imageDataLength];
                dis.readFully(imageData);

                // Convert bytes to image and update label
                //BufferedImage slika = ByteToBufferedImage.convert(imageData);
                //Icon slikaa = new ImageIcon(slika);

                BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
                BufferedImage scaledImage = scaleImageToFitLabel(originalImage);

                SwingUtilities.invokeLater(() -> {
                    label.setText("");
                    label.setIcon(new ImageIcon(scaledImage));
                    label.revalidate();
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
        }*/


        //slanje klika
        DataOutputStream finalDos = dos;
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Uhvacen klik");
                try {
                    // Izračunavanje razmere između originalne i trenutne veličine slike
                    int displayedImageWidth = label.getWidth();
                    int displayedImageHeight = label.getHeight();

                    double originalAspectRatio = (double) originalScreenWidth / originalScreenHeight;
                    double labelAspectRatio = (double) displayedImageWidth / displayedImageHeight;

                    double scaleX;
                    double scaleY;

                    if (originalAspectRatio > labelAspectRatio) {
                        scaleX = (double) originalScreenWidth / displayedImageWidth;
                        scaleY = scaleX;
                    } else {
                        scaleY = (double) originalScreenHeight / displayedImageHeight;
                        scaleX = scaleY;
                    }
                    /*double scaleX = (double) originalScreenWidth / displayedImageWidth;
                    double scaleY = (double) originalScreenHeight / displayedImageHeight;*/

                    // Izračunavanje stvarne pozicije klika
                    int actualX = (int) (e.getX() * scaleX);
                    int actualY = (int) (e.getY() * scaleY);
                    int eventType = 0;//FLAG da se radi o kliku misa, da bi uvijek znao da sta se salje

                    // Slanje koordinata i tipa klika serveru
                    finalDos.writeInt(eventType);
                    finalDos.writeInt(actualX);
                    finalDos.writeInt(actualY);
                    finalDos.writeInt(e.getButton());
                    System.out.println("Posalan klik");

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
        label.addKeyListener(keyAdapter);


    }

    //Skalira slikuuu
    private BufferedImage scaleImageToFitLabel(BufferedImage originalImage) {

        int labelWidth = PanelSlike.getWidth();
        int labelHeight = PanelSlike.getHeight();

        double originalAspectRatio = (double) originalScreenWidth / originalScreenHeight;
        double labelAspectRatio = (double) labelWidth / labelHeight;

        double scaleX;
        double scaleY;

        if (originalAspectRatio > labelAspectRatio) {
            scaleX = (double) labelWidth / originalScreenWidth;
            scaleY = scaleX;
        } else {
            scaleY = (double) labelHeight / originalScreenHeight;
            scaleX = scaleY;
        }

        /*double scaleX = (double) labelWidth / originalScreenWidth;
        double scaleY = (double) labelHeight / originalScreenHeight;
        */
        int scaledWidth = (int) (originalImage.getWidth() * scaleX);
        int scaledHeight = (int) (originalImage.getHeight() * scaleY);

        Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        BufferedImage bufferedScaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufferedScaledImage.createGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        return bufferedScaledImage;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("KlijentGUI");
        KlijentGUI klijentGUI = new KlijentGUI();
        Dimension minimumSize = new Dimension(1120, 630); // Proporcije 16:9
        frame.setMinimumSize(minimumSize);
        frame.setContentPane(klijentGUI.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


    }


}

